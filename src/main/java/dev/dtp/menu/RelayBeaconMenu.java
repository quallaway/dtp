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
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/** Neben-Beacon: nur Effektauswahl, keine Käufe. Knopf-IDs wie in {@link EffectSelection}. */
public class RelayBeaconMenu extends BlockBoundMenu {
    private Component message;

    public RelayBeaconMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.RELAY_BEACON.get(), id, inventory, pos, ModBlocks.RELAY_BEACON.get());
    }

    public static RelayBeaconMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new RelayBeaconMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerUpgrade effect = EffectSelection.byButton(buttonId);
        if (effect == null) {
            return false;
        }
        WorldProgress progress = WorldProgress.get(level.getServer());
        message = progress.toggleRelayEffect(GlobalPos.of(level.dimension(), pos), effect) ? null
                : Component.translatable(progress.level(effect) == 0 ? "dtp.effects.locked" : "dtp.beacon.no_effect_slot");
        sync.markChanged();
        return true;
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        CompoundTag tag = new CompoundTag();
        GlobalPos relay = GlobalPos.of(level.dimension(), pos);
        // Ein Neben-Beacon wirkt immer: ohne Sperre, unabhängig von Durchlauf und Territorium.
        tag.putInt("radius", DtpConfig.RELAY_RADIUS_CHUNKS.get() * 16);
        EffectSelection.put(tag, progress, progress.relayEffects(relay), progress.relaySlots());
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }
}
