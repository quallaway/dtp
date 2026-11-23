package dev.dtp.blackmarket;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.currency.Payout;
import dev.dtp.currency.Taler;
import dev.dtp.upgrade.ChainBreaker;

/**
 * Der Maut-Fluch: ein Werkzeug-Fluch vom Schwarzmarkt ({@code data/dtp/enchantment/toll_curse.json}), im Tag
 * {@code #minecraft:curse}. Jeder Abbau eines Blocks mit Härte über 0 kostet {@code curse.tollPrice} Taler aus dem
 * Inventar; was der Mehrfachabbau mitnimmt, ist darin enthalten. Ohne genug Geld baut das Werkzeug nur mit
 * {@code curse.tollSlowdown} der Geschwindigkeit ab, und es gibt keinen Mehrfachabbau. Zum Ausgleich wirkt Unbreaking eine Stufe höher als eingetragen — das Geld trägt einen Teil
 * der Abnutzung, und man wird das Werkzeug nicht so leicht los.
 *
 * <p>Wie der {@link ScamCurse} sperrt er Contracts, den Schwarzmarkt und das Kombinieren im Amboss
 * ({@link ScamCurse#isLocked}).
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class TollCurse {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "toll_curse"));

    private TollCurse() {
    }

    public static boolean isCursed(ItemStack stack) {
        return ScamCurse.has(stack, KEY);
    }

    /** Kann der Fluch auf dieses Item: ein Abbauwerkzeug, das ihn noch nicht trägt. */
    public static boolean canApply(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.MINING_ENCHANTABLE) && !isCursed(stack);
    }

    public static void apply(ItemStack stack, HolderLookup.Provider registries) {
        Holder<Enchantment> curse = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(curse, 1));
    }

    /** Taler im Inventar des Spielers, in Kupfertalern. */
    public static long balance(Player player) {
        long sum = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            sum += Taler.valueOf(inventory.getItem(i));
        }
        return sum;
    }

    public static boolean canPay(Player player) {
        return balance(player) >= DtpConfig.TOLL_PRICE.get();
    }

    /**
     * Zieht den Betrag aus dem Inventar, kleinste Stufe zuerst; Wechselgeld geht zurück ins Inventar. Reicht das Geld
     * nicht, wird nichts entnommen.
     */
    public static boolean pay(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (balance(player) < amount) {
            return false;
        }
        Inventory inventory = player.getInventory();
        long remaining = amount;
        outer:
        for (Taler denomination : Taler.values()) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (remaining <= 0) {
                    break outer;
                }
                ItemStack stack = inventory.getItem(slot);
                if (Taler.of(stack) != denomination) {
                    continue;
                }
                long needed = (remaining + denomination.value - 1) / denomination.value;
                int take = (int) Math.min(stack.getCount(), needed);
                stack.shrink(take);
                remaining -= take * denomination.value;
            }
        }
        if (remaining < 0) {
            Payout.toPlayer(player, -remaining);
        }
        inventory.setChanged();
        return true;
    }

    /** Muss dieser Spieler für diesen Block zahlen? Haupthand verflucht, Überleben, Block mit Härte über 0. */
    static boolean tolled(Player player, BlockState state, float hardness) {
        return !player.isCreative() && !player.isSpectator() && isCursed(player.getMainHandItem()) && hardness > 0
                && DtpConfig.TOLL_PRICE.get() > 0 && !state.isAir();
    }

    /** Abbaugeschwindigkeit mit Maut: unverändert, wenn bezahlt werden kann, sonst mit dem Bremsfaktor. */
    public static float speed(float speed, boolean canPay) {
        return canPay ? speed : speed * DtpConfig.TOLL_SLOWDOWN.get().floatValue();
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!isCursed(player.getMainHandItem()) || player.isCreative()) {
            return;
        }
        float hardness = event.getPosition().map(p -> event.getState().getDestroySpeed(player.level(), p)).orElse(1.0f);
        if (tolled(player, event.getState(), hardness)) {
            event.setNewSpeed(speed(event.getNewSpeed(), canPay(player)));
        }
    }

    /** Wurde beim laufenden Abbau die Maut nicht bezahlt? Nur auf dem Server-Thread benutzt. */
    private static boolean unpaid;

    /** Ohne bezahlte Maut startet {@link ChainBreaker} keinen Mehrfachabbau. */
    public static boolean blocksChain() {
        return unpaid;
    }

    // Vor ChainBreaker (LOWEST): Der getroffene Block ist bezahlt, bevor die Kette startet. Die mit abgebauten Blöcke
    // laufen wieder hier durch und sind frei — ein Abbau kostet einmal Maut.
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBreak(BreakBlockEvent event) {
        if (ChainBreaker.isBreaking()) {
            return;
        }
        unpaid = false;
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        if (!tolled(player, state, state.getDestroySpeed(event.getLevel(), event.getPos()))) {
            return;
        }
        // Der langsame Einzelabbau ist frei, einen Mehrfachabbau ohne Geld gibt es nicht.
        unpaid = !pay(player, DtpConfig.TOLL_PRICE.get());
    }

    @SubscribeEvent
    static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        BlockState state = player.level().getBlockState(event.getPos());
        if (tolled(player, state, state.getDestroySpeed(player.level(), event.getPos())) && !canPay(player)) {
            player.sendOverlayMessage(Component.translatable("dtp.toll_curse.no_coins", DtpConfig.TOLL_PRICE.get()).withStyle(ChatFormatting.RED));
        }
    }

    /** Unbreaking +1 für die Spielmechanik. Der Tooltip zeigt weiter die eingetragene Stufe, dazu eine eigene Zeile. */
    @SubscribeEvent
    static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (!event.isTargetting(Enchantments.UNBREAKING)) {
            return;
        }
        boolean cursed = event.getStack().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream().anyMatch(h -> h.is(KEY));
        if (!cursed) {
            return;
        }
        event.getHolder(Enchantments.UNBREAKING).ifPresent(unbreaking -> {
            ItemEnchantments.Mutable enchantments = event.getEnchantments();
            enchantments.set(unbreaking, enchantments.getLevel(unbreaking) + 1);
        });
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isCursed(stack)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        Component line = Component.translatable("dtp.toll_curse.unbreaking").withStyle(ChatFormatting.GRAY);
        String curseName = Component.translatable("enchantment.dtp.toll_curse").getString();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().startsWith(curseName)) {
                tooltip.add(i + 1, line);
                return;
            }
        }
        tooltip.add(line);
    }
}
