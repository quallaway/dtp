package dev.dtp.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import dev.dtp.DtpConfig;
import dev.dtp.chunkloader.ChunkLoaderData;
import dev.dtp.chunkloader.ChunkLoaders;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Money;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;

/** Chunkloader: Restlaufzeit anzeigen und eine Ladung kaufen. */
public class ChunkLoaderMenu extends BlockBoundMenu {
    public static final int CHARGE = 1;

    private Component message;

    public ChunkLoaderMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.CHUNK_LOADER.get(), id, inventory, pos, ModBlocks.CHUNK_LOADER.get());
    }

    public static ChunkLoaderMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new ChunkLoaderMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != CHARGE || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        message = switch (ChunkLoaders.charge(level, pos, player)) {
            case ChunkLoaders.Charged c -> Component.translatable("dtp.chunkloader.charged", minutes(c.remaining()));
            case ChunkLoaders.Disabled d -> Component.translatable("dtp.chunkloader.disabled");
            case ChunkLoaders.Full f -> Component.translatable("dtp.chunkloader.full");
            case ChunkLoaders.Insufficient i -> Component.translatable("dtp.upgrade.insufficient", Money.component(i.missing()));
        };
        sync.markChanged();
        return true;
    }

    /** Laufzeit in Minuten, aufgerundet: Der Chunkloader zählt Ticks und springt beim Schlafen nicht vor. */
    private static long minutes(long ticks) {
        return (ticks + 1199) / 1200;
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        long duration = DtpConfig.CHUNKLOADER_DURATION_TICKS.get();
        tag.putLong("remaining", ChunkLoaderData.get(level.getServer()).remaining(GlobalPos.of(level.dimension(), pos)));
        tag.putLong("max", ChunkLoaders.maxTicks());
        tag.putLong("duration", duration);
        tag.putLong("price", DtpConfig.CHUNKLOADER_PRICE.get());
        int r = DtpConfig.CHUNKLOADER_RADIUS_CHUNKS.get();
        tag.putInt("size", 2 * r + 1);
        tag.putBoolean("enabled", DtpConfig.CHUNKLOADER_ENABLED.get());
        tag.putBoolean("ticketed", ChunkLoaders.isTicketed(level, pos));
        DepositChests.Pool pool = DepositChests.find(level, pos, ChunkLoaders.depositRadius(level));
        tag.putLong("balance", pool.balance());
        tag.putInt("chests", pool.chests().size());
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }
}
