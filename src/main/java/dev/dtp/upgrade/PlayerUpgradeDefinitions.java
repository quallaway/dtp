package dev.dtp.upgrade;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Either;
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
 * Lädt die Spieler-Upgrades aus {@code data/<ns>/dtp/player_upgrade/<id>.json}: Tab und Position im Baum,
 * Symbol, Maximalstufe, Kosten ({@link CostCurve}), Wirkungswerte je Stufe, Voraussetzungen und benannte
 * Parameter ({@code params}, etwa Abklingzeit und Dauer der Module).
 * Ein Upgrade ohne Datei erscheint nicht im Baum.
 */
public class PlayerUpgradeDefinitions extends SimpleJsonResourceReloadListener<PlayerUpgradeDefinitions.Definition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "player_upgrades");

    /** Die Tabs des Upgrade-Baums (docs/30-persoenliche-upgrades.md). */
    public enum Tab implements StringRepresentable {
        BODY("body"),
        EQUIPMENT("equipment"),
        TRADE("trade"),
        FARM("farm");

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
            return "dtp.workshop.tab." + id;
        }
    }

    /** Ein anderes Spieler-Upgrade muss mindestens auf {@code level} stehen. */
    public record Requirement(String upgrade, int level) {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("upgrade").forGetter(Requirement::upgrade),
                Codec.intRange(1, 1000).optionalFieldOf("level", 1).forGetter(Requirement::level)
        ).apply(i, Requirement::new));
    }

    /** Ein Parameter ist eine Zahl (gilt für alle Stufen) oder eine Liste mit einem Wert je Stufe. */
    private static final Codec<List<Double>> PARAM_CODEC = Codec.either(Codec.DOUBLE, Codec.DOUBLE.listOf()).xmap(
            e -> e.map(List::of, l -> l),
            l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l));

    public record Definition(Tab tab, UpgradeIcon icon, int x, int y, int maxLevel, CostCurve curve, List<Double> values,
            List<Requirement> requires, Map<String, List<Double>> params, List<List<Integer>> levelPositions,
            List<UpgradeIcon> levelIcons) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(i -> i.group(
                Tab.CODEC.fieldOf("tab").forGetter(Definition::tab),
                UpgradeIcon.CODEC.fieldOf("icon").forGetter(Definition::icon),
                Codec.intRange(0, 100).fieldOf("x").forGetter(Definition::x),
                Codec.intRange(0, 100).fieldOf("y").forGetter(Definition::y),
                Codec.intRange(1, 1000).fieldOf("max_level").forGetter(Definition::maxLevel),
                CostCurve.MAP_CODEC.forGetter(Definition::curve),
                Codec.DOUBLE.listOf().optionalFieldOf("values", List.of()).forGetter(Definition::values),
                Requirement.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Definition::requires),
                Codec.unboundedMap(Codec.STRING, PARAM_CODEC).optionalFieldOf("params", Map.of()).forGetter(Definition::params),
                TreeLayout.LEVEL_POSITIONS.optionalFieldOf("level_positions", List.of()).forGetter(Definition::levelPositions),
                UpgradeIcon.CODEC.listOf().optionalFieldOf("level_icons", List.of()).forGetter(Definition::levelIcons)
        ).apply(i, Definition::new));

        /** Jede Stufe ein eigenes Feld im Baum, siehe {@link TreeLayout}. */
        public boolean split() {
            return TreeLayout.split(maxLevel, levelPositions);
        }

        /** Rasterplatz {x, y} des Feldes für {@code level}. */
        public int[] position(int level) {
            return TreeLayout.position(x, y, maxLevel, levelPositions, level);
        }

        /** Symbol des Feldes für {@code level}; {@code level_icons} gelten ab Stufe 2. */
        public UpgradeIcon icon(int level) {
            return UpgradeIcon.forLevel(icon, levelIcons, level);
        }

        /** Preis für das Erreichen von {@code level} (1-basiert). */
        public long cost(int level) {
            return curve.cost(level);
        }

        /** Wirkungswert auf {@code level} (1-basiert); fehlen Einträge, gilt der letzte. 0 ohne Stufe oder Werte. */
        public double value(int level) {
            if (level <= 0 || values.isEmpty()) {
                return 0.0;
            }
            return values.get(Math.min(level, values.size()) - 1);
        }

        /** Benannter Wirkungsparameter auf Stufe 1, siehe {@link #param(String, int, double)}. */
        public double param(String name, double fallback) {
            return param(name, 1, fallback);
        }

        /**
         * Benannter Wirkungsparameter (Abklingzeit, Dauer, Effektstufe …) aus {@code params} für {@code level}
         * (1-basiert); fehlen Einträge, gilt der letzte. Ohne Eintrag {@code fallback}.
         */
        public double param(String name, int level, double fallback) {
            List<Double> list = params.get(name);
            if (list == null || list.isEmpty()) {
                return fallback;
            }
            return list.get(Math.clamp(level, 1, list.size()) - 1);
        }
    }

    private static Map<PlayerUpgrade, Definition> definitions = Map.of();

    public PlayerUpgradeDefinitions() {
        super(Definition.CODEC, FileToIdConverter.json("dtp/player_upgrade"));
    }

    @Override
    protected void apply(Map<Identifier, Definition> loaded, ResourceManager manager, ProfilerFiller profiler) {
        Map<PlayerUpgrade, Definition> result = new EnumMap<>(PlayerUpgrade.class);
        loaded.forEach((id, def) -> {
            PlayerUpgrade upgrade = PlayerUpgrade.byId(id.getPath());
            if (upgrade == null) {
                Dtp.LOGGER.warn("Unknown player upgrade '{}' in datapack, ignored", id);
                return;
            }
            for (Requirement r : def.requires()) {
                if (PlayerUpgrade.byId(r.upgrade()) == null) {
                    Dtp.LOGGER.warn("Player upgrade '{}' requires unknown upgrade '{}'", id, r.upgrade());
                }
            }
            if (!def.levelPositions().isEmpty() && !def.split()) {
                Dtp.LOGGER.warn("Player upgrade '{}': level_positions needs {} entries, got {} — shown as one node", id,
                        def.maxLevel() - 1, def.levelPositions().size());
            }
            result.put(upgrade, def);
        });
        definitions = result;
        Dtp.LOGGER.info("Loaded {} player upgrade definitions", result.size());
    }

    public static Definition get(PlayerUpgrade upgrade) {
        return definitions.get(upgrade);
    }
}
