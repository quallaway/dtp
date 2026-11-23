package dev.dtp.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import dev.dtp.currency.Money;
import dev.dtp.menu.MenuSync;
import dev.dtp.menu.UpgradeTree;

/**
 * Upgrade-Baum im Stil der Advancements für Werkstatt und Beacon: Felder im Raster, Linien zu den Voraussetzungen,
 * darunter der Detailbereich zum gewählten Feld. Aufgeteilte Upgrades zeigen ein Feld je Stufe mit eigenem Preis und
 * eigener Beschreibung; jede Stufe hängt an der vorigen. Daten aus {@link UpgradeTree} im Menü.
 *
 * <p>Optional je Feld: {@code module} (blaue Ecke, Werkstatt), {@code skill} (orange Ecke statt blau: aktiver Effekt,
 * nur einer ausrüstbar) und {@code block} — ein Grund, warum die nächste Stufe
 * gerade nicht kaufbar ist (z. B. fehlende Contract-Abschlüsse am Beacon).
 */
final class UpgradeTreeView {
    static final int CELL = 28;
    static final int NODE = 22;

    private static final int CANVAS_BG = 0xFF161616;
    private static final int LOCKED = 0xFF505050;
    private static final int LINE_MET = Gui.GOLD;
    private static final int LINE_OPEN = 0xFF606060;
    /** Ecke an Modul-Feldern: wirkt nur ausgerüstet. */
    static final int MODULE_MARK = 0xFF60B0FF;
    /** Ecke an Skill-Feldern: aktiver Effekt, nur einer gleichzeitig ausrüstbar. */
    static final int SKILL_MARK = 0xFFFF9030;

    private final String namePrefix;
    final int canvasX;
    final int canvasY;
    final int canvasW;
    final int canvasH;
    final int detailTop;

    /** @param namePrefix Präfix der Übersetzungsschlüssel, z. B. {@code player_upgrade.dtp.} oder {@code upgrade.dtp.} */
    UpgradeTreeView(String namePrefix, int canvasX, int canvasY, int canvasW, int canvasH, int detailTop) {
        this.namePrefix = namePrefix;
        this.canvasX = canvasX;
        this.canvasY = canvasY;
        this.canvasW = canvasW;
        this.canvasH = canvasH;
        this.detailTop = detailTop;
    }

    record Node(CompoundTag data, int x, int y) {
        int ordinal() {
            return data.getIntOr("ordinal", -1);
        }

        /** Stufe dieses Feldes bei aufgeteilten Upgrades, 0 = ganzes Upgrade. */
        int nodeLevel() {
            return data.getIntOr("node_level", 0);
        }

        int key() {
            return UpgradeTreeView.key(ordinal(), nodeLevel());
        }

        boolean maxed() {
            return data.getLongOr("cost", -1) < 0;
        }

        /** Gekauft: diese Stufe bzw. beim ganzen Upgrade mindestens eine. */
        boolean owned() {
            int level = data.getIntOr("level", 0);
            return nodeLevel() > 0 ? level >= nodeLevel() : level > 0;
        }

        boolean requirementsMet() {
            ListTag requires = data.getListOrEmpty("requires");
            for (int i = 0; i < requires.size(); i++) {
                if (!requires.getCompoundOrEmpty(i).getBooleanOr("met", false)) {
                    return false;
                }
            }
            return true;
        }

        Component block() {
            return MenuSync.getComponent(data, "block");
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx < x + NODE && my >= y && my < y + NODE;
        }
    }

    static int key(int ordinal, int nodeLevel) {
        return ordinal * UpgradeTree.LEVELS_PER_UPGRADE + nodeLevel;
    }

    /** Felder mit Panel-Koordinaten. Der Baum wird in der Fläche zentriert, solange er hineinpasst. */
    List<Node> nodes(CompoundTag d) {
        ListTag list = d.getListOrEmpty("nodes");
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            minX = Math.min(minX, e.getIntOr("x", 0));
            minY = Math.min(minY, e.getIntOr("y", 0));
            maxX = Math.max(maxX, e.getIntOr("x", 0));
            maxY = Math.max(maxY, e.getIntOr("y", 0));
        }
        int width = (maxX - minX) * CELL + NODE;
        int height = (maxY - minY) * CELL + NODE;
        int originX = canvasX + Math.max(4, (canvasW - width) / 2) - minX * CELL;
        int originY = canvasY + Math.max(4, (canvasH - height) / 2) - minY * CELL;
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            nodes.add(new Node(e, originX + e.getIntOr("x", 0) * CELL, originY + e.getIntOr("y", 0) * CELL));
        }
        return nodes;
    }

    Node selected(CompoundTag d, List<Node> nodes) {
        int key = key(d.getIntOr("selected", -1), d.getIntOr("selected_level", 0));
        for (Node n : nodes) {
            if (n.key() == key) {
                return n;
            }
        }
        return null;
    }

    /** Knopf-ID für einen Klick auf ein Feld oder −1. */
    int clicked(CompoundTag d, int mx, int my) {
        for (Node n : nodes(d)) {
            if (n.contains(mx, my)) {
                return n.nodeLevel() > 0 ? UpgradeTree.SELECT_LEVEL + key(n.ordinal(), n.nodeLevel()) : UpgradeTree.SELECT + n.ordinal();
            }
        }
        return -1;
    }

    /** Kaufen-Knopf aktiv: im Gebiet, nicht gekauft, Voraussetzungen erfüllt, kein Sperrgrund. */
    boolean canBuy(Node sel, boolean usable) {
        return sel != null && usable && !sel.maxed() && sel.requirementsMet() && sel.block() == null;
    }

    /** Teil des Layout-Schlüssels, der die Knöpfe neu aufbauen lässt. */
    String layoutKey(CompoundTag d) {
        Node sel = selected(d, nodes(d));
        return d.getIntOr("selected", -1) + ":" + d.getIntOr("selected_level", 0)
                + (sel == null ? "" : ":" + sel.maxed() + ":" + sel.requirementsMet() + ":" + (sel.block() != null));
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    void draw(GuiGraphicsExtractor g, Font font, CompoundTag d, long balance, boolean usable, int mouseX, int mouseY) {
        g.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, CANVAS_BG);
        g.outline(canvasX, canvasY, canvasW, canvasH, Gui.BORDER);

        List<Node> nodes = nodes(d);
        Map<Integer, Node> byKey = new HashMap<>();
        nodes.forEach(n -> byKey.put(n.key(), n));
        for (Node n : nodes) {
            ListTag requires = n.data().getListOrEmpty("requires");
            for (int i = 0; i < requires.size(); i++) {
                CompoundTag r = requires.getCompoundOrEmpty(i);
                Node from = byKey.get(key(r.getIntOr("ordinal", -1), r.getIntOr("node_level", 0)));
                if (from != null) {
                    connect(g, from, n, r.getBooleanOr("met", false) ? LINE_MET : LINE_OPEN);
                }
            }
        }

        Node sel = selected(d, nodes);
        Node hovered = null;
        for (Node n : nodes) {
            drawNode(g, n, balance, n == sel);
            if (n.contains(mouseX, mouseY)) {
                hovered = n;
            }
        }
        if (nodes.isEmpty()) {
            g.centeredText(font, Component.translatable("dtp.workshop.empty"), canvasX + canvasW / 2, canvasY + canvasH / 2 - 4, Gui.MUTED);
        }

        detail(g, font, sel, balance, usable);
        if (hovered != null) {
            g.setComponentTooltipForNextFrame(font, tooltip(hovered, balance), mouseX, mouseY);
        }
    }

    /** Rechtwinklige Verbindung: waagerecht aus der Voraussetzung heraus, dann senkrecht zum Ziel. */
    private static void connect(GuiGraphicsExtractor g, Node from, Node to, int color) {
        int fx = from.x() + NODE / 2;
        int fy = from.y() + NODE / 2;
        int tx = to.x() + NODE / 2;
        int ty = to.y() + NODE / 2;
        g.fill(Math.min(fx, tx), fy - 1, Math.max(fx, tx) + 1, fy + 1, color);
        g.fill(tx - 1, Math.min(fy, ty), tx + 1, Math.max(fy, ty) + 1, color);
    }

    private static void drawNode(GuiGraphicsExtractor g, Node n, long balance, boolean selected) {
        long cost = n.data().getLongOr("cost", -1);
        int frame = n.maxed() ? Gui.GOLD
                : !n.requirementsMet() || n.block() != null ? LOCKED
                : balance >= cost ? Gui.GOOD : Gui.MUTED;
        if (selected) {
            g.fill(n.x() - 2, n.y() - 2, n.x() + NODE + 2, n.y() + NODE + 2, Gui.TEXT);
        }
        g.fill(n.x(), n.y(), n.x() + NODE, n.y() + NODE, frame);
        g.fill(n.x() + 1, n.y() + 1, n.x() + NODE - 1, n.y() + NODE - 1, n.owned() ? 0xFF3A3220 : 0xFF202020);
        if (n.data().getBooleanOr("module", false)) {
            g.fill(n.x() + NODE - 5, n.y() + 1, n.x() + NODE - 1, n.y() + 5,
                    n.data().getBooleanOr("skill", false) ? SKILL_MARK : MODULE_MARK);
        }
        Gui.icon(g, n.data(), n.x(), n.y(), NODE, 1.0f);
    }

    private Component name(String id) {
        return Component.translatable(namePrefix + id);
    }

    /** Name mit Stufe: „Rausch II“ für ein Stufen-Feld, sonst „Name · Stufe x/y“. */
    private Component title(CompoundTag e) {
        String id = e.getStringOr("id", "");
        int nodeLevel = e.getIntOr("node_level", 0);
        if (nodeLevel > 0) {
            return Component.translatable("dtp.workshop.node_title", name(id), Component.translatable("enchantment.level." + nodeLevel));
        }
        return Component.translatable("dtp.workshop.title", name(id), e.getIntOr("level", 0), e.getIntOr("max", 0));
    }

    /** Beschreibung nur dieser Stufe ({@code .desc.<Stufe>}), sonst die allgemeine. */
    private Component description(CompoundTag e) {
        String base = namePrefix + e.getStringOr("id", "") + ".desc";
        int nodeLevel = e.getIntOr("node_level", 0);
        if (nodeLevel > 0 && Language.getInstance().has(base + "." + nodeLevel)) {
            return Component.translatable(base + "." + nodeLevel);
        }
        return Component.translatable(base);
    }

    private List<Component> tooltip(Node n, long balance) {
        CompoundTag e = n.data();
        String id = e.getStringOr("id", "");
        boolean split = n.nodeLevel() > 0;
        List<Component> lines = new ArrayList<>();
        lines.add(split ? title(e).copy().withColor(Gui.GOLD) : name(id).copy().withColor(Gui.GOLD));
        if (!split) {
            lines.add(Component.translatable("dtp.workshop.level", e.getIntOr("level", 0), e.getIntOr("max", 0)).withColor(Gui.MUTED));
        }
        if (e.getBooleanOr("skill", false)) {
            lines.add(Component.translatable("dtp.workshop.skill_tag").withColor(SKILL_MARK));
        } else if (e.getBooleanOr("module", false)) {
            lines.add(Component.translatable("dtp.workshop.module_tag").withColor(MODULE_MARK));
        }
        lines.add(description(e));
        long cost = e.getLongOr("cost", -1);
        if (cost < 0 && split) {
            lines.add(Component.translatable("dtp.workshop.owned").withColor(Gui.GOLD));
        } else if (cost >= 0) {
            lines.add(balance >= cost ? Money.component(cost).copy().withColor(Gui.GOOD)
                    : Component.translatable("dtp.upgrade.missing", Money.format(cost), Money.format(cost - balance)).withColor(Gui.GOLD));
        }
        lines.addAll(missing(e));
        if (n.block() != null) {
            lines.add(n.block().copy().withColor(Gui.WARN));
        }
        return lines;
    }

    /** Fehlende Voraussetzungen, je eine Zeile — so ist benannt, was zuerst gekauft werden muss. */
    private List<Component> missing(CompoundTag e) {
        List<Component> lines = new ArrayList<>();
        ListTag requires = e.getListOrEmpty("requires");
        for (int i = 0; i < requires.size(); i++) {
            CompoundTag r = requires.getCompoundOrEmpty(i);
            if (!r.getBooleanOr("met", false)) {
                lines.add(Component.translatable("dtp.workshop.requires", name(r.getStringOr("id", "")), r.getIntOr("level", 1))
                        .withColor(Gui.WARN));
            }
        }
        return lines;
    }

    private void detail(GuiGraphicsExtractor g, Font font, Node sel, long balance, boolean usable) {
        if (sel == null) {
            g.text(font, Component.translatable("dtp.workshop.hint"), canvasX, detailTop + 5, Gui.MUTED, false);
            return;
        }
        CompoundTag e = sel.data();
        g.text(font, Gui.fit(font, title(e), 200), canvasX, detailTop, Gui.TEXT, false);

        long cost = e.getLongOr("cost", -1);
        List<Component> missing = missing(e);
        Component status;
        int color;
        if (!usable) {
            status = Component.translatable("dtp.workshop.outside");
            color = Gui.WARN;
        } else if (cost < 0) {
            status = Component.translatable(sel.nodeLevel() > 0 ? "dtp.workshop.owned" : "dtp.upgrade.maxed");
            color = Gui.GOLD;
        } else if (!missing.isEmpty()) {
            status = missing.getFirst();
            color = Gui.WARN;
        } else if (sel.block() != null) {
            status = sel.block();
            color = Gui.WARN;
        } else if (balance >= cost) {
            status = Money.component(cost);
            color = Gui.GOOD;
        } else {
            status = Component.translatable("dtp.upgrade.missing", Money.format(cost), Money.format(cost - balance));
            color = Gui.GOLD;
        }
        g.text(font, Gui.fit(font, status, 200), canvasX, detailTop + 11, color, false);
        g.textWithWordWrap(font, description(e), canvasX, detailTop + 24, canvasW, Gui.MUTED, false);
    }
}
