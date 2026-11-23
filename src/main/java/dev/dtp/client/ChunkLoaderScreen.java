package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.currency.Money;
import dev.dtp.menu.ChunkLoaderMenu;

/** Chunkloader: Restlaufzeit, Kistenstand und ein Knopf für die nächste Ladung. */
public class ChunkLoaderScreen extends PanelScreen<ChunkLoaderMenu> {
    private static final int BUTTON_TOP = 58;

    public ChunkLoaderScreen(ChunkLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 240, 96);
    }

    /** Laufzeit in Minuten, aufgerundet: Der Chunkloader zählt Ticks und springt beim Schlafen nicht vor. */
    private static long minutes(long ticks) {
        return (ticks + 1199) / 1200;
    }

    private boolean canCharge() {
        CompoundTag d = data();
        long duration = d.getLongOr("duration", 0L);
        return d.getBooleanOr("enabled", false)
                && d.getLongOr("remaining", 0L) + duration <= d.getLongOr("max", 0L)
                && d.getLongOr("balance", 0L) >= d.getLongOr("price", 0L);
    }

    @Override
    protected void init() {
        super.init();
        CompoundTag d = data();
        if (d.isEmpty()) {
            return;
        }
        Component label = Component.translatable("dtp.chunkloader.charge",
                minutes(d.getLongOr("duration", 0L)), Money.component(d.getLongOr("price", 0L)));
        button(label, 8, BUTTON_TOP, imageWidth - 16, ChunkLoaderMenu.CHARGE, canCharge());
    }

    @Override
    protected String layoutKey() {
        CompoundTag d = data();
        return d.isEmpty() + "|" + canCharge() + "|" + d.getLongOr("price", 0L) + "|" + d.getLongOr("duration", 0L);
    }

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        if (d.isEmpty()) {
            return;
        }
        int width = imageWidth - 16;
        long remaining = d.getLongOr("remaining", 0L);
        Component status;
        int color;
        if (!d.getBooleanOr("enabled", false)) {
            status = Component.translatable("dtp.chunkloader.disabled");
            color = Gui.WARN;
        } else if (remaining > 0) {
            status = Component.translatable("dtp.chunkloader.remaining", minutes(remaining), minutes(d.getLongOr("max", 0L)));
            color = Gui.GOOD;
        } else {
            status = Component.translatable("dtp.chunkloader.inactive");
            color = Gui.MUTED;
        }
        g.text(font, Gui.fit(font, status, width), 8, 18, color, false);
        int size = d.getIntOr("size", 3);
        g.text(font, Gui.fit(font, Component.translatable("dtp.chunkloader.hint", size, size), width), 8, 30, Gui.MUTED, false);
        Component balance = Component.translatable("dtp.balance",
                Money.component(d.getLongOr("balance", 0L)), d.getIntOr("chests", 0));
        g.text(font, Gui.fit(font, balance, width), 8, 42, Gui.TEXT, false);
    }
}
