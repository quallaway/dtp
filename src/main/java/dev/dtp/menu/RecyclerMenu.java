package dev.dtp.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.dtp.block.RecyclerBlockEntity;
import dev.dtp.registry.ModMenus;

/**
 * Fenster des Recyclers, aufgebaut wie der Vanilla-Ofen (gleiche Slot-Positionen, gleiche Shift-Klick-Regeln), aber
 * ohne Rezeptbuch. Die Eingabe nimmt alles an; ob ein Item recycelbar ist, weiß nur der Server.
 */
public class RecyclerMenu extends AbstractContainerMenu {
    private static final int SLOTS = 3;
    private static final int INV_START = SLOTS;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int INV_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public RecyclerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.RECYCLER.get(), id);
        checkContainerSize(container, SLOTS);
        checkContainerDataCount(data, RecyclerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, RecyclerBlockEntity.INPUT, 56, 17));
        addSlot(new Slot(container, RecyclerBlockEntity.FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RecyclerBlockEntity.isFuel(stack) || stack.is(Items.BUCKET);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return stack.is(Items.BUCKET) ? 1 : super.getMaxStackSize(stack);
            }
        });
        addSlot(new Slot(container, RecyclerBlockEntity.OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public static RecyclerMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        buf.readBlockPos();
        return new RecyclerMenu(id, inventory, new SimpleContainer(SLOTS), new SimpleContainerData(RecyclerBlockEntity.DATA_COUNT));
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /** Wie beim Ofen: Brennstoff ins Brennstofffach, alles andere in die Eingabe; Schrott zurück ins Inventar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, INV_START, INV_END, index == RecyclerBlockEntity.OUTPUT)) {
                return ItemStack.EMPTY;
            }
        } else if (RecyclerBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, RecyclerBlockEntity.FUEL, RecyclerBlockEntity.FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, RecyclerBlockEntity.INPUT, RecyclerBlockEntity.INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return clicked;
    }

    public boolean isLit() {
        return data.get(RecyclerBlockEntity.DATA_LIT_TIME) > 0;
    }

    /** Restglut, 1 = frisch angezündet. */
    public float litProgress() {
        int duration = data.get(RecyclerBlockEntity.DATA_LIT_DURATION);
        if (duration == 0) {
            duration = 200;
        }
        return Mth.clamp((float) data.get(RecyclerBlockEntity.DATA_LIT_TIME) / duration, 0.0f, 1.0f);
    }

    /** Fortschritt des aktuellen Items, 0–1. */
    public float progress() {
        int current = data.get(RecyclerBlockEntity.DATA_PROGRESS);
        int total = data.get(RecyclerBlockEntity.DATA_TOTAL);
        return total != 0 && current != 0 ? Mth.clamp((float) current / total, 0.0f, 1.0f) : 0.0f;
    }
}
