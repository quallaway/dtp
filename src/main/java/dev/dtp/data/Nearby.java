package dev.dtp.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Umkreissuche nach Lagern — für Einzahlkisten und Trade-Chests (siehe {@link DtpStorage}).
 *
 * Reine Abstandsprüfung, keine Sichtlinie. Sortiert: nächste zuerst, bei gleichem Abstand nach Koordinaten.
 * Durchsucht werden nur die Block-Entities geladener Chunks, nicht jede Blockposition im Radius.
 */
public final class Nearby {
    private Nearby() {
    }

    /**
     * Alle Lager mit der Capability {@code storage} ({@link DtpStorage#TRADE} oder {@link DtpStorage#DEPOSIT}) im
     * Radius. Geprüft werden nur Positionen mit Block-Entity; ob sie zählen, entscheidet die Capability — so zählt
     * auch ein Block aus einem anderen Mod, wenn er sie anbietet.
     */
    public static List<DtpStorage.Found> find(ServerLevel level, BlockPos center, int radius, BlockCapability<Container, Void> storage) {
        long maxDistSqr = (long) radius * radius;
        List<DtpStorage.Found> found = new ArrayList<>();
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (be.isRemoved() || pos.distSqr(center) > maxDistSqr) {
                        continue;
                    }
                    Container container = level.getCapability(storage, pos, be.getBlockState(), be, null);
                    if (container != null) {
                        found.add(new DtpStorage.Found(pos, container));
                    }
                }
            }
        }
        found.sort(Comparator.<DtpStorage.Found>comparingDouble(f -> f.pos().distSqr(center))
                .thenComparingInt(f -> f.pos().getX())
                .thenComparingInt(f -> f.pos().getY())
                .thenComparingInt(f -> f.pos().getZ()));
        return found;
    }
}
