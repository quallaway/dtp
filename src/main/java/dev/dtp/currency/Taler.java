package dev.dtp.currency;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import dev.dtp.registry.ModItems;

/**
 * Die neun Talerstufen: Kupfer, Silber, Gold in je Taler, Beutel, Sack — durchgehend Faktor 9.
 * Die Reihenfolge ist aufsteigend nach Wert; {@link #value} ist 9^ordinal.
 */
public enum Taler {
    COPPER_TALER("copper_taler"),
    COPPER_POUCH("copper_pouch"),
    COPPER_SACK("copper_sack"),
    SILVER_TALER("silver_taler"),
    SILVER_POUCH("silver_pouch"),
    SILVER_SACK("silver_sack"),
    GOLD_TALER("gold_taler"),
    GOLD_POUCH("gold_pouch"),
    GOLD_SACK("gold_sack");

    public final String id;
    public final long value;

    Taler(String id) {
        this.id = id;
        this.value = pow9(this.ordinal());
    }

    private static long pow9(int n) {
        long v = 1;
        for (int i = 0; i < n; i++) {
            v *= 9;
        }
        return v;
    }

    public Item item() {
        return ModItems.TALER.get(this).get();
    }

    public DeferredItem<Item> holder() {
        return ModItems.TALER.get(this);
    }

    public static Taler of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Taler t : values()) {
            if (stack.is(t.item())) {
                return t;
            }
        }
        return null;
    }

    public static boolean isTaler(ItemStack stack) {
        return of(stack) != null;
    }

    /** Wert eines Stapels in Kupfertalern, 0 für alles, was kein Taler ist. */
    public static long valueOf(ItemStack stack) {
        Taler t = of(stack);
        return t == null ? 0 : t.value * stack.getCount();
    }

    /**
     * Zerlegt einen Betrag in möglichst wenige Items: größte passende Stufe zuerst, Rest absteigend.
     * 500 → 6 Kupfersäcke + 1 Kupferbeutel + 5 Kupfertaler.
     */
    public static List<ItemStack> split(long amount) {
        List<ItemStack> result = new ArrayList<>();
        Taler[] all = values();
        for (int i = all.length - 1; i >= 0 && amount > 0; i--) {
            Taler t = all[i];
            long count = amount / t.value;
            amount -= count * t.value;
            while (count > 0) {
                int stackSize = (int) Math.min(count, t.item().getDefaultInstance().getMaxStackSize());
                result.add(new ItemStack(t.item(), stackSize));
                count -= stackSize;
            }
        }
        return result;
    }
}
