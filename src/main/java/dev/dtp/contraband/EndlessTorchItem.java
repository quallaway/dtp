package dev.dtp.contraband;

import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Endlose Fackel: „Alles hat ein Ende, nur diese Fackel nicht.“ Setzt eine normale Fackel (Boden oder Wand, wie die
 * Vanilla-Fackel) und kostet dafür eine Ladung. Ein Klick in die Luft tut nichts.
 */
public class EndlessTorchItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(256, 1, 0.25, 10, false, Map.of());

    public EndlessTorchItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    @Override
    protected boolean usesBlocks() {
        return true;
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        return UseOutcome.SILENT;
    }

    @Override
    protected UseOutcome activateOn(ServerLevel level, Player player, ItemStack stack, UseOnContext context, ContrabandDefinition def) {
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        BlockPlaceContext place = new BlockPlaceContext(level, player, context.getHand(), new ItemStack(Items.TORCH), hit);
        if (!((BlockItem) Items.TORCH).place(place).consumesAction()) {
            return UseOutcome.SILENT;
        }
        // Vanilla spielt den Platzier-Ton für alle außer dem Platzierenden (dessen Client sagt ihn sonst selbst
        // vorher). Hier läuft alles serverseitig, also bekommt der Platzierende den Ton nachgereicht.
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos pos = place.getClickedPos();
            SoundType sound = level.getBlockState(pos).getSoundType();
            serverPlayer.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.getPlaceSound()), SoundSource.BLOCKS,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F, level.getRandom().nextLong()));
        }
        return UseOutcome.USED;
    }

    /** Den Platzier-Ton spielt die Fackel selbst (siehe {@link #activateOn}). */
    @Override
    protected SoundEvent useSound() {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.dtp.endless_torch.hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
