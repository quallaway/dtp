package dev.dtp.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.market.MarketData;

/**
 * Messprotokoll für Balancing-Runden (docs/62-balancing.md): eine CSV-Zeile pro Verkauf, Kauf und
 * Contract-Abschluss, auswertbar zwischen zwei Sitzungen.
 */
public final class Protocol {
    private static final String HEADER = "real_time,market_day,player,action,subject,count,amount\n";

    private Protocol() {
    }

    public static void log(MinecraftServer server, Player player, String action, String subject, long count, long amount) {
        if (!DtpConfig.PROTOCOL.get()) {
            return;
        }
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("dtp").resolve("protocol.csv");
        String line = String.format(Locale.ROOT, "%s,%.3f,%s,%s,%s,%d,%d%n",
                Instant.now(), MarketData.get(server).day(),
                player == null ? "-" : player.getGameProfile().name(), action, subject, count, amount);
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                Files.writeString(file, HEADER, StandardCharsets.UTF_8);
            }
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Dtp.LOGGER.warn("Could not write protocol entry", e);
        }
    }
}
