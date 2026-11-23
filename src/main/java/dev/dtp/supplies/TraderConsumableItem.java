package dev.dtp.supplies;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.dtp.contraband.UseOutcome;

/**
 * Verbrauchsgegenstände vom Schwarzmarkthändler (Rückkehrrolle, Katalysatoren). Kein Contraband: keine Ladungen,
 * stapelbar bis 16, ein Erfolg verbraucht ein Stück. Ein {@link UseOutcome.Refused} kostet nichts.
 */
public abstract class TraderConsumableItem extends Item {
    protected TraderConsumableItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** Sound nach einer erfolgreichen Benutzung; {@code null}, wenn die Wirkung ihren Ton selbst spielt. */
    protected abstract SoundEvent useSound();

    /** Wertet ein Ergebnis aus: Meldung bei Ablehnung, sonst ein Stück verbrauchen und Ton spielen. */
    protected boolean finish(ServerLevel level, Player player, ItemStack stack, UseOutcome outcome) {
        if (outcome instanceof UseOutcome.Refused(Component reason)) {
            if (reason != null) {
                player.sendOverlayMessage(reason.copy().withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        SoundEvent sound = useSound();
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return true;
    }
}
