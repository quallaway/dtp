package dev.dtp.upgrade;

import java.util.Collection;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Drops direkt ins Inventar des Spielers — für Sammel-Upgrades (Mehrfachabbau, Flächenernte, Wolle, Fischernetz),
 * deren Items sonst verstreut liegen blieben. Was nicht passt, fällt um den Spieler zu Boden.
 */
public final class DirectDrops {
    private DirectDrops() {
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            Block.popResource(player.level(), player.blockPosition(), stack);
        }
    }

    public static void giveAll(Player player, Collection<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                give(player, stack);
            }
        }
    }
}
