package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.currency.Money;
import dev.dtp.menu.BeaconMenu;
import dev.dtp.menu.EffectSelection;
import dev.dtp.upgrade.UpgradeCosts;

/**
 * Beacon: Server-Upgrades als Baum auf den Reitern Welt, Markt und Module — gezeichnet wie die Werkstatt
 * ({@link UpgradeTreeView}) — und als vierter Reiter die Effektauswahl.
 */
public class BeaconScreen extends PanelScreen<BeaconMenu> {
    private static final int TABS_TOP = 42;
    private static final int CANVAS_X = 8;
    private static final int CANVAS_Y = 64;
    private static final int CANVAS_W = 264;
    private static final int CANVAS_H = 118;
    private static final int DETAIL_TOP = 188;
    private static final int PICKER_TOP = 68;

    private final UpgradeTreeView tree = new UpgradeTreeView("upgrade.dtp.", CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, DETAIL_TOP);

    public BeaconScreen(BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 280, 246);
    }

    private boolean effectsTab() {
        return data().getBooleanOr("effects_tab", false);
    }

    @Override
    protected void init() {
        super.init();
        CompoundTag d = data();
        if (!d.getBooleanOr("started", false)) {
            button(Component.translatable("dtp.beacon.confirm"), 8, 110, 170, BeaconMenu.CONFIRM, true);
            button(Component.translatable("dtp.beacon.cancel"), 184, 110, 88, BeaconMenu.CANCEL, true);
            return;
        }
        int current = d.getIntOr("tab", 0);
        UpgradeCosts.Tab[] tabs = UpgradeCosts.Tab.values();
        int tabWidth = CANVAS_W / (tabs.length + 1);
        // Der aktive Reiter ist deaktiviert dargestellt — wie in der Werkstatt.
        for (int i = 0; i < tabs.length; i++) {
            button(Component.translatable(tabs[i].translationKey()), CANVAS_X + i * tabWidth, TABS_TOP, tabWidth - 2, BeaconMenu.TAB + i,
                    i != current);
        }
        button(Component.translatable("dtp.beacon.tab.effects"), CANVAS_X + tabs.length * tabWidth, TABS_TOP, tabWidth - 2,
                BeaconMenu.TAB + BeaconMenu.EFFECTS_TAB, current != BeaconMenu.EFFECTS_TAB);
        if (effectsTab()) {
            return;
        }
        UpgradeTreeView.Node sel = tree.selected(d, tree.nodes(d));
        if (sel != null) {
            Component label = sel.maxed() ? Component.translatable("dtp.upgrade.max") : Component.translatable("dtp.upgrade.buy");
            button(label, 214, DETAIL_TOP, 58, BeaconMenu.BUY, tree.canBuy(sel, true));
        }
    }

    @Override
    protected String layoutKey() {
        CompoundTag d = data();
        return d.getBooleanOr("started", false) + "|" + d.getIntOr("tab", 0) + "|" + tree.layoutKey(d);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (data().getBooleanOr("started", false)) {
            int mx = (int) event.x() - leftPos;
            int my = (int) event.y() - topPos;
            if (effectsTab()) {
                int ordinal = EffectPicker.clicked(data(), 8, PICKER_TOP, mx, my);
                if (ordinal >= 0) {
                    click(EffectSelection.TOGGLE + ordinal);
                    return true;
                }
            } else {
                int button = tree.clicked(data(), mx, my);
                if (button >= 0) {
                    click(button);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        if (!d.getBooleanOr("started", false)) {
            g.textWithWordWrap(font, Component.translatable("dtp.beacon.pending"), 8, 22, imageWidth - 16, Gui.TEXT, false);
            return;
        }
        Component radius = d.getBooleanOr("border_removed", false)
                ? Component.translatable("dtp.beacon.border_removed")
                : Component.translatable("dtp.beacon.radius", d.getIntOr("radius", 0));
        g.text(font, radius, 8, 18, Gui.GOOD, false);
        long balance = d.getLongOr("balance", 0);
        g.text(font, Component.translatable("dtp.balance", Money.component(balance), d.getIntOr("chests", 0)), 8, 30, Gui.MUTED, false);

        if (effectsTab()) {
            EffectPicker.draw(g, font, d, 8, PICKER_TOP, mouseX, mouseY);
            return;
        }
        tree.draw(g, font, d, balance, true, mouseX, mouseY);
    }
}
