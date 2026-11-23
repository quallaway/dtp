package dev.dtp.upgrade;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import dev.dtp.Dtp;

/**
 * Server-Upgrade „Meisterschmied“ (am Beacon, wirkt für alle Spieler): Amboss-Kosten höchstens {@value #MAX_COST} Level.
 *
 * <p>Vanilla verwirft das Ergebnis bei Kosten ab 40, <em>bevor</em> NeoForges {@link AnvilUpdateEvent} feuert. Ohne
 * Eingriff in Vanilla-Code bleibt nur, das Ergebnis in diesem Fall selbst nachzurechnen: {@link #compute} ist ein
 * Nachbau von {@code AnvilMenu.createResultInternal} (Minecraft 26.3, NeoForge 26.3.0.99), nur mit gedeckelten Kosten.
 * <b>Bei Minecraft-Updates gegen den Vanilla-Quelltext prüfen</b> — der GameTest {@code anvil_cap} vergleicht beide.
 *
 * <p>Das Event feuert auf Server und Client; die Upgrade-Stufe ist auf beiden bekannt ({@link ServerUpgradeSync}),
 * also zeigt auch der Client das Ergebnis und die Kosten.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class AnvilCap {
    public static final int MAX_COST = 39;
    private static final int TOO_EXPENSIVE = 40;

    private AnvilCap() {
    }

    /** Ergebnis, Levelkosten und verbrauchte Anzahl des rechten Stapels. */
    public record Result(ItemStack output, int cost, int material) {
    }

    @SubscribeEvent
    static void onAnvilUpdate(AnvilUpdateEvent event) {
        AnvilUpdateEvent.VanillaResult vanilla = event.getVanillaResult();
        if (!vanilla.output().isEmpty() || vanilla.xpCost() < TOO_EXPENSIVE
                || ServerUpgradeSync.level(event.getPlayer(), ServerUpgrade.ANVIL_CAP) == 0) {
            return;
        }
        Result result = compute(event.getLeft(), event.getRight(), event.getName(), event.getPlayer(), MAX_COST);
        if (result != null) {
            event.setOutput(result.output());
            event.setXpCost(result.cost());
            event.setMaterialCost(result.material());
        }
    }

    /**
     * Nachbau der Vanilla-Rechnung mit Obergrenze {@code maxCost} für die Levelkosten; {@code null}, wenn Vanilla
     * kein Ergebnis hätte (unpassende Zutaten, nichts zu tun). Mit {@code maxCost = Integer.MAX_VALUE} liefert sie
     * die ungedeckelten Vanilla-Kosten — so prüft der GameTest den Nachbau.
     */
    public static Result compute(ItemStack input, ItemStack addition, String itemName, Player player, int maxCost) {
        if (input.isEmpty() || !EnchantmentHelper.canStoreEnchantments(input)) {
            return null;
        }
        int price = 0;
        long tax = 0L;
        int namingCost = 0;
        int material = 0;
        ItemStack result = input.copy();
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(result));
        tax += (long) input.getOrDefault(DataComponents.REPAIR_COST, 0) + addition.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (!addition.isEmpty()) {
            boolean usingBook = addition.has(DataComponents.STORED_ENCHANTMENTS);
            if (result.isDamageableItem() && input.isValidRepairItem(addition)) {
                int repairAmount = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
                if (repairAmount <= 0) {
                    return null;
                }
                int count;
                for (count = 0; repairAmount > 0 && count < addition.getCount(); count++) {
                    result.setDamageValue(result.getDamageValue() - repairAmount);
                    price++;
                    repairAmount = Math.min(result.getDamageValue(), result.getMaxDamage() / 4);
                }
                material = count;
            } else {
                if (!usingBook && (!result.is(addition.getItem()) || !result.isDamageableItem())) {
                    return null;
                }
                if (result.isDamageableItem() && !usingBook) {
                    int remaining1 = input.getMaxDamage() - input.getDamageValue();
                    int remaining2 = addition.getMaxDamage() - addition.getDamageValue();
                    int remaining = remaining1 + remaining2 + result.getMaxDamage() * 12 / 100;
                    int resultDamage = Math.max(0, result.getMaxDamage() - remaining);
                    if (resultDamage < result.getDamageValue()) {
                        result.setDamageValue(resultDamage);
                        price += 2;
                    }
                }
                boolean anyCompatible = false;
                boolean anyIncompatible = false;
                for (Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(addition).entrySet()) {
                    Holder<Enchantment> holder = entry.getKey();
                    int current = enchantments.getLevel(holder);
                    int level = entry.getIntValue();
                    level = current == level ? level + 1 : Math.max(level, current);
                    Enchantment enchantment = holder.value();
                    boolean compatible = input.supportsEnchantment(holder) || player.getAbilities().instabuild;
                    for (Holder<Enchantment> other : enchantments.keySet()) {
                        if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                            compatible = false;
                            price++;
                        }
                    }
                    if (!compatible) {
                        anyIncompatible = true;
                    } else {
                        anyCompatible = true;
                        level = Math.min(level, enchantment.getMaxLevel());
                        enchantments.set(holder, level);
                        int fee = enchantment.getAnvilCost();
                        if (usingBook) {
                            fee = Math.max(1, fee / 2);
                        }
                        price += fee * level;
                        if (input.getCount() > 1) {
                            price = 40;
                        }
                    }
                }
                if (anyIncompatible && !anyCompatible) {
                    return null;
                }
            }
        }
        if (itemName != null && !StringUtil.isBlank(itemName)) {
            if (!itemName.equals(input.getHoverName().getString())) {
                namingCost = 1;
                price += namingCost;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
            }
        } else if (input.has(DataComponents.CUSTOM_NAME)) {
            namingCost = 1;
            price += namingCost;
            result.remove(DataComponents.CUSTOM_NAME);
        }
        if (price <= 0) {
            return null;
        }
        int cost = (int) Mth.clamp(tax + price, 0L, Integer.MAX_VALUE);
        if (namingCost == price && cost >= TOO_EXPENSIVE) {
            cost = TOO_EXPENSIVE - 1;
        }
        cost = Math.min(cost, maxCost);

        int baseCost = Math.max(result.getOrDefault(DataComponents.REPAIR_COST, 0), addition.getOrDefault(DataComponents.REPAIR_COST, 0));
        if (namingCost != price || namingCost == 0) {
            baseCost = AnvilMenu.calculateIncreasedRepairCost(baseCost);
        }
        result.set(DataComponents.REPAIR_COST, baseCost);
        EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
        return new Result(result, cost, material);
    }
}
