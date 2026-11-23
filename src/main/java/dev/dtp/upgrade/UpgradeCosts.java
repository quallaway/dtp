package dev.dtp.upgrade;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;

import dev.dtp.Dtp;

/**
 * Lädt Kosten, Maximalstufe und Lage im Beacon-Baum der Server-Upgrades aus {@code data/<ns>/dtp/upgrade/<id>.json}.
 *
 * Die Preisformel steht in {@link CostCurve}. Ein Upgrade ohne Datei ist nicht kaufbar.
 */
public class UpgradeCosts extends SimpleJsonResourceReloadListener<UpgradeCosts.Definition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "upgrades");

    /** Die Reiter des Beacon-Baums; die Effektauswahl ist ein eigener Reiter ohne Baum. */
    public enum Tab implements StringRepresentable {
        WORLD("world"),
        MARKET("market"),
        MODULES("modules");

        public static final Codec<Tab> CODEC = StringRepresentable.fromEnum(Tab::values);

        public final String id;

        Tab(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String translationKey() {
            return "dtp.beacon.tab." + id;
        }
    }

    /**
     * Maximalstufe, Kosten und Lage im Beacon-Baum: Reiter, Symbol, Platz, optional ein Feld je Stufe
     * ({@link TreeLayout}) und Voraussetzungen (andere Server-Upgrades).
     */
    public record Definition(int maxLevel, CostCurve curve, Tab tab, UpgradeIcon icon, int x, int y,
            List<List<Integer>> levelPositions, List<UpgradeIcon> levelIcons, List<PlayerUpgradeDefinitions.Requirement> requires) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 1000).fieldOf("max_level").forGetter(Definition::maxLevel),
                CostCurve.MAP_CODEC.forGetter(Definition::curve),
                Tab.CODEC.optionalFieldOf("tab", Tab.WORLD).forGetter(Definition::tab),
                UpgradeIcon.CODEC.optionalFieldOf("icon", UpgradeIcon.BARRIER).forGetter(Definition::icon),
                Codec.intRange(0, 100).optionalFieldOf("x", 0).forGetter(Definition::x),
                Codec.intRange(0, 100).optionalFieldOf("y", 0).forGetter(Definition::y),
                TreeLayout.LEVEL_POSITIONS.optionalFieldOf("level_positions", List.of()).forGetter(Definition::levelPositions),
                UpgradeIcon.CODEC.listOf().optionalFieldOf("level_icons", List.of()).forGetter(Definition::levelIcons),
                PlayerUpgradeDefinitions.Requirement.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Definition::requires)
        ).apply(i, Definition::new));

        /** Preis für das Erreichen von {@code level} (1-basiert). */
        public long cost(int level) {
            return curve.cost(level);
        }

        public boolean split() {
            return TreeLayout.split(maxLevel, levelPositions);
        }

        public int[] position(int level) {
            return TreeLayout.position(x, y, maxLevel, levelPositions, level);
        }

        /** Symbol des Feldes für {@code level}; {@code level_icons} gelten ab Stufe 2. */
        public UpgradeIcon icon(int level) {
            return UpgradeIcon.forLevel(icon, levelIcons, level);
        }
    }

    private static Map<ServerUpgrade, Definition> definitions = Map.of();

    public UpgradeCosts() {
        super(Definition.CODEC, FileToIdConverter.json("dtp/upgrade"));
    }

    @Override
    protected void apply(Map<Identifier, Definition> loaded, ResourceManager manager, ProfilerFiller profiler) {
        Map<ServerUpgrade, Definition> result = new EnumMap<>(ServerUpgrade.class);
        loaded.forEach((id, def) -> {
            ServerUpgrade upgrade = ServerUpgrade.byId(id.getPath());
            if (upgrade == null) {
                Dtp.LOGGER.warn("Unknown server upgrade '{}' in datapack, ignored", id);
            } else {
                result.put(upgrade, def);
            }
        });
        definitions = result;
        Dtp.LOGGER.info("Loaded {} upgrade definitions", result.size());
    }

    public static Definition get(ServerUpgrade upgrade) {
        return definitions.get(upgrade);
    }
}
