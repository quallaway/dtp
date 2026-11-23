package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.dtp.upgrade.PlayerUpgrade;

/**
 * Slot-Auswahl für Hauptbeacon, Neben-Beacon und die Module der Werkstatt: oben die Slots mit den aktiven Einträgen,
 * darunter alle als Symbole. Ein Klick auf ein Symbol schaltet um. Daten aus {@code EffectSelection} im Menü bzw.
 * im selben Format aus der Werkstatt.
 *
 * <p>Optionale Schlüssel für andere Verwendungen als den Beacon: je Eintrag {@code icon} (Item statt Effekt-Sprite)
 * und {@code name} (Übersetzungsschlüssel), im Datenstand {@code label_active}, {@code label_pick} und
 * {@code label_locked} für die Beschriftungen.
 */
final class EffectPicker {
    private static final int BOX = 22;
    private static final int STEP = 26;
    private static final int PER_ROW = 9;
    private static final int LOCKED = 0xFF404040;
    private static final int INNER = 0xFF202020;
    /** {@code INNER} mit Alpha 0.7 über einem Item — ergibt dasselbe Bild wie ein Sprite mit Alpha 0.3 auf {@code INNER}. */
    private static final int DIM_OVERLAY = 0xB3202020;

    private EffectPicker() {
    }

    private static int slotTop(int top) {
        return top + 12;
    }

    private static int gridTop(CompoundTag d, int top) {
        int slotRows = Math.max(1, (d.getIntOr("effect_slots", 0) + PER_ROW - 1) / PER_ROW);
        return slotTop(top) + slotRows * STEP + 16;
    }

    private static int boxX(int x, int i) {
        return x + (i % PER_ROW) * STEP;
    }

    private static int boxY(int rowTop, int i) {
        return rowTop + (i / PER_ROW) * STEP;
    }

    private static boolean inside(int bx, int by, int mx, int my) {
        return mx >= bx && mx < bx + BOX && my >= by && my < by + BOX;
    }

    /** Ordinal des angeklickten Effekts oder -1. Aktive Effekte lassen sich auch in ihrem Slot abschalten. */
    static int clicked(CompoundTag d, int x, int top, int mx, int my) {
        ListTag active = d.getListOrEmpty("active_effects");
        for (int i = 0; i < active.size(); i++) {
            if (inside(boxX(x, i), boxY(slotTop(top), i), mx, my)) {
                return active.getCompoundOrEmpty(i).getIntOr("ordinal", -1);
            }
        }
        ListTag all = d.getListOrEmpty("effects");
        int gridTop = gridTop(d, top);
        for (int i = 0; i < all.size(); i++) {
            if (inside(boxX(x, i), boxY(gridTop, i), mx, my)) {
                return all.getCompoundOrEmpty(i).getIntOr("ordinal", -1);
            }
        }
        return -1;
    }

    static void draw(GuiGraphicsExtractor g, Font font, CompoundTag d, int x, int top, int mouseX, int mouseY) {
        int slots = d.getIntOr("effect_slots", 0);
        ListTag active = d.getListOrEmpty("active_effects");
        g.text(font, Component.translatable(d.getStringOr("label_active", "dtp.effects.active"), active.size(), slots), x, top, Gui.TEXT, false);
        CompoundTag hovered = null;
        for (int i = 0; i < slots; i++) {
            int bx = boxX(x, i);
            int by = boxY(slotTop(top), i);
            if (i < active.size()) {
                CompoundTag e = active.getCompoundOrEmpty(i);
                box(g, e, bx, by, Gui.GOLD, false);
                if (inside(bx, by, mouseX, mouseY)) {
                    hovered = e;
                }
            } else {
                g.fill(bx, by, bx + BOX, by + BOX, LOCKED);
                g.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, INNER);
            }
        }

        ListTag all = d.getListOrEmpty("effects");
        int gridTop = gridTop(d, top);
        g.text(font, Component.translatable(d.getStringOr("label_pick", "dtp.effects.pick")), x, gridTop - 12, Gui.MUTED, false);
        for (int i = 0; i < all.size(); i++) {
            CompoundTag e = all.getCompoundOrEmpty(i);
            int bx = boxX(x, i);
            int by = boxY(gridTop, i);
            boolean unlocked = e.getBooleanOr("unlocked", false);
            int frame = e.getBooleanOr("active", false) ? Gui.GOLD : unlocked ? Gui.BORDER : LOCKED;
            box(g, e, bx, by, frame, !unlocked);
            if (inside(bx, by, mouseX, mouseY)) {
                hovered = e;
            }
        }
        if (hovered != null) {
            g.setComponentTooltipForNextFrame(font, tooltip(d, hovered), mouseX, mouseY);
        }
    }

    private static void box(GuiGraphicsExtractor g, CompoundTag e, int bx, int by, int frame, boolean dim) {
        g.fill(bx, by, bx + BOX, by + BOX, frame);
        g.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, INNER);
        if (e.contains("icon") || e.contains("icon_sprite")) {
            boolean item = Gui.icon(g, e, bx, by, BOX, dim ? 0.3f : 1.0f);
            if (dim && item) {
                // Items lassen sich nicht mit Alpha zeichnen; stattdessen abgedunkelt wie die gesperrten Effekt-Sprites.
                g.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, DIM_OVERLAY);
            }
            return;
        }
        Identifier effect = Identifier.tryParse(e.getStringOr("effect", ""));
        if (effect != null) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, effect.withPrefix("mob_effect/"), bx + 2, by + 2, 18, 18, dim ? 0.3f : 1.0f);
        }
    }

    private static List<Component> tooltip(CompoundTag d, CompoundTag e) {
        String key = e.getStringOr("name", "upgrade.dtp." + e.getStringOr("id", ""));
        Component name = Component.translatable(key).copy().withColor(Gui.GOLD);
        String hint = !e.getBooleanOr("unlocked", false) ? d.getStringOr("label_locked", "dtp.effects.locked")
                : e.getBooleanOr("active", false) ? "dtp.effects.click_off" : "dtp.effects.click_on";
        List<Component> lines = new ArrayList<>();
        lines.add(name);
        if (e.contains("name")) {
            // Module der Werkstatt: Beschreibung und, bei Skills, womit sie ausgelöst werden.
            lines.add(Component.translatable(key + ".desc"));
            PlayerUpgrade skill = PlayerUpgrade.byId(e.getStringOr("id", ""));
            if (skill != null && skill.isSkill()) {
                lines.add(Component.translatable("dtp.workshop.modules.trigger", SkillKeys.trigger(skill)).withColor(Gui.GOLD));
            }
        }
        lines.add(Component.translatable(hint).withColor(Gui.MUTED));
        return lines;
    }
}
