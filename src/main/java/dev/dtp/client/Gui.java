package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import dev.dtp.upgrade.UpgradeIcon;

/** Gemeinsame Zeichenhilfen für die DTP-Oberflächen. */
public final class Gui {
    public static final int PANEL = 0xF0262626;
    public static final int BORDER = 0xFF8B8B8B;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFA0A0A0;
    public static final int WARN = 0xFFFF6060;
    public static final int GOOD = 0xFF70E070;
    public static final int GOLD = 0xFFFFD050;

    private Gui() {
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, BORDER);
    }

    public static ItemStack stack(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /**
     * Upgrade-Symbol aus Menüdaten ({@code icon} = Item, {@code icon_sprite} = GUI-Sprite, siehe {@link UpgradeIcon}),
     * mittig in einem Feld der Kantenlänge {@code box}. Gibt {@code true} zurück, wenn ein Item gezeichnet wurde: Items
     * lassen sich nicht mit Alpha zeichnen, gesperrte Felder dunkelt der Aufrufer dann selbst ab.
     */
    public static boolean icon(GuiGraphicsExtractor g, CompoundTag tag, int x, int y, int box, float alpha) {
        Identifier sprite = Identifier.tryParse(tag.getStringOr("icon_sprite", ""));
        if (tag.contains("icon_sprite") && sprite != null) {
            return icon(g, UpgradeIcon.sprite(sprite), x, y, box, alpha);
        }
        g.item(stack(tag.getStringOr("icon", "")), x + (box - 16) / 2, y + (box - 16) / 2);
        return true;
    }

    /** Wie oben, direkt für ein {@link UpgradeIcon}. Sprites in ihrer echten Größe (Vanilla-Effekte 18×18, eigene 16×16). */
    public static boolean icon(GuiGraphicsExtractor g, UpgradeIcon icon, int x, int y, int box, float alpha) {
        if (!icon.sprite()) {
            g.item(stack(icon.id().toString()), x + (box - 16) / 2, y + (box - 16) / 2);
            return true;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(icon.id());
        int w = sprite.contents().width();
        int h = sprite.contents().height();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, icon.id(), x + (box - w) / 2, y + (box - h) / 2, w, h, alpha);
        return false;
    }

    /** Zeichnet ein Item-Symbol und merkt einen Tooltip vor, wenn die Maus darüber liegt. */
    public static void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        item(g, stack, x, y, mouseX, mouseY, List.of());
    }

    /** Wie oben, mit zusätzlichen Zeilen unter dem Item-Tooltip. */
    public static void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY, List<Component> extra) {
        g.item(stack, x, y);
        if (!stack.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            Minecraft minecraft = Minecraft.getInstance();
            if (extra.isEmpty()) {
                g.setTooltipForNextFrame(minecraft.font, stack, mouseX, mouseY);
            } else {
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
                lines.addAll(extra);
                g.setTooltipForNextFrame(minecraft.font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
            }
        }
    }

    public static final int GAUGE_WIDTH = 26;
    private static final int GAUGE_HEIGHT = 4;
    private static final int GAUGE_RED = 0xFFD03A30;
    private static final int GAUGE_YELLOW = 0xFFE0C030;
    private static final int GAUGE_GREEN = 0xFF40C040;

    /**
     * Druckanzeige: eine Skala von Rot (Preis an der Untergrenze, Markt gesättigt) bis Grün (Preis an der
     * Obergrenze). Die Markierung zeigt den aktuellen Stand, der feine Strich den Ruhepreis — linear wie der Preisverlauf.
     *
     * @param position Stand im Preisband, 0..1
     * @param neutral  Lage des Ruhepreises im Preisband, 0..1
     */
    public static void gauge(GuiGraphicsExtractor g, int x, int y, double position, double neutral, int mouseX, int mouseY) {
        int w = GAUGE_WIDTH;
        g.fill(x - 1, y - 1, x + w + 1, y + GAUGE_HEIGHT + 1, 0xFF101010);
        for (int i = 0; i < w; i++) {
            g.fill(x + i, y, x + i + 1, y + GAUGE_HEIGHT, gaugeColor(i / (float) (w - 1)));
        }
        int n = x + (int) Math.round(neutral * (w - 1));
        g.fill(n, y, n + 1, y + GAUGE_HEIGHT, 0xA0FFFFFF);
        int m = x + (int) Math.round(position * (w - 1));
        g.fill(m - 1, y - 2, m + 2, y + GAUGE_HEIGHT + 2, 0xFF101010);
        g.fill(m, y - 1, m + 1, y + GAUGE_HEIGHT + 1, 0xFFFFFFFF);
        if (mouseX >= x - 1 && mouseX <= x + w && mouseY >= y - 2 && mouseY <= y + GAUGE_HEIGHT + 2) {
            g.setTooltipForNextFrame(Minecraft.getInstance().font, Component.translatable(gaugeTooltipKey(position, neutral)), mouseX, mouseY);
        }
    }

    /** Übersetzungsschlüssel des Hover-Texts zur Druckanzeige: gesättigt, normal oder gefragt. */
    public static String gaugeTooltipKey(double position, double neutral) {
        return position < neutral - 0.04 ? "dtp.gauge.saturated"
                : position > neutral + 0.04 ? "dtp.gauge.in_demand" : "dtp.gauge.normal";
    }

    /** Farbe der Druckskala an Position {@code t} (0 = rot/gesättigt, 1 = grün/gefragt). */
    public static int gaugeColor(float t) {
        return t < 0.5f ? lerp(GAUGE_RED, GAUGE_YELLOW, t * 2f) : lerp(GAUGE_YELLOW, GAUGE_GREEN, (t - 0.5f) * 2f);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gr = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    /** Kürzt Text auf eine Breite, damit lange Itemnamen keine Knöpfe überdecken. */
    public static String fit(Font font, Component text, int width) {
        String s = text.getString();
        if (font.width(s) <= width) {
            return s;
        }
        while (!s.isEmpty() && font.width(s + "…") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /** Rahmenfarbe einer Contract-Stufe: Kupfer, Eisen, Gold, Diamant. */
    public static int tierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFFC06A45;
            case 2 -> 0xFFD8D8D8;
            case 3 -> 0xFFF2C230;
            case 4 -> 0xFF4AE0D0;
            default -> MUTED;
        };
    }

    /** Kurzform für große Mengen: 1728 → „1.7k“, 12400 → „12k“, 3 400 000 → „3.4M“. */
    public static String compact(long n) {
        if (n < 1000) {
            return Long.toString(n);
        }
        if (n < 1_000_000) {
            return n < 10_000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0) : n / 1000 + "k";
        }
        return n < 10_000_000 ? String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0) : n / 1_000_000 + "M";
    }
}
