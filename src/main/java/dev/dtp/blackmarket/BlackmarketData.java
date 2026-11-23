package dev.dtp.blackmarket;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.data.Versioned;
import dev.dtp.market.MarketData;

/**
 * Serverweiter Stand des Schwarzmarkts. Ein Tag ist ein Markttag ({@link MarketData#day()}): Die Uhr läuft nur, während
 * jemand online ist, Schlafen zählt mit.
 *
 * <p>Für den Schwarzmarkthändler steht hier die Spielzeit (Overworld-Ticks), ab der der nächste erscheinen darf. Sie
 * liegt hinter dem Ende des aktuellen Händlers, damit es serverweit immer nur einen gibt.
 */
public class BlackmarketData extends SavedData {
    private static final Codec<BlackmarketData> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("last_use_day", -1).forGetter(d -> d.lastUseDay),
            Codec.LONG.optionalFieldOf("next_trader_tick", 0L).forGetter(d -> d.nextTraderTick)
    ).apply(i, BlackmarketData::new));

    public static final Codec<BlackmarketData> CODEC = Versioned.codec(1, INNER, List.of());

    public static final SavedDataType<BlackmarketData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "blackmarket"), BlackmarketData::new, CODEC, (DataFixTypes) null);

    private static final long TICKS_PER_DAY = 24000L;

    /** Markttag der letzten Nutzung am Schwarzmarkt-Block, −1 für nie. */
    private int lastUseDay;

    /** Spielzeit, ab der der nächste Schwarzmarkthändler erscheinen darf. */
    private long nextTraderTick;

    public BlackmarketData() {
        this(-1, 0L);
    }

    private BlackmarketData(int lastUseDay, long nextTraderTick) {
        this.lastUseDay = lastUseDay;
        this.nextTraderTick = nextTraderTick;
    }

    public static BlackmarketData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static int today(MinecraftServer server) {
        return (int) Math.floor(MarketData.get(server).day());
    }

    public boolean usedToday(MinecraftServer server) {
        return lastUseDay >= today(server);
    }

    public void markUsed(MinecraftServer server) {
        lastUseDay = today(server);
        setDirty();
    }

    public int lastUseDay() {
        return lastUseDay;
    }

    /** Für Tests und Admins: den Tag der letzten Nutzung setzen. */
    public void setLastUseDay(int day) {
        lastUseDay = day;
        setDirty();
    }

    /** Spielzeit in Ticks bis zum nächsten Markttag. */
    public static long ticksUntilNextDay(MinecraftServer server) {
        double day = MarketData.get(server).day();
        return Math.max(1L, (long) Math.ceil((Math.floor(day) + 1.0 - day) * TICKS_PER_DAY));
    }

    public long nextTraderTick() {
        return nextTraderTick;
    }

    public void setNextTraderTick(long tick) {
        nextTraderTick = tick;
        setDirty();
    }
}
