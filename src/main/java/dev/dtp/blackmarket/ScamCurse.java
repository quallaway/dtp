package dev.dtp.blackmarket;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import dev.dtp.Dtp;

/**
 * Der Scam-Fluch: eine datengetriebene Verzauberung ({@code data/dtp/enchantment/scam_curse.json}) ohne eigene Wirkung,
 * im Tag {@code #minecraft:curse} — rot im Tooltip, der Schleifstein entfernt ihn nicht. Er steht in keinem Tag für
 * Verzauberungstisch, Handel oder Beute; man bekommt ihn nur vom Schwarzmarkt.
 *
 * <p>Was er sperrt — ebenso der {@link TollCurse Maut-Fluch} —, prüfen die betroffenen Stellen über {@link #isLocked}: Contracts ({@code Contract.matches}),
 * der Schwarzmarkt ({@link Blackmarket#check}) und hier der Amboss. Der Markt nimmt verzauberte Items ohnehin nicht.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ScamCurse {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "scam_curse"));

    private ScamCurse() {
    }

    /** Trägt das Item den Fluch — als Verzauberung oder, bei Büchern, als gespeicherte Verzauberung? */
    public static boolean isCursed(ItemStack stack) {
        return has(stack, KEY);
    }

    /**
     * Ist das Item vom Schwarzmarkt gezeichnet — Scam-Fluch oder {@link TollCurse Maut-Fluch}? Beide sperren
     * Contracts, den Schwarzmarkt und das Kombinieren im Amboss.
     */
    public static boolean isLocked(ItemStack stack) {
        return isCursed(stack) || TollCurse.isCursed(stack);
    }

    static boolean has(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return false;
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().stream().anyMatch(h -> h.is(key))
                || stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(key));
    }

    /** Legt den Fluch auf das Item. */
    public static void apply(ItemStack stack, HolderLookup.Provider registries) {
        Holder<Enchantment> curse = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(curse, 1));
    }

    /**
     * Verfluchte Items (Scam- und Maut-Fluch) lassen sich im Amboss mit nichts kombinieren — weder mit Items, Büchern noch Reparaturmaterial.
     * Reines Umbenennen bleibt erlaubt; ein umbenanntes Item zählt ohnehin für keinen Contract. Ein abgebrochenes
     * Event erreicht spätere Handler nicht, also auch nicht {@code AnvilCap}.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!event.getRight().isEmpty() && (isLocked(event.getLeft()) || isLocked(event.getRight()))) {
            event.setCanceled(true);
        }
    }
}
