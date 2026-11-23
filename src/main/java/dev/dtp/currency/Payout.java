package dev.dtp.currency;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Auszahlen von Talern in Spielerinventare und Container — immer in kleinstmöglicher Itemzahl. */
public final class Payout {
    private Payout() {
    }

    /** Gibt dem Spieler den Betrag; was nicht ins Inventar passt, fällt vor ihm auf den Boden. */
    public static void toPlayer(Player player, long amount) {
        for (ItemStack stack : Taler.split(amount)) {
            toPlayer(player, stack);
        }
    }

    public static void toPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    /** Legt den Betrag in den Container; Überschuss fällt am Block heraus. */
    public static void toContainer(Container container, Level level, BlockPos dropAt, long amount) {
        for (ItemStack stack : Taler.split(amount)) {
            ItemStack rest = insert(container, stack);
            if (!rest.isEmpty()) {
                Block.popResource(level, dropAt.above(), rest);
            }
        }
    }

    /** Legt Stapel in den Container, erst auf passende Stapel, dann in freie Plätze. Gibt den Rest zurück. */
    public static ItemStack insert(Container container, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = 0; slot < container.getContainerSize() && !rest.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, rest)) {
                int space = Math.min(existing.getMaxStackSize(), container.getMaxStackSize()) - existing.getCount();
                int moved = Math.min(space, rest.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    rest.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < container.getContainerSize() && !rest.isEmpty(); slot++) {
            if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, rest)) {
                int moved = Math.min(rest.getCount(), Math.min(rest.getMaxStackSize(), container.getMaxStackSize()));
                container.setItem(slot, rest.split(moved));
            }
        }
        container.setChanged();
        return rest;
    }

    public static void insertAll(Container container, List<ItemStack> stacks, Level level, BlockPos dropAt) {
        for (ItemStack stack : stacks) {
            ItemStack rest = insert(container, stack);
            if (!rest.isEmpty()) {
                Block.popResource(level, dropAt.above(), rest);
            }
        }
    }
}
