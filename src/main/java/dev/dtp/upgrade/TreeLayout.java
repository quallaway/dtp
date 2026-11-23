package dev.dtp.upgrade;

import java.util.List;

import com.mojang.serialization.Codec;

/**
 * Lage eines Upgrades im Upgrade-Baum (Werkstatt und Beacon): Stufe 1 auf {@code x}/{@code y}, optional ein eigenes
 * Feld je weiterer Stufe über {@code level_positions} (Plätze der Stufen 2 bis max). Raster 9 × 4.
 */
public final class TreeLayout {
    public static final int COLUMNS = 9;
    public static final int ROWS = 4;

    /** {@code level_positions}: Liste von [x, y]-Paaren. */
    public static final Codec<List<List<Integer>>> LEVEL_POSITIONS = Codec.intRange(0, 100).listOf(2, 2).listOf();

    private TreeLayout() {
    }

    /** Jede Stufe ein eigenes Feld — nur, wenn es genau {@code maxLevel - 1} weitere Plätze gibt. */
    public static boolean split(int maxLevel, List<List<Integer>> levelPositions) {
        return maxLevel > 1 && levelPositions.size() == maxLevel - 1;
    }

    /** Rasterplatz {x, y} des Feldes für {@code level}; ohne Aufteilung immer {@code x}/{@code y}. */
    public static int[] position(int x, int y, int maxLevel, List<List<Integer>> levelPositions, int level) {
        if (!split(maxLevel, levelPositions) || level <= 1) {
            return new int[] {x, y};
        }
        List<Integer> p = levelPositions.get(Math.min(level, maxLevel) - 2);
        return new int[] {p.get(0), p.get(1)};
    }
}
