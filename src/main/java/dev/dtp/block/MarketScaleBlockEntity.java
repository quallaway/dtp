package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.dtp.market.Market;
import dev.dtp.registry.ModBlocks;

/**
 * Hält das Filter-Item der Marktwaage — eine Kopie, kein echter Inhalt — und das daraus berechnete
 * Comparator-Signal. Der Marktdruck klingt mit der Marktzeit ab und springt bei Ereignissen und fremden
 * Verkäufen, ohne dass ein Blockupdate fällt; deshalb wird einmal pro Sekunde neu gerechnet.
 */
public class MarketScaleBlockEntity extends BlockEntity {
    private static final int INTERVAL = 20;

    private ItemStack filter = ItemStack.EMPTY;
    private int signal;

    public MarketScaleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MARKET_SCALE_BE.get(), pos, state);
    }

    public ItemStack filter() {
        return filter;
    }

    public int signal() {
        return signal;
    }

    /** Setzt den Filter (leer = löschen); das Item wird nicht verbraucht. */
    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            refresh(serverLevel);
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MarketScaleBlockEntity scale) {
        // Versetzt je Position, damit nicht alle Waagen im selben Tick rechnen.
        if (level instanceof ServerLevel serverLevel && (level.getGameTime() + pos.hashCode()) % INTERVAL == 0) {
            scale.refresh(serverLevel);
        }
    }

    /** Rechnet das Signal neu und meldet eine Änderung an Comparatoren in der Nähe. */
    public void refresh(ServerLevel level) {
        int next = filter.isEmpty() ? 0
                : Market.factorAt(level, worldPosition, filter).map(Market::comparatorSignal).orElse(0);
        if (next != signal) {
            signal = next;
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter = input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        signal = input.getIntOr("signal", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!filter.isEmpty()) {
            output.store("filter", ItemStack.CODEC, filter);
        }
        output.putInt("signal", signal);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
