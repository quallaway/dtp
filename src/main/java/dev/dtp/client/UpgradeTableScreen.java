package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.currency.Money;
import dev.dtp.menu.UpgradeTableMenu;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;

/**
 * Upgrade-Baum im Stil der Advancements: ein Tab je Pfad, Upgrades als Symbole im Raster, Linien zu den
 * Voraussetzungen. Klick wählt ein Upgrade aus, gekauft wird über den Knopf darunter. Aufgeteilte Upgrades zeigen ein
 * Feld je Stufe mit eigenem Preis und eigener Beschreibung; jede Stufe hängt an der vorigen.
 * Der letzte Tab ist die Modulansicht: persönliche Module aus- und abrüsten, gezeichnet wie die Beacon-Effekte.
 */
public class UpgradeTableScreen extends PanelScreen<UpgradeTableMenu> {
    private static final int TABS_TOP = 30;
    private static final int CANVAS_X = 8;
    private static final int CANVAS_Y = 52;
    private static final int CANVAS_W = 264;
    private static final int CANVAS_H = 118;
    private static final int DETAIL_TOP = 176;
    private static final int PICKER_X = CANVAS_X + 6;
    private static final int PICKER_TOP = CANVAS_Y + 6;

    private final UpgradeTreeView tree = new UpgradeTreeView("player_upgrade.dtp.", CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, DETAIL_TOP);

    public UpgradeTableScreen(UpgradeTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 280, 236);
    }

    private boolean modulesView() {
        return data().getBooleanOr("modules_view", false);
    }

    // --- Knöpfe -----------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        CompoundTag d = data();
        boolean modules = modulesView();
        int current = modules ? -1 : d.getIntOr("tab", 0);
        PlayerUpgradeDefinitions.Tab[] tabs = PlayerUpgradeDefinitions.Tab.values();
        int tabWidth = CANVAS_W / (tabs.length + 1);
        for (int i = 0; i < tabs.length; i++) {
            // Der aktive Tab ist deaktiviert dargestellt — so ist sichtbar, wo man gerade ist.
            button(Component.translatable(tabs[i].translationKey()), CANVAS_X + i * tabWidth, TABS_TOP, tabWidth - 2,
                    UpgradeTableMenu.TAB + i, i != current);
        }
        button(Component.translatable("dtp.workshop.tab.modules"), CANVAS_X + tabs.length * tabWidth, TABS_TOP, tabWidth - 2,
                UpgradeTableMenu.MODULES, !modules);
        if (modules) {
            return;
        }
        UpgradeTreeView.Node sel = tree.selected(d, tree.nodes(d));
        if (sel != null) {
            Component label = sel.maxed() ? Component.translatable("dtp.upgrade.max") : Component.translatable("dtp.upgrade.buy");
            button(label, 214, DETAIL_TOP, 58, UpgradeTableMenu.BUY, tree.canBuy(sel, d.getBooleanOr("usable", false)));
        }
    }

    @Override
    protected String layoutKey() {
        CompoundTag d = data();
        return d.getBooleanOr("modules_view", false) + "|" + d.getIntOr("tab", 0) + "|" + d.getBooleanOr("usable", false)
                + "|" + tree.layoutKey(d);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x() - leftPos;
        int my = (int) event.y() - topPos;
        int button = modulesView() ? EffectPicker.clicked(data(), PICKER_X, PICKER_TOP, mx, my) : tree.clicked(data(), mx, my);
        if (button >= 0) {
            click(modulesView() ? UpgradeTableMenu.TOGGLE_MODULE + button : button);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        long balance = d.getLongOr("balance", 0);
        g.text(font, Component.translatable("dtp.balance", Money.component(balance), d.getIntOr("chests", 0)), 8, 18, Gui.MUTED, false);
        if (modulesView()) {
            g.fill(CANVAS_X, CANVAS_Y, CANVAS_X + CANVAS_W, CANVAS_Y + CANVAS_H, 0xFF161616);
            g.outline(CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, Gui.BORDER);
            EffectPicker.draw(g, font, d, PICKER_X, PICKER_TOP, mouseX, mouseY);
            moduleHint(g, d);
            return;
        }
        tree.draw(g, font, d, balance, d.getBooleanOr("usable", false), mouseX, mouseY);
    }

    /** Hinweis unter der Modulansicht: was fehlt, sonst wie Module wirken. */
    private void moduleHint(GuiGraphicsExtractor g, CompoundTag d) {
        Component hint;
        int color;
        if (d.getIntOr("effect_slots", 0) == 0) {
            hint = Component.translatable("dtp.workshop.modules.no_slot");
            color = Gui.WARN;
        } else if (!d.getBooleanOr("modules_usable", false)) {
            hint = Component.translatable("dtp.workshop.modules.outside");
            color = Gui.WARN;
        } else {
            hint = Component.translatable("dtp.workshop.modules.hint");
            color = Gui.MUTED;
        }
        g.textWithWordWrap(font, hint, 8, DETAIL_TOP, CANVAS_W, color, false);
    }

}
