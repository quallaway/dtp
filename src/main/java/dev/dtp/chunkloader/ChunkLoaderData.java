package dev.dtp.chunkloader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.data.Versioned;

/**
 * Restlaufzeit aller Chunkloader des Servers, über alle Dimensionen. Die Daten sind die Wahrheit,
 * die Chunk-Tickets werden daraus abgeleitet ({@link ChunkLoaders}).
 */
public class ChunkLoaderData extends SavedData {
    private record Entry(GlobalPos pos, long remaining) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Codec.LONG.fieldOf("remaining").forGetter(Entry::remaining)
        ).apply(i, Entry::new));
    }

    private static final Codec<ChunkLoaderData> INNER = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("loaders", List.of()).forGetter(ChunkLoaderData::entries)
    ).apply(i, ChunkLoaderData::new));

    /** Version 1: erste Fassung. */
    public static final Codec<ChunkLoaderData> CODEC = Versioned.codec(1, INNER, List.of());

    public static final SavedDataType<ChunkLoaderData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "chunk_loaders"), ChunkLoaderData::new, CODEC, (DataFixTypes) null);

    private final Map<GlobalPos, Long> remaining = new LinkedHashMap<>();

    public ChunkLoaderData() {
    }

    private ChunkLoaderData(List<Entry> entries) {
        for (Entry e : entries) {
            if (e.remaining() > 0) {
                remaining.put(e.pos(), e.remaining());
            }
        }
    }

    public static ChunkLoaderData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Entry> entries() {
        return remaining.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList();
    }

    /** Restlaufzeit in Ticks, 0 wenn nicht geladen. */
    public long remaining(GlobalPos pos) {
        return remaining.getOrDefault(pos, 0L);
    }

    /** Setzt die Restlaufzeit; 0 oder weniger entfernt den Eintrag. */
    public void set(GlobalPos pos, long ticks) {
        if (ticks <= 0) {
            if (remaining.remove(pos) != null) {
                setDirty();
            }
        } else {
            remaining.put(pos, ticks);
            setDirty();
        }
    }

    public Map<GlobalPos, Long> all() {
        return Collections.unmodifiableMap(remaining);
    }
}
