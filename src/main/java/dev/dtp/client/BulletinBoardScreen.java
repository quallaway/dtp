package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import dev.dtp.contract.Contract;
import dev.dtp.currency.Money;
import dev.dtp.menu.BulletinBoardMenu;
import dev.dtp.menu.MenuSync;

/** Aushang und eigene Contracts. Geliefert wird aus den Trade-Chests in Reichweite des Boards. */
public class BulletinBoardScreen extends PanelScreen<BulletinBoardMenu> {
    private static final int WIDTH = 320;
    /** Contracts stehen als Kacheln in zwei Spalten — so passen voller Aushang und alle Slots ohne Scrollen. */
    private static final int COLS = 2;
    private static final int GAP = 4;
    private static final int TILE_W = (WIDTH - 16 - GAP) / COLS;
    private static final int TILE_H = 30;
    private static final int ROW = TILE_H + 2;
    private static final int BUTTON = 18;
    private static final int OFFERS_TOP = 30;
    /** Alles außer den Kachelreihen: Kopf, Überschrift „Deine Contracts“ und Fußzeile für Meldungen. */
    private static final int CHROME = OFFERS_TOP + 13 + 16;
    /** So viele Reihen bleiben je Bereich mindestens sichtbar, wenn der Bildschirm zu niedrig ist. */
    private static final int MIN_ROWS = 1;

    /** Scroll-Positionen in Kachelreihen. */
    private int offerScroll;
    private int acceptedScroll;

    public BulletinBoardScreen(BulletinBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, 200);
    }

    private static int rowsFor(int tiles) {
        return (tiles + COLS - 1) / COLS;
    }

    private int maxOfferRows() {
        return rowsFor(Math.max(1, data().getIntOr("board_max", 0)));
    }

    private int maxAcceptedRows() {
        return rowsFor(Math.max(1, data().getIntOr("slots_max", 0)));
    }

    /** Reihen, die das Fenster unterhalb des Bildschirmrands nicht mehr zeigen kann. */
    private int overflow() {
        int budget = Math.max(0, (height - 8 - CHROME) / ROW);
        return Math.max(0, maxOfferRows() + maxAcceptedRows() - budget);
    }

    /** Sichtbare Aushangreihen: so viele, wie der voll ausgebaute Aushang hat — passt das nicht, wird hier zuerst gekürzt. */
    private int offerRows() {
        int max = maxOfferRows();
        return Math.max(Math.min(MIN_ROWS, max), max - overflow());
    }

    /** Sichtbare eigene Reihen: alle möglichen Slots, gekürzt nur, wenn der Aushang schon auf das Minimum geschrumpft ist. */
    private int acceptedRows() {
        int max = maxAcceptedRows();
        int rest = Math.max(0, overflow() - (maxOfferRows() - offerRows()));
        return Math.max(Math.min(MIN_ROWS, max), max - rest);
    }

    /** Eigene Contracts stehen bündig am unteren Rand, der Aushang oben. */
    private int acceptedTop() {
        return panelHeight() - 16 - acceptedRows() * ROW;
    }

    /** Feste Höhe: hängt nur von den möglichen Contracts ab, nie von den gerade vorhandenen. */
    @Override
    protected int panelHeight() {
        return CHROME + (offerRows() + acceptedRows()) * ROW;
    }

    private static int tileX(int slot) {
        return 8 + slot % COLS * (TILE_W + GAP);
    }

    private static int tileY(int top, int slot) {
        return top + slot / COLS * ROW;
    }

    /** Hält die Scroll-Positionen im gültigen Bereich, auch nachdem die Listen kürzer geworden sind. */
    private void clampScroll() {
        CompoundTag d = data();
        offerScroll = Math.clamp(offerScroll, 0, Math.max(0, rowsFor(d.getListOrEmpty("offers").size()) - offerRows()));
        acceptedScroll = Math.clamp(acceptedScroll, 0, Math.max(0, rowsFor(d.getListOrEmpty("accepted").size()) - acceptedRows()));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int ly = (int) y - topPos;
            int step = scrollY > 0 ? -1 : 1;
            int offersBottom = OFFERS_TOP + offerRows() * ROW;
            int accTop = acceptedTop();
            boolean changed = false;
            if (ly >= OFFERS_TOP && ly < offersBottom) {
                int before = offerScroll;
                offerScroll += step;
                clampScroll();
                changed = offerScroll != before;
            } else if (ly >= accTop && ly < accTop + acceptedRows() * ROW) {
                int before = acceptedScroll;
                acceptedScroll += step;
                clampScroll();
                changed = acceptedScroll != before;
            }
            if (changed) {
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void init() {
        super.init();
        clampScroll();
        CompoundTag d = data();
        ListTag offers = d.getListOrEmpty("offers");
        ListTag accepted = d.getListOrEmpty("accepted");
        boolean slotFree = accepted.size() < d.getIntOr("slots", 0);
        // Außerhalb des Territoriums ist das Board nur lesbar; Abbrechen bleibt möglich.
        boolean usable = !d.getBooleanOr("outside", false);
        // Die Knopf-ID trägt den echten Listenindex, nicht die sichtbare Kachel.
        int first = offerScroll * COLS;
        int offerEnd = Math.min(offers.size(), first + offerRows() * COLS);
        for (int i = first; i < offerEnd; i++) {
            CompoundTag e = offers.getCompoundOrEmpty(i);
            boolean locked = e.getBooleanOr("locked", false);
            int x = tileX(i - first) + TILE_W - BUTTON - 2;
            Component hint = locked ? Component.translatable("dtp.contract.tier_locked", e.getIntOr("tier", 1))
                    : Component.translatable("dtp.contract.accept");
            button(Component.literal("✔"), x, tileY(OFFERS_TOP, i - first) + 1, BUTTON, BulletinBoardMenu.ACCEPT + i,
                    usable && slotFree && !locked).setTooltip(Tooltip.create(hint));
        }
        int top = acceptedTop();
        first = acceptedScroll * COLS;
        int acceptedEnd = Math.min(accepted.size(), first + acceptedRows() * COLS);
        for (int i = first; i < acceptedEnd; i++) {
            int x = tileX(i - first) + TILE_W - 2 * BUTTON - 3;
            int y = tileY(top, i - first) + 1;
            boolean inStock = accepted.getCompoundOrEmpty(i).getIntOr("in_stock", 0) > 0;
            button(Component.literal("↓"), x, y, BUTTON, BulletinBoardMenu.DELIVER + i, usable && inStock)
                    .setTooltip(Tooltip.create(Component.translatable("dtp.contract.deliver")));
            button(Component.literal("✕"), x + BUTTON + 1, y, BUTTON, BulletinBoardMenu.CANCEL + i, true)
                    .setTooltip(Tooltip.create(Component.translatable("dtp.contract.cancel_hint")));
        }
    }

    @Override
    protected String layoutKey() {
        clampScroll();
        CompoundTag d = data();
        StringBuilder key = new StringBuilder().append(d.getIntOr("slots", 0)).append('|').append(d.getBooleanOr("outside", false))
                .append('|').append(maxOfferRows()).append('|').append(maxAcceptedRows())
                .append('|').append(offerScroll).append('|').append(acceptedScroll);
        ListTag offers = d.getListOrEmpty("offers");
        for (int i = 0; i < offers.size(); i++) {
            key.append('|').append(offers.getCompoundOrEmpty(i).getBooleanOr("locked", false));
        }
        ListTag accepted = d.getListOrEmpty("accepted");
        for (int i = 0; i < accepted.size(); i++) {
            key.append("|a").append(accepted.getCompoundOrEmpty(i).getIntOr("in_stock", 0) > 0);
        }
        return key.toString();
    }

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        ListTag offers = d.getListOrEmpty("offers");
        ListTag accepted = d.getListOrEmpty("accepted");
        int tier = d.getIntOr("tier", 0);
        int slots = d.getIntOr("slots", 0);

        g.text(font, Component.translatable("dtp.contract.board_rotation", String.format("%.1f", d.getDoubleOr("rotation_in", 0))),
                8, OFFERS_TOP - 11, Gui.TEXT, false);
        if (offers.isEmpty()) {
            Component empty = Component.translatable(tier < 1 ? "dtp.contract.status_locked" : "dtp.contract.board_empty");
            g.text(font, Gui.fit(font, empty, WIDTH - 16), 8, OFFERS_TOP + 5, tier < 1 ? Gui.WARN : Gui.MUTED, false);
        }
        int first = offerScroll * COLS;
        int offerEnd = Math.min(offers.size(), first + offerRows() * COLS);
        for (int i = first; i < offerEnd; i++) {
            tile(g, offers.getCompoundOrEmpty(i), tileX(i - first), tileY(OFFERS_TOP, i - first), mouseX, mouseY, false);
        }
        scrollbar(g, OFFERS_TOP, offerRows(), rowsFor(offers.size()), offerScroll);

        int top = acceptedTop();
        Component heading = Component.translatable("dtp.contract.mine", accepted.size(), slots);
        g.text(font, heading, 8, top - 11, Gui.TEXT, false);
        if (mouseX >= 8 && mouseX < 8 + font.width(heading) && mouseY >= top - 11 && mouseY < top - 2) {
            g.setTooltipForNextFrame(font, Component.translatable("dtp.contract.mine_hint"), mouseX, mouseY);
        }
        if (slots < 1) {
            g.text(font, Component.translatable("dtp.contract.mine_empty"), 8, top + 5, Gui.MUTED, false);
        }
        first = acceptedScroll * COLS;
        int visibleEnd = first + acceptedRows() * COLS;
        for (int i = first; i < visibleEnd; i++) {
            int x = tileX(i - first);
            int y = tileY(top, i - first);
            if (i < accepted.size()) {
                tile(g, accepted.getCompoundOrEmpty(i), x, y, mouseX, mouseY, true);
            } else if (i < slots) {
                // Freier Slot: leere Kachel, damit man sieht, dass noch ein Contract angenommen werden kann.
                g.fill(x, y, x + TILE_W, y + TILE_H, 0x14FFFFFF);
            }
        }
        scrollbar(g, top, acceptedRows(), rowsFor(accepted.size()), acceptedScroll);

        // Truhen-Probleme nur als Warnung in der Fußzeile, solange dort keine Meldung steht.
        if (MenuSync.getComponent(d, "message") == null) {
            String warning = d.getBooleanOr("outside", false) ? "dtp.contract.warn_outside"
                    : d.getIntOr("trade_chests", 0) < 1 ? "dtp.contract.warn_no_trade_chest"
                    : d.getIntOr("deposit_chests", 0) < 1 ? "dtp.contract.warn_no_deposit" : null;
            if (warning != null) {
                g.text(font, Component.translatable(warning), 8, panelHeight() - 14, Gui.WARN, false);
            }
        }
    }

    /** Schmale Leiste rechts am Bereich — nur, wenn mehr Reihen da sind als sichtbar. */
    private void scrollbar(GuiGraphicsExtractor g, int y, int rows, int size, int scroll) {
        if (size <= rows) {
            return;
        }
        int h = rows * ROW - 2;
        int thumb = Math.max(6, h * rows / size);
        int thumbY = y + (h - thumb) * scroll / (size - rows);
        g.fill(WIDTH - 6, y, WIDTH - 3, y + h, 0xFF202020);
        g.fill(WIDTH - 6, thumbY, WIDTH - 3, thumbY + thumb, Gui.MUTED);
    }

    /**
     * Eine Contract-Kachel: Symbol, Menge, Belohnung und als dritte Zeile Wert und nächste Absenkung.
     * Alles Weitere (Name, Stufe, Bedingungen, Lager) steht im Tooltip des Symbols.
     */
    private void tile(GuiGraphicsExtractor g, CompoundTag e, int x, int y, int mouseX, int mouseY, boolean mine) {
        boolean locked = !mine && e.getBooleanOr("locked", false);
        g.fill(x, y, x + TILE_W, y + TILE_H, locked ? 0x30FF6060 : 0x24FFFFFF);
        // Rahmen in der Tier-Farbe: Kupfer, Eisen, Gold, Diamant.
        int tier = e.getIntOr("tier", 1);
        int frame = Gui.tierColor(tier);
        g.fill(x, y, x + TILE_W, y + 1, frame);
        g.fill(x, y + TILE_H - 1, x + TILE_W, y + TILE_H, frame);
        g.fill(x, y + 1, x + 1, y + TILE_H - 1, frame);
        g.fill(x + TILE_W - 1, y + 1, x + TILE_W, y + TILE_H - 1, frame);
        ItemStack stack = MenuSync.getStack(e, "stack", minecraft.level.registryAccess());
        if (stack.isEmpty()) {
            stack = Gui.stack(e.getStringOr("item", ""));
        }
        List<Component> conditions = new ArrayList<>();
        // Gesperrte Stufe rot als Warnung, sonst dunkelgrau als Nebeninformation.
        MutableComponent tierLine = Component.translatable("dtp.contract.tier", tier);
        conditions.add(locked ? tierLine.withColor(Gui.WARN) : tierLine.withStyle(ChatFormatting.DARK_GRAY));
        // Nur gemeinsame Waren tragen einen Schlüssel; "" würde sonst zu "minecraft:" geparst.
        String rawKey = e.getStringOr("key", "");
        Identifier key = rawKey.isEmpty() ? null : Identifier.tryParse(rawKey);
        if (key != null) {
            // Gemeinsame Ware: Das Symbol wechselt jede Sekunde durch alle Arten, der Tooltip nennt die Gruppe.
            ListTag members = e.getListOrEmpty("members");
            if (!members.isEmpty()) {
                int index = (int) (System.currentTimeMillis() / 1000 % members.size());
                ItemStack member = Gui.stack(members.getStringOr(index, ""));
                if (!member.isEmpty()) {
                    stack = member;
                }
            }
            stack = stack.copy();
            stack.set(DataComponents.ITEM_NAME, Contract.groupName(key));
            conditions.add(Component.translatable("dtp.contract.condition_group").withStyle(ChatFormatting.GRAY));
        }
        if (e.getBooleanOr("min_enchant", false)) {
            conditions.add(Component.translatable("dtp.contract.condition_enchant").withStyle(ChatFormatting.GRAY));
        }
        if (e.getBooleanOr("undamaged", false)) {
            conditions.add(Component.translatable("dtp.contract.condition_undamaged").withStyle(ChatFormatting.GRAY));
        }
        int count = e.getIntOr("count", 0);
        int delivered = e.getIntOr("delivered", 0);
        int inStock = e.getIntOr("in_stock", 0);
        int stockColor = inStock >= count - delivered ? Gui.GOOD : inStock > 0 ? Gui.GOLD : Gui.MUTED;
        if (mine) {
            conditions.add(Component.translatable("dtp.contract.in_stock", inStock).withColor(stockColor));
            conditions.add(Component.translatable("dtp.contract.condition_renamed").withStyle(ChatFormatting.DARK_GRAY));
        }
        Gui.item(g, stack, x + 2, y + 2, mouseX, mouseY, conditions);

        // Zeile 1: Menge, bei eigenen Contracts mit Lagerbestand; bei gesperrter Stufe dahinter die Stufe.
        int textX = x + 21;
        int buttonsLeft = x + TILE_W - (mine ? 2 * BUTTON + 3 : BUTTON + 2);
        String amount = mine || delivered > 0 ? delivered + "/" + count : count + "×";
        g.text(font, amount, textX, y + 2, Gui.TEXT, false);
        int after = textX + font.width(amount);
        if (mine) {
            String stock = " (+" + inStock + ")";
            if (after + font.width(stock) > buttonsLeft - 2) {
                stock = " (+" + Gui.compact(inStock) + ")";
            }
            g.text(font, stock, after, y + 2, stockColor, false);
        } else if (locked) {
            g.text(font, Component.translatable("dtp.contract.tier", tier), after + 6, y + 2, Gui.WARN, false);
        }
        // Zeile 2: Belohnung.
        g.text(font, Money.component(e.getLongOr("reward", 0)), textX, y + 11, Gui.GOLD, false);
        // Zeile 3: aktueller Wert und nächste Absenkung; die ausgeschriebene Form steht im Tooltip.
        long percent = Math.round(e.getDoubleOr("factor", 1.0) * 100);
        long step = Math.round(e.getDoubleOr("drop_step", 0.0) * 100);
        double nextDrop = e.getDoubleOr("next_drop", -1);
        String days = String.format("%.1f", nextDrop);
        Component value = nextDrop < 0
                ? Component.translatable("dtp.contract.value_min", percent)
                : Component.translatable("dtp.contract.value_short", percent, step, days);
        int color = e.getBooleanOr("in_bonus", false) ? Gui.GOOD : Gui.GOLD;
        int valueY = y + TILE_H - 9;
        g.text(font, value, x + 3, valueY, color, false);
        if (nextDrop >= 0 && mouseX >= x && mouseX < x + TILE_W && mouseY >= valueY - 1 && mouseY < y + TILE_H) {
            g.setTooltipForNextFrame(font, Component.translatable("dtp.contract.value_until", percent, step, days), mouseX, mouseY);
        }
    }
}
