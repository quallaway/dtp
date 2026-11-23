package dev.dtp.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.dtp.block.DepositChestBlockEntity;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Taler;
import dev.dtp.registry.ModMenus;

/** Einzahlkiste: zeigt ihren Bestand und den Gesamtvorrat in Reichweite, ordnet Taler auf Knopfdruck. */
public class DepositChestMenu extends ChestMenu implements MenuSync.Holder {
    public static final int BUTTON_COMPACT = 0;

    private final BlockPos pos;
    private final DepositChestBlockEntity chest;
    private final MenuSync sync;

    public DepositChestMenu(int id, Inventory inventory, DepositChestBlockEntity chest) {
        this(id, inventory, chest, chest.getBlockPos(), chest);
    }

    private DepositChestMenu(int id, Inventory inventory, Container container, BlockPos pos, DepositChestBlockEntity chest) {
        super(ModMenus.DEPOSIT_CHEST.get(), id, inventory, container, 3);
        // ChestMenu legt gewöhnliche Slots an; ersetzt durch gefilterte, damit auch von Hand nur Taler hineingehen.
        for (int i = 0; i < DepositChestBlockEntity.SIZE; i++) {
            Slot plain = slots.get(i);
            Slot filtered = new Slot(container, plain.getContainerSlot(), plain.x, plain.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return Taler.isTaler(stack);
                }
            };
            filtered.index = i;
            slots.set(i, filtered);
        }
        this.pos = pos;
        this.chest = chest;
        this.sync = new MenuSync(this, inventory.player);
    }

    public static DepositChestMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new DepositChestMenu(id, inventory, new SimpleContainer(DepositChestBlockEntity.SIZE), buf.readBlockPos(), null);
    }

    @Override
    public MenuSync sync() {
        return sync;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (chest != null && buttonId == BUTTON_COMPACT) {
            chest.compact();
            sync.markChanged();
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (chest != null && chest.getLevel() instanceof ServerLevel level) {
            sync.tick(() -> {
                CompoundTag tag = new CompoundTag();
                tag.putLong("own", chest.balance());
                DepositChests.Pool pool = DepositChests.find(level, pos);
                tag.putLong("pool", pool.balance());
                tag.putInt("chests", pool.chests().size());
                return tag;
            });
        }
    }
}
