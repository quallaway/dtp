package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.dtp.menu.RelayBeaconMenu;
import dev.dtp.upgrade.WorldProgress;

/**
 * Neben-Beacon: strahlt eine eigene Auswahl der am Hauptbeacon freigeschalteten Effekte in kleinerem Radius aus.
 * Über ihn lässt sich nichts kaufen. Die Auswahl liegt am Server ({@link WorldProgress}), nicht am Block — so
 * lassen sich die Effekte verteilen, ohne die Welt nach Neben-Beacons zu durchsuchen.
 */
public class RelayBeaconBlock extends Block {
    public RelayBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            WorldProgress.get(serverLevel.getServer()).addRelay(GlobalPos.of(level.dimension(), pos));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        WorldProgress.get(level.getServer()).removeRelay(GlobalPos.of(level.dimension(), pos));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            // Vor Version 3 wurden Neben-Beacons außerhalb der Oberwelt nicht registriert — beim Öffnen nachholen.
            WorldProgress.get(serverLevel.getServer()).addRelay(GlobalPos.of(level.dimension(), pos));
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new RelayBeaconMenu(id, inventory, pos),
                    Component.translatable("block.dtp.relay_beacon")), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
