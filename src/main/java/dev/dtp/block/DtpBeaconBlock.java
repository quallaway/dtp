package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import dev.dtp.beacon.BeaconLogic;
import dev.dtp.menu.BeaconMenu;

/** Der Mod-Beacon — nicht der Vanilla-Beacon. Nicht abbaubar, keine Pyramide, keine Energie. */
public class DtpBeaconBlock extends Block {
    public DtpBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (level instanceof ServerLevel serverLevel) {
            BeaconLogic.onPlaced(serverLevel, pos, by instanceof Player player ? player : null);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BeaconMenu(id, inventory, pos),
                    Component.translatable("block.dtp.beacon")), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
