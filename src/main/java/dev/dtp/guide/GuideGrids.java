package dev.dtp.guide;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Crafting-Raster des DTP-Handbuchs. Die Markdown-Dateien ({@code assets/dtp/guide/<sprache>.md}) wählen sie
 * per Name ({@code grid=beacon}); die Daten stehen hier, weil der GameTest {@code guide_grids} jedes Raster gegen
 * das echte Rezept prüft. Der Client kennt nicht alle Rezepte und kann sie deshalb nicht selbst nachschlagen.
 */
public final class GuideGrids {
    /**
     * Ein 3×3-Raster: neun Item-IDs zeilenweise, {@code ""} für ein leeres Feld. Bei Tags steht ein Vertreter
     * (Eichenbretter für {@code #minecraft:planks}). {@code shapeless} zeigt das Formlos-Symbol.
     */
    public record Grid(List<String> slots, String result, int count, boolean shapeless) {
        public Grid {
            if (slots.size() != 9) {
                throw new IllegalArgumentException("A guide grid needs 9 slots, got " + slots.size());
            }
            slots = List.copyOf(slots);
        }
    }

    private static final String NONE = "";
    private static final String PLANKS = "minecraft:oak_planks";

    public static final Grid BEACON = new Grid(List.of(
            "minecraft:glass", "minecraft:glass", "minecraft:glass",
            "minecraft:copper_ingot", "minecraft:copper_block", "minecraft:copper_ingot",
            "minecraft:stone_bricks", "minecraft:stone_bricks", "minecraft:stone_bricks"), "dtp:beacon", 1, false);

    public static final Grid MARKET_STAND = new Grid(List.of(
            "minecraft:white_wool", "minecraft:white_wool", "minecraft:white_wool",
            PLANKS, "minecraft:crafting_table", PLANKS,
            PLANKS, NONE, PLANKS), "dtp:market_stand", 1, false);

    public static final Grid TRADE_CHEST = new Grid(List.of(
            PLANKS, "minecraft:iron_ingot", PLANKS,
            "minecraft:iron_ingot", "minecraft:chest", "minecraft:iron_ingot",
            PLANKS, "minecraft:iron_ingot", PLANKS), "dtp:trade_chest", 1, false);

    public static final Grid DEPOSIT_CHEST = new Grid(List.of(
            PLANKS, "minecraft:copper_ingot", PLANKS,
            "minecraft:copper_ingot", "minecraft:chest", "minecraft:copper_ingot",
            PLANKS, "minecraft:copper_ingot", PLANKS), "dtp:deposit_chest", 1, false);

    public static final Grid BULLETIN_BOARD = new Grid(List.of(
            PLANKS, "minecraft:paper", PLANKS,
            "minecraft:paper", "dtp:copper_taler", "minecraft:paper",
            PLANKS, PLANKS, PLANKS), "dtp:bulletin_board", 1, false);

    public static final Grid STATISTICS_BOARD = new Grid(List.of(
            "minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:iron_nugget",
            "minecraft:paper", "minecraft:paper", "minecraft:paper",
            PLANKS, "dtp:copper_taler", PLANKS), "dtp:statistics_board", 1, false);

    public static final Grid UPGRADE_TABLE = new Grid(List.of(
            NONE, "dtp:copper_sack", NONE,
            "minecraft:book", "minecraft:smithing_table", "minecraft:book",
            PLANKS, PLANKS, PLANKS), "dtp:upgrade_table", 1, false);

    public static final Grid REPAIR_STATION = new Grid(List.of(
            NONE, "dtp:copper_pouch", NONE,
            "minecraft:iron_ingot", "minecraft:grindstone", "minecraft:iron_ingot",
            PLANKS, PLANKS, PLANKS), "dtp:repair_station", 1, false);

    public static final Grid RELAY_BEACON = new Grid(List.of(
            NONE, "dtp:gold_taler", NONE,
            NONE, "minecraft:diamond_block", NONE,
            "minecraft:iron_block", "minecraft:iron_block", "minecraft:iron_block"), "dtp:relay_beacon", 1, false);

    public static final Grid CHUNK_LOADER = new Grid(List.of(
            "minecraft:glass", "minecraft:glass", "minecraft:glass",
            "dtp:silver_pouch", "minecraft:ender_pearl", "dtp:silver_pouch",
            "minecraft:soul_sand", "minecraft:soul_sand", "minecraft:soul_sand"), "dtp:chunk_loader", 1, false);

    public static final Grid MARKET_SCALE = new Grid(List.of(
            "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot",
            NONE, "minecraft:comparator", NONE,
            NONE, "minecraft:smooth_stone_slab", NONE), "dtp:market_scale", 1, false);

    public static final Grid BLACKMARKET = new Grid(List.of(
            "minecraft:black_wool", "minecraft:black_wool", "minecraft:black_wool",
            "dtp:market_stand", "dtp:silver_sack", "minecraft:enchanting_table",
            "minecraft:crying_obsidian", "minecraft:crying_obsidian", "minecraft:crying_obsidian"), "dtp:blackmarket", 1, false);

    public static final Grid SLOT_MACHINE = new Grid(List.of(
            "minecraft:iron_ingot", "minecraft:glass_pane", "minecraft:iron_ingot",
            "minecraft:gold_ingot", "dtp:copper_sack", "minecraft:lever",
            "minecraft:iron_ingot", "minecraft:redstone", "minecraft:iron_ingot"), "dtp:slot_machine", 1, false);

    public static final Grid RECYCLER = new Grid(List.of(
            "minecraft:iron_bars", "minecraft:hopper", "minecraft:iron_bars",
            "minecraft:iron_ingot", "minecraft:grindstone", "minecraft:iron_ingot",
            "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"), "dtp:recycler", 1, false);

    public static final Grid TALER = new Grid(List.of(
            "dtp:copper_taler", "dtp:copper_taler", "dtp:copper_taler",
            "dtp:copper_taler", "dtp:copper_taler", "dtp:copper_taler",
            "dtp:copper_taler", "dtp:copper_taler", "dtp:copper_taler"), "dtp:copper_pouch", 1, false);

    public static final Grid GUIDE = new Grid(List.of(
            "minecraft:book", "minecraft:copper_nugget", NONE,
            NONE, NONE, NONE,
            NONE, NONE, NONE), "dtp:guide", 1, true);

    private static final Map<String, Grid> BY_NAME = new LinkedHashMap<>();

    static {
        BY_NAME.put("beacon", BEACON);
        BY_NAME.put("market_stand", MARKET_STAND);
        BY_NAME.put("trade_chest", TRADE_CHEST);
        BY_NAME.put("deposit_chest", DEPOSIT_CHEST);
        BY_NAME.put("bulletin_board", BULLETIN_BOARD);
        BY_NAME.put("statistics_board", STATISTICS_BOARD);
        BY_NAME.put("upgrade_table", UPGRADE_TABLE);
        BY_NAME.put("repair_station", REPAIR_STATION);
        BY_NAME.put("relay_beacon", RELAY_BEACON);
        BY_NAME.put("chunk_loader", CHUNK_LOADER);
        BY_NAME.put("market_scale", MARKET_SCALE);
        BY_NAME.put("blackmarket", BLACKMARKET);
        BY_NAME.put("slot_machine", SLOT_MACHINE);
        BY_NAME.put("recycler", RECYCLER);
        BY_NAME.put("taler", TALER);
        BY_NAME.put("guide", GUIDE);
    }

    private GuideGrids() {
    }

    /** Raster zum Namen aus der Markdown-Datei, oder {@code null}. */
    public static Grid byName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<String> names() {
        return BY_NAME.keySet();
    }

    public static List<Grid> all() {
        return List.copyOf(BY_NAME.values());
    }
}
