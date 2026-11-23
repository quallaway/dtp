package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Payout;
import dev.dtp.currency.Taler;
import dev.dtp.menu.DepositChestMenu;
import dev.dtp.registry.ModBlocks;

/** Hält Taler für Käufe bereit. Hopper dürfen nur Taler hineinfördern. */
public class DepositChestBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public DepositChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DEPOSIT_CHEST_BE.get(), pos, state);
    }

    public long balance() {
        return DepositChests.balance(this);
    }

    /** Ordnet die Taler in möglichst wenige Items um. Andere Items bleiben liegen. */
    public void compact() {
        long total = balance();
        for (int i = 0; i < items.size(); i++) {
            if (Taler.isTaler(items.get(i))) {
                items.set(i, ItemStack.EMPTY);
            }
        }
        Payout.toContainer(this, level, worldPosition, total);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Taler.isTaler(stack);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dtp.deposit_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DepositChestMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
