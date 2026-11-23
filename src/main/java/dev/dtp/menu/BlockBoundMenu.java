package dev.dtp.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Menü ohne Slots für Blöcke ohne Inventar (Marktstand, Beacon). Gültig, solange der Block steht und der Spieler nah ist. */
public abstract class BlockBoundMenu extends AbstractContainerMenu implements MenuSync.Holder {
    protected final BlockPos pos;
    protected final Player player;
    protected final MenuSync sync;
    private final Block block;

    protected BlockBoundMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, Block block) {
        super(type, id);
        this.pos = pos;
        this.player = inventory.player;
        this.block = block;
        this.sync = new MenuSync(this, inventory.player);
    }

    @Override
    public MenuSync sync() {
        return sync;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(block)
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player.level() instanceof ServerLevel level) {
            sync.tick(() -> snapshot(level));
        }
    }

    protected abstract CompoundTag snapshot(ServerLevel level);
}
