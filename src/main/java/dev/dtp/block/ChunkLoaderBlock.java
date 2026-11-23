package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import dev.dtp.chunkloader.ChunkLoaders;
import dev.dtp.menu.ChunkLoaderMenu;

/**
 * Chunkloader: Glaskasten mit schwebender Enderperle. Gegen Taler aus Einzahlkisten in Reichweite hält er die
 * Chunks um sich geladen ({@link ChunkLoaders}). {@link #ACTIVE} zeigt nur an, ob er Restlaufzeit hat — die Perle
 * schwebt dann; die Laufzeit selbst liegt am Server.
 */
public class ChunkLoaderBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ChunkLoaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        ChunkLoaders.remove(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            ChunkLoaders.syncState(serverLevel, pos);
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChunkLoaderMenu(id, inventory, pos),
                    Component.translatable("block.dtp.chunk_loader")), pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.6, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state);
    }
}
