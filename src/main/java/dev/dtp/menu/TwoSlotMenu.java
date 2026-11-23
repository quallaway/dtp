package dev.dtp.menu;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Gemeinsame Bauart von Schwarzmarkt und Reparaturbank: ein Slot für einen Ausrüstungsgegenstand, einer für das Geld,
 * darunter das Inventar. Ein Knopf löst die Aktion aus; was liegen bleibt, geht beim Schließen zurück an den Spieler.
 * Der Block hat kein eigenes Inventar — das Geld liegt nur sichtbar auf dem Tisch.
 */
public abstract class TwoSlotMenu extends BlockBoundMenu {
    /** Die einzige Knopf-ID: die Aktion des Blocks. */
    public static final int ACTION = 0;
    public static final int SLOT_ITEM = 0;
    public static final int SLOT_PAYMENT = 1;
    /** Lage der beiden Slots (Item-Position, der Rahmen liegt 1 px darum); der Bildschirm zeichnet Rahmen und Beschriftung dort. */
    public static final int ITEM_X = 43;
    public static final int PAYMENT_X = 117;
    public static final int SLOT_Y = 30;
    /** Leerer Item-Slot zeigt wie der Schmiedetisch reihum Ausrüstung. */
    private static final List<Identifier> ITEM_ICONS = List.of("sword", "pickaxe", "helmet", "chestplate", "axe", "leggings", "boots", "shovel")
            .stream().map(n -> Identifier.withDefaultNamespace("container/slot/" + n)).toList();
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = 38;

    protected final Container table = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            TwoSlotMenu.this.slotsChanged(this);
        }
    };
    private Component message;

    protected TwoSlotMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, Block block) {
        super(type, id, inventory, pos, block);
        addSlot(new Slot(table, SLOT_ITEM, ITEM_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return acceptsItem(stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return ITEM_ICONS.get((int) (Util.getMillis() / 1000L % ITEM_ICONS.size()));
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(table, SLOT_PAYMENT, PAYMENT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return acceptsPayment(stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return paymentIcon();
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    /** Darf der Stapel in den Item-Slot? */
    protected abstract boolean acceptsItem(ItemStack stack);

    /** Darf der Stapel in den Geld-Slot? */
    protected abstract boolean acceptsPayment(ItemStack stack);

    /** Symbol im leeren Geld-Slot. */
    protected abstract Identifier paymentIcon();

    /** Führt die Aktion des Blocks auf den beiden Slots aus und liefert die Meldung für das Panel. */
    protected abstract Component perform(ServerLevel level, Player player);

    /** Der blockeigene Teil der Panel-Daten; die Meldung der letzten Aktion ergänzt {@link #snapshot}. */
    protected abstract void fill(CompoundTag tag, ServerLevel level);

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        sync.markChanged();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != ACTION || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        message = perform(level, player);
        sync.markChanged();
        broadcastChanges();
        return true;
    }

    @Override
    protected final CompoundTag snapshot(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        fill(tag, level);
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (acceptsPayment(stack)) {
            if (!moveItemStackTo(stack, SLOT_PAYMENT, SLOT_PAYMENT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            Slot target = this.slots.get(SLOT_ITEM);
            if (target.hasItem() || !target.mayPlace(stack)) {
                return ItemStack.EMPTY;
            }
            target.setByPlayer(stack.split(1));
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, table);
        }
    }
}
