package dev.dtp.compat.sophisticated;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import dev.dtp.currency.Taler;

/**
 * Das Inventar eines platzierten Rucksacks als {@link Container}, wie DTPs Lager-Schnittstelle es erwartet.
 *
 * <p>DTP ändert Stapel aus {@link #getItem} direkt (z. B. {@code shrink}) und ruft danach {@link #setChanged()} auf.
 * Sophisticated Core gibt aber nicht verlässlich seine eigenen Stapel heraus. Deshalb arbeitet der Adapter mit Kopien
 * und schreibt bei {@link #setChanged()} jeden geänderten Slot über {@link InventoryHandler#setStackInSlot} zurück.
 * Er lebt nur für eine Suche, also einen Vorgang.
 */
final class BackpackContainer implements Container {
    private final InventoryHandler handler;
    private final boolean talerOnly;
    private final Map<Integer, ItemStack> copies = new HashMap<>();

    BackpackContainer(InventoryHandler handler, boolean talerOnly) {
        this.handler = handler;
        this.talerOnly = talerOnly;
    }

    @Override
    public int getContainerSize() {
        return handler.size();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return copies.computeIfAbsent(slot, s -> handler.getStackInSlot(s).copy());
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = getItem(slot).split(count);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = getItem(slot);
        copies.put(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        copies.put(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return (!talerOnly || Taler.isTaler(stack)) && handler.isItemValid(slot, stack);
    }

    /** Schreibt alle geänderten Slots in den Rucksack zurück. */
    @Override
    public void setChanged() {
        copies.forEach((slot, stack) -> {
            if (!ItemStack.matches(stack, handler.getStackInSlot(slot))) {
                handler.setStackInSlot(slot, stack.copy());
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            copies.put(slot, ItemStack.EMPTY);
        }
        setChanged();
    }
}
