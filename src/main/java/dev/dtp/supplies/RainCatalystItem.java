package dev.dtp.supplies;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.WeatherData;

import dev.dtp.contraband.UseOutcome;

/**
 * Regen-Katalysator: „Es verschlechtert das Wetter.“ Klarer Himmel wird Regen, Regen wird Gewitter, ein Gewitter
 * hält mindestens {@link #MIN_TICKS} an. Nur in der Oberwelt.
 */
public class RainCatalystItem extends TraderConsumableItem {
    /** 10 Minuten. */
    public static final int MIN_TICKS = 12_000;

    public RainCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        return finish(serverLevel, player, player.getItemInHand(hand), apply(serverLevel))
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    /** Die Wirkung auf das Wetter, ohne Spieler (für Tests). */
    public static UseOutcome apply(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return UseOutcome.refused("dtp.supplies.rain_catalyst.no_weather");
        }
        WeatherData weather = level.getWeatherData();
        if (!weather.isRaining()) {
            weather.setClearWeatherTime(0);
            weather.setRaining(true);
            weather.setRainTime(Math.max(weather.getRainTime(), MIN_TICKS));
            weather.setThundering(false);
            weather.setThunderTime(Math.max(weather.getThunderTime(), MIN_TICKS));
            return UseOutcome.USED;
        }
        if (!weather.isThundering()) {
            weather.setThundering(true);
            weather.setThunderTime(MIN_TICKS);
            weather.setRainTime(Math.max(weather.getRainTime(), MIN_TICKS));
            return UseOutcome.USED;
        }
        if (weather.getThunderTime() >= MIN_TICKS && weather.getRainTime() >= MIN_TICKS) {
            return UseOutcome.refused("dtp.supplies.rain_catalyst.worst");
        }
        weather.setThunderTime(Math.max(weather.getThunderTime(), MIN_TICKS));
        weather.setRainTime(Math.max(weather.getRainTime(), MIN_TICKS));
        return UseOutcome.USED;
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.LIGHTNING_BOLT_THUNDER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.dtp.rain_catalyst.hint").withStyle(ChatFormatting.GRAY));
    }
}
