package dev.dtp.supplies;

import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import dev.dtp.contraband.UseOutcome;
import dev.dtp.upgrade.WorldProgress;

/**
 * Rückkehrrolle: {@link #READ_SECONDS} Sekunden lesen (Rechtsklick halten), dann Teleport auf den Hauptbeacon in der
 * Oberwelt, aus jeder Dimension. Ohne gesetzten Beacon beginnt das Lesen gar nicht erst.
 */
public class ReturnScrollItem extends TraderConsumableItem {
    public static final float READ_SECONDS = 3.0F;

    public ReturnScrollItem(Properties properties) {
        super(properties.component(DataComponents.CONSUMABLE, Consumable.builder()
                .consumeSeconds(READ_SECONDS)
                .animation(ItemUseAnimation.TOOT_HORN)
                .sound(Holder.direct(SoundEvents.BOOK_PAGE_TURN))
                .hasConsumeParticles(false)
                .build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && target(serverLevel.getServer()) == null) {
            player.sendOverlayMessage(Component.translatable("dtp.supplies.return_scroll.no_beacon").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    /** Nach dem Lesen: Teleport statt Vanilla-Verzehr. Das Stück verbraucht nur ein gelungener Teleport. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            finish(serverLevel, player, stack, apply(serverLevel.getServer(), player));
        }
        return stack;
    }

    /** Freier Platz für zwei Blöcke über dem Hauptbeacon, oder {@code null} ohne Beacon. */
    public static BlockPos target(MinecraftServer server) {
        WorldProgress progress = WorldProgress.get(server);
        BlockPos beacon = progress.beaconPos();
        if (beacon == null) {
            return null;
        }
        ServerLevel overworld = server.overworld();
        BlockPos pos = beacon.above();
        for (int i = 0; i < 16 && !(overworld.isEmptyBlock(pos) && overworld.isEmptyBlock(pos.above())); i++) {
            pos = pos.above();
        }
        return pos;
    }

    /** Teleportiert {@code player} zum Hauptbeacon. Mock-Spieler aus GameTests bleiben in ihrer Dimension. */
    public static UseOutcome apply(MinecraftServer server, Player player) {
        BlockPos target = target(server);
        if (target == null) {
            return UseOutcome.refused("dtp.supplies.return_scroll.no_beacon");
        }
        ServerLevel overworld = server.overworld();
        if (player.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
        }
        double x = target.getX() + 0.5;
        double z = target.getZ() + 0.5;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(overworld, x, target.getY(), z, Set.of(), player.getYRot(), player.getXRot(), true);
        } else {
            player.teleportTo(x, target.getY(), z);
        }
        player.resetFallDistance();
        overworld.sendParticles(ParticleTypes.PORTAL, x, target.getY() + 1.0, z, 30, 0.4, 0.8, 0.4, 0.2);
        return UseOutcome.USED;
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.ENDERMAN_TELEPORT;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.dtp.return_scroll.hint").withStyle(ChatFormatting.GRAY));
    }
}
