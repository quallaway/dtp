package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.currency.Money;
import dev.dtp.menu.DepositChestMenu;

public class DepositChestScreen extends SidePanelChestScreen<DepositChestMenu> {
    public DepositChestScreen(DepositChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 150);
    }

    @Override
    protected int panelHeight() {
        return 110;
    }

    @Override
    protected void init() {
        super.init();
        button(Component.translatable("dtp.deposit.compact"), 6, 64, 138, DepositChestMenu.BUTTON_COMPACT, true);
    }

    @Override
    protected String layoutKey() {
        return "";
    }

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        g.text(font, Component.translatable("dtp.deposit.own"), 6, 6, Gui.TEXT, false);
        g.text(font, Money.component(d.getLongOr("own", 0)), 6, 18, Gui.GOLD, false);
        g.text(font, Component.translatable("dtp.deposit.pool", d.getIntOr("chests", 0)), 6, 34, Gui.TEXT, false);
        g.text(font, Money.component(d.getLongOr("pool", 0)), 6, 46, Gui.GOLD, false);
    }
}
