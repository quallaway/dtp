package dev.dtp.supplies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

import dev.dtp.contraband.UseOutcome;

/** Amethyst-Katalysator: macht einen Amethystblock zu knospendem Amethyst, an dem wieder Kristalle wachsen. */
public class AmethystCatalystItem extends TraderConsumableItem {
    public AmethystCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        return finish(level, player, context.getItemInHand(), apply(level, context.getClickedPos()))
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    /** Die Wirkung auf einen Block, ohne Spieler (für Tests). */
    public static UseOutcome apply(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) {
            return UseOutcome.refused("dtp.supplies.amethyst_catalyst.wrong_block");
        }
        level.setBlockAndUpdate(pos, Blocks.BUDDING_AMETHYST.defaultBlockState());
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
        return UseOutcome.USED;
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }
}
