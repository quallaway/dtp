package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.data.Versioned;

/**
 * Der gemeinsame Fortschritt des Servers: Hauptbeacon, Server-Upgrades und aktive Beacon-Effekte.
 * Der Fortschritt hängt am Server, nicht am Block.
 */
public class WorldProgress extends SavedData {
    /** Ein Neben-Beacon mit seiner eigenen Effektauswahl. Ohne Dimension (vor Version 3) liegt er in der Oberwelt. */
    private record Relay(ResourceKey<Level> dimension, BlockPos pos, List<String> effects) {
        static final Codec<Relay> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension", Level.OVERWORLD).forGetter(Relay::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Relay::pos),
                Codec.STRING.listOf().optionalFieldOf("effects", List.of()).forGetter(Relay::effects)
        ).apply(i, Relay::new));
    }

    private static final Codec<WorldProgress> INNER = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("beacon").forGetter(d -> Optional.ofNullable(d.beaconPos)),
            Codec.BOOL.optionalFieldOf("started", false).forGetter(d -> d.started),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("upgrades", Map.of()).forGetter(d -> d.upgrades),
            Codec.STRING.listOf().optionalFieldOf("active_effects", List.of()).forGetter(d -> d.activeEffects),
            Codec.INT.optionalFieldOf("completed_contracts", 0).forGetter(d -> d.completedContracts),
            Relay.CODEC.listOf().optionalFieldOf("relays", List.of()).forGetter(d -> d.relayList())
    ).apply(i, WorldProgress::new));

    /**
     * Version 2: Neben-Beacons. Ältere Stände haben schlicht keine — das neue Feld ist optional.
     * Version 3: Neben-Beacons in allen Dimensionen. Ältere Einträge haben keine Dimension und gelten als Oberwelt.
     * Version 4: Der Effekt Kurze Zuchtpause wurde zu Landwirtschaft, ID {@code effect_breeding} → {@code effect_agriculture}
     * in gekauften Upgrades, aktiven Effekten und der Auswahl der Neben-Beacons.
     * Version 5: Beacon-Effekte zusammengelegt. {@code effect_horse_breeding} heißt {@code effect_stablemaster},
     * {@code effect_roads} geht im Stallmeister auf, {@code effect_rebuild} im Baumeister ({@code effect_builder}). Wer
     * einen Teil besaß, besitzt den zusammengelegten Effekt; doppelte Einträge in Auswahl und Neben-Beacons entfallen.
     */
    public static final Codec<WorldProgress> CODEC = Versioned.codec(5, INNER, List.of(new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    }, new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    }, new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return renameUpgrade(data, "effect_breeding", "effect_agriculture");
        }
    }, new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            Dynamic<T> renamed = renameUpgrade(data, "effect_horse_breeding", "effect_stablemaster");
            return mergeUpgrade(mergeUpgrade(renamed, "effect_roads", "effect_stablemaster"), "effect_rebuild", "effect_builder");
        }
    }));

    /**
     * Legt eine Upgrade-ID in eine andere zusammen: Die Stufe von {@code into} ist danach die höhere der beiden, in
     * aktiven Effekten und Neben-Beacons ersetzt {@code into} die alte ID, ohne doppelt aufzutauchen.
     */
    static <T> Dynamic<T> mergeUpgrade(Dynamic<T> data, String from, String into) {
        return data
                .update("upgrades", upgrades -> mergeKey(upgrades, from, into))
                .update("active_effects", ids -> distinct(renameInList(ids, from, into)))
                .update("relays", relays -> relays.createList(relays.asStream()
                        .map(relay -> relay.update("effects", ids -> distinct(renameInList(ids, from, into))))));
    }

    private static <T> Dynamic<T> mergeKey(Dynamic<T> map, String from, String into) {
        return map.get(from).result().map(value -> {
            int level = Math.max(value.asInt(0), map.get(into).asInt(0));
            return map.remove(from).set(into, map.createInt(level));
        }).orElse(map);
    }

    private static <T> Dynamic<T> distinct(Dynamic<T> ids) {
        Set<String> seen = new HashSet<>();
        return ids.createList(ids.asStream().filter(id -> {
            String s = id.asString(null);
            return s == null || seen.add(s);
        }));
    }

    /** Benennt eine Upgrade-ID im gespeicherten Stand um: Stufen, aktive Effekte und Auswahl der Neben-Beacons. */
    static <T> Dynamic<T> renameUpgrade(Dynamic<T> data, String from, String to) {
        return data
                .update("upgrades", upgrades -> renameKey(upgrades, from, to))
                .update("active_effects", ids -> renameInList(ids, from, to))
                .update("relays", relays -> renameInRelays(relays, from, to));
    }

    private static <T> Dynamic<T> renameKey(Dynamic<T> map, String from, String to) {
        return map.get(from).result().map(value -> map.remove(from).set(to, value)).orElse(map);
    }

    private static <T> Dynamic<T> renameInRelays(Dynamic<T> relays, String from, String to) {
        return relays.createList(relays.asStream().map(relay -> relay.update("effects", ids -> renameInList(ids, from, to))));
    }

    private static <T> Dynamic<T> renameInList(Dynamic<T> ids, String from, String to) {
        return ids.createList(ids.asStream()
                .map(id -> from.equals(id.asString(null)) ? id.createString(to) : id));
    }

    public static final SavedDataType<WorldProgress> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "progress"), WorldProgress::new, CODEC, (DataFixTypes) null);

    private BlockPos beaconPos;
    private boolean started;
    private final Map<String, Integer> upgrades;
    private final List<String> activeEffects;
    /** Abgeschlossene Contracts aller Spieler; Voraussetzung für die Contract-Stufen am Beacon. */
    private int completedContracts;
    /** Neben-Beacons nach Dimension und Position, jeweils mit ihrer Effektauswahl (Upgrade-IDs). */
    private final Map<GlobalPos, List<String>> relays = new LinkedHashMap<>();

    public WorldProgress() {
        this(Optional.empty(), false, Map.of(), List.of(), 0, List.of());
    }

    private WorldProgress(Optional<BlockPos> beaconPos, boolean started, Map<String, Integer> upgrades, List<String> activeEffects,
            int completedContracts, List<Relay> relays) {
        this.beaconPos = beaconPos.orElse(null);
        this.started = started;
        this.upgrades = new HashMap<>(upgrades);
        this.activeEffects = new ArrayList<>(activeEffects);
        this.completedContracts = completedContracts;
        relays.forEach(r -> this.relays.put(GlobalPos.of(r.dimension(), r.pos()), new ArrayList<>(r.effects())));
    }

    private List<Relay> relayList() {
        List<Relay> list = new ArrayList<>();
        relays.forEach((pos, effects) -> list.add(new Relay(pos.dimension(), pos.pos(), effects)));
        return list;
    }

    public static WorldProgress get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Beacon ------------------------------------------------------------------------------

    /** Position des Hauptbeacons in der Oberwelt, auch wenn der Durchlauf noch nicht bestätigt ist. */
    public BlockPos beaconPos() {
        return beaconPos;
    }

    /** Der Durchlauf hat begonnen: Beacon bestätigt, Grenze gesetzt. */
    public boolean started() {
        return started && beaconPos != null;
    }

    public void setBeacon(BlockPos pos, boolean started) {
        this.beaconPos = pos == null ? null : pos.immutable();
        this.started = pos != null && started;
        setDirty();
    }

    /**
     * Halbe Kantenlänge des Territoriums in Blöcken. Das Territorium ist die Vanilla-World-Border
     * und damit quadratisch. Nach der letzten Grenzstufe gilt die Größe der höchsten Stufe weiter.
     */
    public int territoryRadius() {
        int stage = level(ServerUpgrade.BORDER);
        List<? extends Integer> chunks = DtpConfig.RADIUS_CHUNKS.get();
        return chunks.get(Math.min(stage, chunks.size() - 1)) * 16;
    }

    /**
     * Wirkungsbereich des Hauptbeacons: fest das Gebiet der höchsten Grenzstufe (halbe Kantenlänge in Blöcken).
     * Solange eine Grenze steht, deckt das jedes erreichbare Fleckchen ab; danach versorgen Neben-Beacons den Rest.
     */
    public static int mainEffectRadius() {
        List<? extends Integer> chunks = DtpConfig.RADIUS_CHUNKS.get();
        return chunks.getLast() * 16;
    }

    public boolean borderRemoved() {
        return level(ServerUpgrade.BORDER) >= DtpConfig.RADIUS_CHUNKS.get().size();
    }

    /** Liegt die Position (Oberwelt) im Territorium? Vor dem Start gibt es kein Territorium. */
    public boolean inTerritory(BlockPos pos) {
        if (!started()) {
            return false;
        }
        if (borderRemoved()) {
            return true;
        }
        int r = territoryRadius();
        return Math.abs(pos.getX() - beaconPos.getX()) <= r && Math.abs(pos.getZ() - beaconPos.getZ()) <= r;
    }

    // --- Upgrades ----------------------------------------------------------------------------

    public int level(ServerUpgrade upgrade) {
        return upgrades.getOrDefault(upgrade.id, 0);
    }

    public void setLevel(ServerUpgrade upgrade, int level) {
        upgrades.put(upgrade.id, level);
        if (upgrade.isEffect() && level == 0) {
            activeEffects.remove(upgrade.id);
        }
        setDirty();
    }

    // --- Contracts ---------------------------------------------------------------------------

    public int completedContracts() {
        return completedContracts;
    }

    public void setCompletedContracts(int count) {
        completedContracts = count;
        setDirty();
    }

    /**
     * Fehlende Contract-Abschlüsse für die nächste Stufe von {@link ServerUpgrade#CONTRACT_TIER}; 0 = kaufbar.
     * Die Voraussetzung gilt serverweit, damit die erste Stufe (0 Abschlüsse) Contracts überhaupt erst freischaltet.
     */
    public int missingForNextContractTier() {
        int nextTier = DtpConfig.BASE_MAX_TIER.get() + level(ServerUpgrade.CONTRACT_TIER) + 1;
        List<? extends Integer> requirements = DtpConfig.TIER_REQUIREMENT.get();
        int required = requirements.isEmpty() ? 0 : requirements.get(Math.min(nextTier - 1, requirements.size() - 1));
        return Math.max(0, required - completedContracts);
    }

    // --- Effekte -----------------------------------------------------------------------------

    public int effectSlots() {
        return DtpConfig.BASE_EFFECT_SLOTS.get() + level(ServerUpgrade.EFFECT_SLOTS);
    }

    public List<ServerUpgrade> activeEffects() {
        return unlockedEffects(activeEffects, effectSlots());
    }

    /** Freigeschaltete Effekte aus einer Auswahl, höchstens {@code slots} — sinkt die Slotzahl, fallen die letzten weg. */
    private List<ServerUpgrade> unlockedEffects(List<String> ids, int slots) {
        List<ServerUpgrade> result = new ArrayList<>();
        for (String id : ids) {
            ServerUpgrade u = ServerUpgrade.byId(id);
            if (u != null && u.isEffect() && level(u) > 0 && result.size() < slots) {
                result.add(u);
            }
        }
        return result;
    }

    /** Schaltet einen freigeschalteten Effekt um. Umstecken ist kostenlos und jederzeit möglich. */
    public boolean toggleEffect(ServerUpgrade effect) {
        if (!effect.isEffect() || level(effect) == 0) {
            return false;
        }
        return toggle(activeEffects, effectSlots(), effect);
    }

    /**
     * Gemeinsame Umschaltlogik. Einträge, die nicht mehr wirken (gesperrt oder jenseits der Slotzahl), werden dabei
     * verworfen — sonst rückte nach dem Abschalten unerwartet ein alter Effekt nach.
     */
    private boolean toggle(List<String> ids, int slots, ServerUpgrade effect) {
        if (ids.remove(effect.id)) {
            setDirty();
            return true;
        }
        List<ServerUpgrade> visible = unlockedEffects(ids, slots);
        if (visible.size() >= slots) {
            return false;
        }
        ids.clear();
        visible.forEach(u -> ids.add(u.id));
        ids.add(effect.id);
        setDirty();
        return true;
    }

    // --- Neben-Beacons -----------------------------------------------------------------------

    /** Ein Slot weniger als der Hauptbeacon, aber immer mindestens einer. */
    public int relaySlots() {
        return Math.max(1, effectSlots() - 1);
    }

    public Set<GlobalPos> relays() {
        return Collections.unmodifiableSet(relays.keySet());
    }

    public boolean isRelay(GlobalPos pos) {
        return relays.containsKey(pos);
    }

    public void addRelay(GlobalPos pos) {
        if (relays.putIfAbsent(GlobalPos.of(pos.dimension(), pos.pos().immutable()), new ArrayList<>()) == null) {
            setDirty();
        }
    }

    public void removeRelay(GlobalPos pos) {
        if (relays.remove(pos) != null) {
            setDirty();
        }
    }

    public List<ServerUpgrade> relayEffects(GlobalPos pos) {
        List<String> ids = relays.get(pos);
        return ids == null ? List.of() : unlockedEffects(ids, relaySlots());
    }

    /** Wie {@link #toggleEffect}, aber für die eigene Auswahl eines Neben-Beacons. */
    public boolean toggleRelayEffect(GlobalPos pos, ServerUpgrade effect) {
        List<String> ids = relays.get(pos);
        if (ids == null || !effect.isEffect() || level(effect) == 0) {
            return false;
        }
        return toggle(ids, relaySlots(), effect);
    }
}
