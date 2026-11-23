package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.dtp.registry.ModBlocks;

/** Ohne eigenen Zustand: nur der Anker für den Renderer der schwebenden Enderperle. */
public class ChunkLoaderBlockEntity extends BlockEntity {
    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CHUNK_LOADER_BE.get(), pos, state);
    }

    public boolean active() {
        return getBlockState().getValue(ChunkLoaderBlock.ACTIVE);
    }
}
