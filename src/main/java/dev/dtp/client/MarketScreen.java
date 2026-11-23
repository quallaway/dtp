package dev.dtp.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.dtp.currency.Money;
import dev.dtp.market.Market;
import dev.dtp.market.Sales;
import dev.dtp.menu.MarketMenu;
import dev.dtp.menu.MenuSync;

/**
 * Zwei Spalten: links Verkauf aus der Hand und aus den Trade-Chests in Reichweite, rechts die Preisübersicht
 * mit Suche und Gruppenfilter. Ausgelegt auf 400×250 GUI-Punkte, damit es auch bei großer GUI-Skalierung in
 * 1080p passt.
 *
 * Suche, Filter und Blättern laufen auf dem Client: Nur er kennt die übersetzten Itemnamen, und die
 * Gruppen-Tags sind ihm ohnehin bekannt.
 */
public class MarketScreen extends PanelScreen<MarketMenu> {
    private static final int LEFT = 8;
    private static final int RIGHT = 206;
    private static final int RIGHT_WIDTH = 186;
    private static final int HAND_TOP = 44;
    private static final int STOCK_TOP = 102;
    private static final int STOCK_ROW = 18;
    private static final int SEARCH_TOP = 18;
    private static final int CATEGORY_TOP = 36;
    /** Oberkante der Spaltenköpfe bei einer Reihe Gruppenknöpfe; jede weitere Reihe schiebt sie nach unten. */
    private static final int HEADER_TOP = 60;
    /** Höhe der Spaltenköpfe über der Preisliste. */
    private static final int HEADER_HEIGHT = 10;
    private static final int PRICE_ROW = 16;
    private static final int PAGE_SIZE = 9;
    private static final int CHART_TOP = 46;
    private static final int CHART_HEIGHT = 94;

    private EditBox searchBox;
    private String query = "";
    private final CategoryBar categories = new CategoryBar(RIGHT, CATEGORY_TOP, RIGHT_WIDTH);
    private int page;
    /** Sortierung der Preisliste, per Klick auf einen Spaltenkopf; NONE = Reihenfolge der Handelsliste. */
    private SortColumn sort = SortColumn.NONE;
    private boolean descending;
    /** Erste sichtbare Lagerzeile; mehr als {@link MarketMenu#STOCK_ROWS} Waren werden mit dem Mausrad gescrollt. */
    private int stockScroll;

    public MarketScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 400, 250);
    }

    @Override
    protected void init() {
        super.init();
        CompoundTag d = data();
        boolean tradable = d.getBooleanOr("hand_tradable", false);
        int handButtons = HAND_TOP + 34;
        sellButton(Component.translatable("dtp.market.sell_one"), LEFT, handButtons, 56, MarketMenu.SELL_ONE,
                tradable, d.getLongOr("hand_one", 0));
        sellButton(Component.translatable("dtp.market.sell_stack", d.getIntOr("hand_count", 0)), LEFT + 60, handButtons, 64,
                MarketMenu.SELL_STACK, tradable, d.getLongOr("hand_stack", 0));
        sellButton(Component.translatable("dtp.market.sell_all", d.getIntOr("hand_all_count", 0)), LEFT + 128, handButtons, 60,
                MarketMenu.SELL_ALL, tradable, d.getLongOr("hand_all", 0));

        ListTag stock = d.getListOrEmpty("stock");
        clampStockScroll();
        // Die Knopf-ID trägt den echten Listenindex, nicht die sichtbare Zeile.
        for (int i = stockScroll; i < stockEnd(stock); i++) {
            sellButton(Component.translatable("dtp.market.sell"), LEFT + 128, STOCK_TOP + 12 + (i - stockScroll) * STOCK_ROW, 60,
                    MarketMenu.SELL_STOCK + i, true, stock.getCompoundOrEmpty(i).getLongOr("total", 0));
        }
        sellButton(Component.translatable("dtp.market.sell_stock_all", Money.format(d.getLongOr("stock_total", 0))),
                LEFT, STOCK_TOP + 12 + MarketMenu.STOCK_ROWS * STOCK_ROW + 2, 188, MarketMenu.SELL_STOCK_ALL,
                !stock.isEmpty(), d.getLongOr("stock_total", 0));

        int navY = priceTop() + PAGE_SIZE * PRICE_ROW + 2;
        if (chart() != null) {
            // Preisverlauf statt Preisliste; Suche und Filter bleiben im Hintergrund erhalten.
            button(Component.translatable("dtp.market.chart.back"), RIGHT, navY, RIGHT_WIDTH, MarketMenu.CHART_CLOSE, true);
            return;
        }
        initSearch();
        categories.init(d, this::local, () -> page = 0);
        int pages = pages();
        page = Math.min(page, pages - 1);
        local(Component.literal("<"), RIGHT, navY, 20, page > 0, () -> page--);
        local(Component.literal(">"), RIGHT + RIGHT_WIDTH - 20, navY, 20, page < pages - 1, () -> page++);
    }

    /**
     * Verkaufsknopf: ausgegraut, wenn der Erlös unter einem Taler läge — mit Tooltip, damit klar ist, warum.
     * Der Server prüft das beim Verkauf noch einmal.
     */
    private void sellButton(Component label, int x, int y, int width, int buttonId, boolean available, long revenue) {
        boolean belowMinimum = available && revenue < Sales.MIN_REVENUE;
        Button b = button(label, x, y, width, buttonId, available && !belowMinimum);
        if (belowMinimum) {
            b.setTooltip(Tooltip.create(Component.translatable("dtp.market.below_minimum")));
        }
    }

    private int stockEnd(ListTag stock) {
        return Math.min(stock.size(), stockScroll + MarketMenu.STOCK_ROWS);
    }

    /** Hält die Scroll-Position gültig, auch nachdem Ware verkauft wurde und die Liste kürzer ist. */
    private void clampStockScroll() {
        stockScroll = Math.clamp(stockScroll, 0, Math.max(0, data().getListOrEmpty("stock").size() - MarketMenu.STOCK_ROWS));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int lx = (int) x - leftPos;
        int ly = (int) y - topPos;
        int top = STOCK_TOP + 12;
        if (scrollY != 0 && lx >= LEFT && lx < RIGHT && ly >= top && ly < top + MarketMenu.STOCK_ROWS * STOCK_ROW) {
            int before = stockScroll;
            stockScroll += scrollY > 0 ? -1 : 1;
            clampStockScroll();
            if (stockScroll != before) {
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private CompoundTag chart() {
        return data().getCompound("chart").orElse(null);
    }

    /** Zeile der Preisliste unter der Maus, oder null — anklickbar nur mit „Marktinformation". */
    private CompoundTag priceRowAt(int mx, int my) {
        if (!data().getBooleanOr("info", false) || chart() != null || mx < RIGHT || mx >= RIGHT + RIGHT_WIDTH || my < priceTop()) {
            return null;
        }
        int row = (my - priceTop()) / PRICE_ROW;
        List<CompoundTag> shown = filtered();
        int index = page * PAGE_SIZE + row;
        return row < PAGE_SIZE && index < shown.size() ? shown.get(index) : null;
    }

    /** Spaltenkopf unter der Maus, oder NONE. */
    private SortColumn headerAt(int mx, int my) {
        if (chart() != null || my < headerTop() || my >= priceTop() - 1) {
            return SortColumn.NONE;
        }
        int pressureX = pressureHeaderX();
        if (mx >= pressureX - 3 && mx < pressureX + font.width(header("dtp.market.column.pressure", SortColumn.PRESSURE)) + 2) {
            return SortColumn.PRESSURE;
        }
        int priceRight = pressureX - 6;
        if (mx >= priceRight - font.width(header("dtp.market.column.price", SortColumn.PRICE)) - 2 && mx < pressureX - 3) {
            return SortColumn.PRICE;
        }
        return SortColumn.NONE;
    }

    /** Klick auf einen Spaltenkopf: absteigend → aufsteigend → unsortiert; eine neue Spalte beginnt absteigend. */
    private void toggleSort(SortColumn column) {
        if (sort != column) {
            sort = column;
            descending = true;
        } else if (descending) {
            descending = false;
        } else {
            sort = SortColumn.NONE;
        }
        page = 0;
        rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        SortColumn header = headerAt((int) event.x() - leftPos, (int) event.y() - topPos);
        if (header != SortColumn.NONE) {
            Button.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            toggleSort(header);
            return true;
        }
        CompoundTag row = priceRowAt((int) event.x() - leftPos, (int) event.y() - topPos);
        if (row != null) {
            click(MarketMenu.CHART + row.getIntOr("index", 0));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Das Suchfeld überlebt den Neuaufbau der Knöpfe, damit Text und Fokus erhalten bleiben. */
    private void initSearch() {
        if (searchBox == null) {
            searchBox = new EditBox(font, 0, 0, RIGHT_WIDTH, 14, Component.translatable("dtp.market.search"));
            searchBox.setHint(Component.translatable("dtp.market.search"));
            searchBox.setMaxLength(50);
            searchBox.setResponder(text -> {
                query = text;
                page = 0;
            });
        }
        searchBox.setPosition(leftPos + RIGHT, topPos + SEARCH_TOP);
        addRenderableWidget(searchBox);
    }

    /** Jede Reihe Gruppenknöpfe über der ersten schiebt die Preisliste nach unten und macht das Panel höher. */
    private int extraCategoryHeight() {
        return categories.extraHeight(data());
    }

    private int headerTop() {
        return HEADER_TOP + extraCategoryHeight();
    }

    private int priceTop() {
        return headerTop() + HEADER_HEIGHT;
    }

    @Override
    protected int panelHeight() {
        return imageHeight + extraCategoryHeight();
    }

    /** Knopf, der nur die Ansicht ändert und nichts an den Server schickt. */
    private Button local(Component label, int x, int y, int width, boolean active, Runnable action) {
        Button b = Button.builder(label, btn -> {
            action.run();
            rebuildWidgets();
        }).bounds(leftPos + x, topPos + y, width, 18).build();
        b.active = active;
        return addRenderableWidget(b);
    }

    // --- Filter -----------------------------------------------------------------------------

    private List<CompoundTag> filtered() {
        CompoundTag d = data();
        TagKey<Item> tag = categories.tag(d);
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<CompoundTag> result = new ArrayList<>();
        ListTag prices = d.getListOrEmpty("prices");
        for (int i = 0; i < prices.size(); i++) {
            CompoundTag e = prices.getCompoundOrEmpty(i);
            String id = e.getStringOr("item", "");
            ItemStack stack = display(e, "item", "stack");
            if (!CategoryBar.matches(tag, stack)) {
                continue;
            }
            if (!q.isEmpty() && !stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q) && !id.contains(q)) {
                continue;
            }
            result.add(e);
        }
        Comparator<CompoundTag> order = switch (sort) {
            case PRICE -> Comparator.comparingDouble(e -> e.getDoubleOr("price", 0));
            case PRESSURE -> Comparator.comparingDouble(e -> e.getDoubleOr("band", 0.5));
            case NONE -> null;
        };
        if (order != null) {
            // Stabil: Gleichstände behalten die Reihenfolge der Handelsliste.
            result.sort(descending ? order.reversed() : order);
        }
        return result;
    }

    private int pages() {
        return Math.max(1, (filtered().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    @Override
    protected String layoutKey() {
        CompoundTag d = data();
        // Welche Verkaufsknöpfe unter dem Mindesterlös liegen, entscheidet über ausgegraut und Tooltip.
        StringBuilder belowMinimum = new StringBuilder();
        for (String key : List.of("hand_one", "hand_stack", "hand_all")) {
            belowMinimum.append(d.getLongOr(key, 0) < Sales.MIN_REVENUE ? '0' : '1');
        }
        ListTag stock = d.getListOrEmpty("stock");
        clampStockScroll();
        for (int i = 0; i < stock.size(); i++) {
            belowMinimum.append(stock.getCompoundOrEmpty(i).getLongOr("total", 0) < Sales.MIN_REVENUE ? '0' : '1');
        }
        return d.getBooleanOr("hand_tradable", false) + "|" + d.getIntOr("hand_count", 0) + "|" + d.getIntOr("hand_all_count", 0)
                + "|" + belowMinimum + "|" + stock.size() + "|" + stockScroll + "|" + d.getLongOr("stock_total", 0)
                + "|" + d.getListOrEmpty("categories").size() + "|" + page + "|" + pages() + "|" + (chart() != null);
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        // Erste Zeile: Außerhalb des Territoriums der Warnhinweis, sonst das angekündigte Marktereignis.
        Component eventTarget = MenuSync.getComponent(d, "event_target");
        if (d.getBooleanOr("outside", false)) {
            g.text(font, Gui.fit(font, Component.translatable("dtp.market.outside_short"), 188), LEFT, 18, Gui.WARN, false);
        } else if (eventTarget != null) {
            String type = d.getStringOr("event_type", "demand_surge");
            int running = d.getIntOr("running_count", 0);
            MutableComponent event = Component.translatable("dtp.event.next", d.getLongOr("event_minutes", 0),
                    Component.translatable("dtp.event.type." + type), eventTarget);
            if (running > 0) {
                event.append(Component.translatable("dtp.event.running_suffix", running));
            }
            g.text(font, Gui.fit(font, event, 188), LEFT, 18, Gui.GOLD, false);
            if (mouseX >= LEFT && mouseX < LEFT + 188 && mouseY >= 18 && mouseY < 27) {
                // Hinweis zum angekündigten Ereignis, darunter laufende Ereignisse und Aufschläge aus der Welt.
                List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.translatable("dtp.event.next_hint." + type, eventTarget), 200));
                if (running > 0) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(font.split(Component.translatable("dtp.event.running_header").withStyle(ChatFormatting.GOLD), 200));
                    for (int i = 0; i < running; i++) {
                        Component line = MenuSync.getComponent(d, "running_" + i);
                        if (line != null) {
                            lines.addAll(font.split(line, 200));
                        }
                    }
                }
                g.setTooltipForNextFrame(lines, mouseX, mouseY);
            }
        }
        int chests = d.getIntOr("chests", 0);
        Component deposit = switch (chests) {
            case 0 -> Component.translatable("dtp.market.deposit_none");
            case 1 -> Component.translatable("dtp.market.deposit_one");
            default -> Component.translatable("dtp.market.deposit_many", chests);
        };
        g.text(font, deposit, LEFT, 30, chests == 0 ? Gui.WARN : Gui.MUTED, false);

        hand(g, d, mouseX, mouseY);
        stock(g, d, mouseX, mouseY);
        CompoundTag chart = chart();
        if (chart != null) {
            chart(g, d, chart, mouseX, mouseY);
        } else {
            categories.icons(g, d);
            headers(g, d, mouseX, mouseY);
            prices(g, d, mouseX, mouseY);
        }
    }

    /** Das Item einer Zeile: der mitgeschickte Anzeige-Stapel (Ware mit Komponenten, z. B. Böses Banner) oder die Item-ID. */
    private ItemStack display(CompoundTag e, String idKey, String stackKey) {
        if (e.contains(stackKey) && minecraft != null && minecraft.level != null) {
            ItemStack stack = MenuSync.getStack(e, stackKey, minecraft.level.registryAccess());
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return Gui.stack(e.getStringOr(idKey, ""));
    }

    private void hand(GuiGraphicsExtractor g, CompoundTag d, int mouseX, int mouseY) {
        g.text(font, Component.translatable("dtp.market.hand"), LEFT, HAND_TOP, Gui.TEXT, false);
        ItemStack hand = display(d, "hand_item", "hand_display");
        Gui.item(g, hand, LEFT, HAND_TOP + 12, mouseX, mouseY);
        int x = LEFT + 20;
        if (hand.isEmpty()) {
            g.text(font, Component.translatable("dtp.market.hand_empty"), x, HAND_TOP + 16, Gui.MUTED, false);
        } else if (!d.getBooleanOr("hand_tradable", false)) {
            g.text(font, Component.translatable("dtp.market.not_tradable"), x, HAND_TOP + 16, Gui.WARN, false);
        } else {
            double band = d.getDoubleOr("hand_band", 0.5);
            int right = LEFT + 188;
            int indicatorX;
            if (d.contains("hand_factor")) {
                // „Marktinformation“: Prozentwert mit Trendpfeil in der Farbe, die die Druckskala an dieser Stelle hat.
                boolean rising = d.getBooleanOr("hand_rising", false);
                boolean falling = d.getBooleanOr("hand_falling", false);
                String factor = String.format("%d%%%s", Math.round(d.getDoubleOr("hand_factor", 1) * 100),
                        rising ? "↑" : falling ? "↓" : "→");
                indicatorX = right - font.width(factor);
                g.text(font, factor, indicatorX, HAND_TOP + 12, Gui.gaugeColor((float) band), false);
                if (mouseX >= indicatorX && mouseX < right && mouseY >= HAND_TOP + 11 && mouseY < HAND_TOP + 21) {
                    g.setTooltipForNextFrame(font, Component.translatable(Gui.gaugeTooltipKey(band, neutral(d))), mouseX, mouseY);
                }
            } else {
                indicatorX = right - Gui.GAUGE_WIDTH;
                Gui.gauge(g, indicatorX, HAND_TOP + 14, band, neutral(d), mouseX, mouseY);
            }
            g.text(font, Gui.fit(font, Component.translatable("dtp.market.unit_price", Money.formatPrice(d.getDoubleOr("hand_unit", 0))),
                    indicatorX - 4 - x), x, HAND_TOP + 12, Gui.TEXT, false);
            g.text(font, Gui.fit(font, Component.translatable("dtp.market.totals",
                    Money.format(d.getLongOr("hand_stack", 0)), Money.format(d.getLongOr("hand_all", 0))), 168), x, HAND_TOP + 22, Gui.MUTED, false);
        }
    }

    private void stock(GuiGraphicsExtractor g, CompoundTag d, int mouseX, int mouseY) {
        Component header = Component.translatable("dtp.market.stock", d.getIntOr("trade_chests", 0));
        g.text(font, Gui.fit(font, header, 188), LEFT, STOCK_TOP, Gui.TEXT, false);
        ListTag stock = d.getListOrEmpty("stock");
        if (stock.isEmpty()) {
            Component empty = Component.translatable(d.getIntOr("trade_chests", 0) == 0 ? "dtp.market.no_trade_chest" : "dtp.market.stock_empty");
            g.textWithWordWrap(font, empty, LEFT, STOCK_TOP + 16, 188, Gui.MUTED, false);
        }
        for (int i = stockScroll; i < stockEnd(stock); i++) {
            CompoundTag e = stock.getCompoundOrEmpty(i);
            int y = STOCK_TOP + 12 + (i - stockScroll) * STOCK_ROW;
            ItemStack stack = display(e, "item", "stack");
            Gui.item(g, stack, LEFT, y + 1, mouseX, mouseY);
            g.text(font, e.getIntOr("count", 0) + "×", LEFT + 20, y + 5, Gui.TEXT, false);
            long amount = e.getLongOr("total", 0);
            String total = Money.format(amount);
            // Unter dem Mindesterlös bleibt die Zeile liegen — gedämpft statt golden.
            g.text(font, total, LEFT + 94 - font.width(total), y + 5, amount < Sales.MIN_REVENUE ? Gui.MUTED : Gui.GOLD, false);
            Gui.gauge(g, LEFT + 98, y + 7, e.getDoubleOr("band", 0.5), neutral(d), mouseX, mouseY);
        }
        // Scrollleiste rechts neben den Verkaufsknöpfen — nur, wenn mehr Ware da ist als Zeilen.
        int rows = MarketMenu.STOCK_ROWS;
        if (stock.size() > rows) {
            int top = STOCK_TOP + 12;
            int h = rows * STOCK_ROW;
            int thumb = Math.max(6, h * rows / stock.size());
            int thumbY = top + (h - thumb) * stockScroll / (stock.size() - rows);
            g.fill(LEFT + 190, top, LEFT + 193, top + h, 0xFF202020);
            g.fill(LEFT + 190, thumbY, LEFT + 193, thumbY + thumb, Gui.MUTED);
        }
    }

    /** Linke Kante der Druckanzeige in der Preisliste; mit „Marktinformation“ rückt sie für Faktor und Trend nach links. */
    private static int gaugeX(boolean info) {
        return info ? RIGHT + 126 : RIGHT + RIGHT_WIDTH - Gui.GAUGE_WIDTH;
    }

    /** Spaltenköpfe „Preis“ und „Druck“: anklickbar zum Sortieren, die aktive Spalte golden mit Richtungspfeil. */
    private void headers(GuiGraphicsExtractor g, CompoundTag d, int mouseX, int mouseY) {
        int pressureX = pressureHeaderX();
        int y = headerTop();
        SortColumn hovered = headerAt(mouseX, mouseY);
        Component price = header("dtp.market.column.price", SortColumn.PRICE);
        g.text(font, price, pressureX - 6 - font.width(price), y, headerColor(SortColumn.PRICE, hovered), false);
        g.text(font, header("dtp.market.column.pressure", SortColumn.PRESSURE), pressureX, y, headerColor(SortColumn.PRESSURE, hovered), false);
        if (hovered != SortColumn.NONE) {
            g.setTooltipForNextFrame(font, Component.translatable("dtp.market.sort_hint"), mouseX, mouseY);
        }
    }

    /**
     * Linke Kante des Kopfes „Druck“: über der Druckanzeige, aber nie über den rechten Rand der Liste hinaus —
     * ohne „Marktinformation“ steht die Anzeige ganz rechts und der Kopf ist breiter als sie.
     */
    private int pressureHeaderX() {
        int width = font.width(header("dtp.market.column.pressure", SortColumn.PRESSURE));
        return Math.min(gaugeX(data().getBooleanOr("info", false)), RIGHT + RIGHT_WIDTH - width);
    }

    private Component header(String key, SortColumn column) {
        Component label = Component.translatable(key);
        if (sort != column) {
            return label;
        }
        return Component.empty().append(label).append(descending ? " ▼" : " ▲");
    }

    private int headerColor(SortColumn column, SortColumn hovered) {
        return sort == column ? Gui.GOLD : hovered == column ? Gui.TEXT : Gui.MUTED;
    }

    private void prices(GuiGraphicsExtractor g, CompoundTag d, int mouseX, int mouseY) {
        boolean info = d.getBooleanOr("info", false);
        List<CompoundTag> shown = filtered();
        if (shown.isEmpty()) {
            g.text(font, Component.translatable("dtp.market.no_match"), RIGHT, priceTop() + 4, Gui.MUTED, false);
        }
        int from = page * PAGE_SIZE;
        CompoundTag hovered = priceRowAt(mouseX, mouseY);
        for (int i = from; i < Math.min(shown.size(), from + PAGE_SIZE); i++) {
            CompoundTag e = shown.get(i);
            int y = priceTop() + (i - from) * PRICE_ROW;
            if (e == hovered) {
                // Hervorhebung zeigt, dass die Zeile den Preisverlauf öffnet.
                g.fill(RIGHT - 2, y - 1, RIGHT + RIGHT_WIDTH + 2, y + PRICE_ROW - 1, 0x30FFFFFF);
            }
            ItemStack stack = display(e, "item", "stack");
            Gui.item(g, stack, RIGHT, y, mouseX, mouseY);
            // Spalten von rechts: [Faktor+Trend nur mit Marktinformation] · Druckanzeige · Preis · Name
            int gaugeX = gaugeX(info);
            Gui.gauge(g, gaugeX, y + 6, e.getDoubleOr("band", 0.5), neutral(d), mouseX, mouseY);
            String price = Money.formatPrice(e.getDoubleOr("price", 0));
            g.text(font, price, gaugeX - 6 - font.width(price), y + 4, Gui.GOLD, false);
            g.text(font, Gui.fit(font, stack.getHoverName(), info ? 60 : 94), RIGHT + 20, y + 4, Gui.TEXT, false);
            if (info) {
                // ↑ erholt sich nach oben, ↓ sinkt nach einem Marktereignis zurück auf den Ruhepreis, → steht.
                boolean rising = e.getBooleanOr("rising", false);
                boolean falling = e.getBooleanOr("falling", false);
                String arrow = rising ? "↑" : falling ? "↓" : "→";
                String factor = String.format("%d%%%s", Math.round(e.getDoubleOr("factor", 1) * 100), arrow);
                g.text(font, factor, RIGHT + RIGHT_WIDTH - font.width(factor), y + 4, rising ? Gui.GOOD : falling ? Gui.GOLD : Gui.MUTED, false);
            }
        }
        int navY = priceTop() + PAGE_SIZE * PRICE_ROW + 2;
        Component nav = Component.translatable("dtp.market.page", page + 1, pages(), shown.size());
        g.centeredText(font, nav, RIGHT + RIGHT_WIDTH / 2, navY + 5, Gui.MUTED);
    }

    /**
     * Preisverlauf der gewählten Ware auf linearer Achse vom unteren bis zum oberen Rand des Preisbands. Die
     * transparente Linie markiert den Ruhepreis: darunter haben Verkäufe den Preis gedrückt, darüber ein
     * Marktereignis gehoben. Darunter die Kennzahlen je in einer Zeile.
     */
    private void chart(GuiGraphicsExtractor g, CompoundTag d, CompoundTag chart, int mouseX, int mouseY) {
        ItemStack stack = display(chart, "item", "stack");
        Gui.item(g, stack, RIGHT, SEARCH_TOP, mouseX, mouseY);
        g.text(font, Gui.fit(font, stack.getHoverName(), RIGHT_WIDTH - 20), RIGHT + 20, SEARCH_TOP + 4, Gui.TEXT, false);
        long percent = Math.round(chart.getDoubleOr("factor", 1.0) * 100);
        g.text(font, Gui.fit(font, Component.translatable("dtp.market.chart.now",
                Money.formatPrice(chart.getDoubleOr("price", 0.0)), percent), RIGHT_WIDTH), RIGHT, SEARCH_TOP + 18, Gui.GOLD, false);

        int left = RIGHT;
        int top = CHART_TOP;
        int w = RIGHT_WIDTH;
        int h = CHART_HEIGHT;
        double min = chart.getDoubleOr("min", 0.05);
        double max = Math.max(min + 1e-6, chart.getDoubleOr("max", 2.5));
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFF101010);
        for (int y = 0; y < h; y++) {
            // Farbe wie in den Druckanzeigen, damit derselbe Preis überall gleich aussieht.
            double factor = max - y / (double) (h - 1) * (max - min);
            int color = Gui.gaugeColor((float) Market.bandPosition(factor));
            g.fill(left, top + y, left + w, top + y + 1, (color & 0x00FFFFFF) | 0x50000000);
        }
        int restY = chartY(chart.getDoubleOr("rest", 1.41), min, max);
        g.fill(left, restY, left + w, restY + 1, 0xA0FFFFFF);

        int days = Math.max(2, chart.getIntOr("days", 14));
        ListTag points = chart.getListOrEmpty("points");
        // Je Tag ein Balken für die Spanne (Tiefst- bis Höchstwert), die Linie verbindet die letzten Werte der Tage.
        // Ganz rechts endet sie beim aktuellen Wert.
        int prevX = -1;
        int prevY = -1;
        for (int i = 0; i <= points.size(); i++) {
            boolean current = i == points.size();
            CompoundTag p = current ? null : points.getCompoundOrEmpty(i);
            int ago = current ? 0 : p.getIntOr("ago", 0);
            int x = left + (int) Math.round((days - 1 - ago) / (double) (days - 1) * (w - 1));
            int y = chartY(current ? chart.getDoubleOr("factor", 1.0) : p.getDoubleOr("close", 1.0), min, max);
            if (!current) {
                int yHigh = chartY(p.getDoubleOr("high", 1.0), min, max);
                int yLow = chartY(p.getDoubleOr("low", 1.0), min, max);
                g.fill(x - 1, yHigh, x + 2, yLow + 1, 0x90FFFFFF);
            }
            if (prevX >= 0) {
                line(g, prevX, prevY, x, y);
            }
            g.fill(x - 1, y - 1, x + 2, y + 2, current ? Gui.GOLD : Gui.TEXT);
            prevX = x;
            prevY = y;
        }
        g.text(font, Component.translatable("dtp.market.chart.days_ago", days - 1), left, top + h + 3, Gui.MUTED, false);
        Component today = Component.translatable("dtp.market.chart.today");
        g.text(font, today, left + w - font.width(today), top + h + 3, Gui.MUTED, false);

        int textY = top + h + 14;
        row(g, Component.translatable("dtp.market.chart.rest"), Component.translatable("dtp.market.chart.price_percent",
                Money.formatPrice(chart.getDoubleOr("rest_price", 0.0)), Math.round(chart.getDoubleOr("rest", 1.41) * 100)), textY);
        row(g, Component.translatable("dtp.market.chart.lowest"),
                Component.literal(Math.round(chart.getDoubleOr("low", 1.0) * 100) + " %"), textY + 11);
        row(g, Component.translatable("dtp.market.chart.highest"),
                Component.literal(Math.round(chart.getDoubleOr("high", 1.0) * 100) + " %"), textY + 22);
        row(g, Component.translatable("dtp.market.chart.half_amount"),
                Component.literal(String.valueOf(Math.round(chart.getDoubleOr("half_amount", 0.0)))), textY + 33);
        // Unter einem Markttag lohnt keine Angabe — die Zeile bleibt dann leer.
        double toRest = chart.getDoubleOr("days_to_rest", 0.0);
        if (toRest > 1.0) {
            Component recovery = Component.translatable("dtp.market.chart.recovery", String.format(Locale.ROOT, "%.1f", toRest));
            g.text(font, Gui.fit(font, recovery, w), left, textY + 44, Gui.MUTED, false);
        }
    }

    /** Höhe eines Preisfaktors im Graphen, linear zwischen {@code min} (unten) und {@code max} (oben). */
    private static int chartY(double factor, double min, double max) {
        double t = Math.clamp((factor - min) / (max - min), 0.0, 1.0);
        return CHART_TOP + (int) Math.round((1.0 - t) * (CHART_HEIGHT - 1));
    }

    /** Kennzahl unter dem Graphen: Bezeichnung links, Wert rechtsbündig. */
    private void row(GuiGraphicsExtractor g, Component label, Component value, int y) {
        int valueWidth = font.width(value);
        g.text(font, value, RIGHT + RIGHT_WIDTH - valueWidth, y, Gui.TEXT, false);
        g.text(font, Gui.fit(font, label, RIGHT_WIDTH - valueWidth - 6), RIGHT, y, Gui.MUTED, false);
    }

    /** Linie aus kleinen Rechtecken — die Oberfläche kennt keine Linien. */
    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (steps == 0 ? 0 : (x1 - x0) * i / steps);
            int y = y0 + (steps == 0 ? 0 : (y1 - y0) * i / steps);
            g.fill(x, y, x + 1, y + 2, Gui.TEXT);
        }
    }

    private static double neutral(CompoundTag d) {
        return d.getDoubleOr("neutral", 0.5);
    }

    private enum SortColumn { NONE, PRICE, PRESSURE }
}
