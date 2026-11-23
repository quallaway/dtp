package dev.dtp.blackmarket;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.dtp.DtpConfig;
import dev.dtp.currency.Taler;
import dev.dtp.data.Protocol;
import dev.dtp.market.Market;

/**
 * Der Schwarzmarkt-Block: Ein verzauberter Gegenstand und ein Silberbeutel gehen hinein, eine zufällige Verzauberung
 * unter ihrer Höchststufe steigt um eins. Einmal pro Markttag für den ganzen Server — und nicht immer ehrlich.
 *
 * <p>Bewusst nur mit einem Beutel im Slot bezahlt, nicht aus den Einzahlkisten: Man legt das Geld sichtbar auf den
 * Tisch. Bücher zählen nicht, sonst ließen sich verbesserte Stufen frei weitergeben.
 */
public final class Blackmarket {
    public static final int SLOT_ITEM = 0;
    public static final int SLOT_PAYMENT = 1;

    private Blackmarket() {
    }

    /** Ausgang eines Handels. Alles außer {@link #SUCCESS} ist ein Scam. */
    public enum Outcome {
        SUCCESS, STEAL, NOTHING, CURSE, TOLL;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean scam() {
            return this != SUCCESS;
        }

        public boolean upgrades() {
            return this == SUCCESS || this == CURSE || this == TOLL;
        }
    }

    /** Warum der Handel gerade nicht geht. */
    public enum Blocked {
        NO_ITEM, NOT_ENCHANTED, CURSED, MAXED, NO_PAYMENT, USED_TODAY;

        public String translationKey() {
            return "dtp.blackmarket.blocked." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Nur echte Verzauberungen zählen, keine gespeicherten in Büchern. */
    public static boolean accepts(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    public static boolean isPayment(ItemStack stack) {
        return Taler.of(stack) == Taler.SILVER_POUCH;
    }

    /**
     * Trägt das Item irgendeinen Fluch — Vanilla (Vanishing, Binding) oder vom Schwarzmarkt? Verbessert wird nur
     * Unverfluchtes. Contracts und Amboss sperren dagegen nur die Schwarzmarkt-Flüche ({@link ScamCurse#isLocked}).
     */
    public static boolean hasAnyCurse(ItemStack stack) {
        return ScamCurse.isLocked(stack) || stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet().stream()
                .anyMatch(h -> h.is(EnchantmentTags.CURSE));
    }

    /** Verzauberungen, die noch steigen können: unter der Höchststufe und kein Fluch. */
    public static List<Holder<Enchantment>> candidates(ItemStack stack) {
        List<Holder<Enchantment>> list = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            if (!holder.is(EnchantmentTags.CURSE) && e.getIntValue() < holder.value().getMaxLevel()) {
                list.add(holder);
            }
        }
        return list;
    }

    /** Sperrgrund für diese Slots, {@code null} wenn der Handel geht. Das Tageslimit nur mit Server (sonst übersprungen). */
    public static Blocked check(MinecraftServer server, ItemStack item, ItemStack payment) {
        if (item.isEmpty()) {
            return Blocked.NO_ITEM;
        }
        if (!accepts(item)) {
            return Blocked.NOT_ENCHANTED;
        }
        if (hasAnyCurse(item)) {
            return Blocked.CURSED;
        }
        if (candidates(item).isEmpty()) {
            return Blocked.MAXED;
        }
        if (!isPayment(payment)) {
            return Blocked.NO_PAYMENT;
        }
        if (server != null && BlackmarketData.get(server).usedToday(server)) {
            return Blocked.USED_TODAY;
        }
        return null;
    }

    /** Würfelt den Ausgang mit den Werten aus der Config. Die Maut gibt es nur für Werkzeuge, die sie noch nicht tragen. */
    public static Outcome roll(RandomSource random, ItemStack item) {
        return roll(random, DtpConfig.BLACKMARKET_SCAM_CHANCE.get(), DtpConfig.BLACKMARKET_WEIGHT_STEAL.get(),
                DtpConfig.BLACKMARKET_WEIGHT_NOTHING.get(), DtpConfig.BLACKMARKET_WEIGHT_CURSE.get(),
                TollCurse.canApply(item) ? DtpConfig.BLACKMARKET_WEIGHT_TOLL.get() : 0);
    }

    /** Erst: Scam ja oder nein. Dann: welcher Scam, nach Gewicht. Ohne Gewichte bleibt es beim Erfolg. */
    public static Outcome roll(RandomSource random, double scamChance, int steal, int nothing, int curse, int toll) {
        if (random.nextDouble() >= scamChance) {
            return Outcome.SUCCESS;
        }
        int total = steal + nothing + curse + toll;
        if (total <= 0) {
            return Outcome.SUCCESS;
        }
        int r = random.nextInt(total);
        if (r < steal) {
            return Outcome.STEAL;
        }
        if (r < steal + nothing) {
            return Outcome.NOTHING;
        }
        return r < steal + nothing + curse ? Outcome.CURSE : Outcome.TOLL;
    }

    /**
     * Ein Handel am Block: prüfen, würfeln, ausführen, Tag verbrauchen. Liefert die Meldung für die Oberfläche;
     * bei einem Sperrgrund passiert nichts.
     */
    public static Component trade(MinecraftServer server, Player player, Container slots, RandomSource random) {
        Blocked blocked = check(server, slots.getItem(SLOT_ITEM), slots.getItem(SLOT_PAYMENT));
        if (blocked != null) {
            return Component.translatable(blocked.translationKey());
        }
        String itemId = Market.itemId(slots.getItem(SLOT_ITEM).getItem()).toString();
        Outcome outcome = roll(random, slots.getItem(SLOT_ITEM));
        Component message = apply(outcome, slots, server.registryAccess(), random);
        // Jede Nutzung verbraucht den Tag, auch ein Scam.
        BlackmarketData.get(server).markUsed(server);
        Protocol.log(server, player, "blackmarket_" + outcome.id(), itemId, 1, Taler.SILVER_POUCH.value);
        player.level().playSound(null, player.blockPosition(),
                outcome.scam() ? SoundEvents.WITCH_CELEBRATE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return message;
    }

    /**
     * Führt einen festen Ausgang aus: Der Beutel ist in jedem Fall weg. Ohne Sperrprüfung und ohne Tageslimit — das
     * erledigt {@link #trade}; getrennt, damit die Tests jeden Ausgang gezielt prüfen können.
     */
    public static Component apply(Outcome outcome, Container slots, HolderLookup.Provider registries, RandomSource random) {
        slots.getItem(SLOT_PAYMENT).shrink(1);
        ItemStack item = slots.getItem(SLOT_ITEM);
        Component message;
        if (outcome == Outcome.STEAL) {
            message = Component.translatable("dtp.blackmarket.outcome.steal", item.getHoverName());
            slots.setItem(SLOT_ITEM, ItemStack.EMPTY);
        } else if (outcome == Outcome.NOTHING) {
            message = Component.translatable("dtp.blackmarket.outcome.nothing");
        } else {
            Component raised = upgrade(item, random);
            if (outcome == Outcome.CURSE) {
                ScamCurse.apply(item, registries);
                message = Component.translatable("dtp.blackmarket.outcome.curse", raised);
            } else if (outcome == Outcome.TOLL) {
                TollCurse.apply(item, registries);
                message = Component.translatable("dtp.blackmarket.outcome.toll", raised);
            } else {
                message = Component.translatable("dtp.blackmarket.outcome.success", raised);
            }
        }
        slots.setChanged();
        return message;
    }

    /** Hebt eine zufällige steigerbare Verzauberung um eine Stufe; liefert ihren neuen Namen samt Stufe. */
    static Component upgrade(ItemStack item, RandomSource random) {
        List<Holder<Enchantment>> candidates = candidates(item);
        if (candidates.isEmpty()) {
            return Component.empty();
        }
        Holder<Enchantment> pick = candidates.get(random.nextInt(candidates.size()));
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        int level = enchantments.getLevel(pick) + 1;
        enchantments.set(pick, level);
        item.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return Enchantment.getFullname(pick, level);
    }
}
