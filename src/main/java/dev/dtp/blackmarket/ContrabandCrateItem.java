package dev.dtp.blackmarket;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import dev.dtp.currency.Payout;
import dev.dtp.registry.ModDataComponents;

/**
 * Schmuggelkiste: eine Lootbox vom Schwarzmarkthändler. Welche Loot-Table sie würfelt, steht in der Komponente
 * {@code dtp:crate_loot}, ihren Namen setzt das Händlerangebot über {@code minecraft:item_name}. Eine neue Sorte braucht
 * also nur eine Loot-Table und ein Angebot. Die Kisten sollen Lücken schließen (eine fehlende Holzart, ein Schnüffler-Ei),
 * keine Sammlungen abkürzen.
 */
public class ContrabandCrateItem extends Item {
    public ContrabandCrateItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ItemStack base, ResourceKey<LootTable> table) {
        ItemStack stack = base.copy();
        stack.set(ModDataComponents.CRATE_LOOT.get(), table);
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack crate = player.getItemInHand(hand);
        if (crate.get(ModDataComponents.CRATE_LOOT.get()) == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            List<ItemStack> loot = roll(serverLevel, crate, player);
            loot.forEach(stack -> Payout.toPlayer(player, stack));
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.6F, 1.4F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
            if (!loot.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("dtp.crate.opened", loot.getFirst().getHoverName()).withStyle(ChatFormatting.GOLD));
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        crate.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /** Würfelt den Inhalt einer Kiste, ohne sie zu verbrauchen. Leer, wenn die Kiste keine Loot-Table trägt. */
    public static List<ItemStack> roll(ServerLevel level, ItemStack crate, Entity opener) {
        ResourceKey<LootTable> key = crate.get(ModDataComponents.CRATE_LOOT.get());
        if (key == null) {
            return List.of();
        }
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, opener.position())
                .withParameter(LootContextParams.THIS_ENTITY, opener)
                .create(LootContextParamSets.GIFT);
        return level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.dtp.contraband_crate.hint").withStyle(ChatFormatting.GRAY));
    }
}
