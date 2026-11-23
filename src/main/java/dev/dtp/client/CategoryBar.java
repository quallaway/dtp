package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Gruppenknöpfe über einer Warenliste: „∗“ für alle, danach je freigeschalteter Gruppe ein Knopf mit Symbol.
 * Gebraucht von Marktstand und Statistik-Tafel; die Gruppen kommen aus {@code MarketCategories.snapshot} im Feld
 * {@code categories}. Gefiltert wird auf dem Client über den Item-Tag der Gruppe.
 *
 * <p>So viele Reihen wie nötig, gleichmäßig gefüllt — bei 12 Knöpfen also 6 + 6 statt 9 + 3.
 */
public final class CategoryBar {
    public static final int STEP = 19;

    /** Legt einen Knopf an, der nur die Ansicht ändert und nichts an den Server schickt. */
    @FunctionalInterface
    public interface Buttons {
        Button add(Component label, int x, int y, int width, boolean active, Runnable action);
    }

    private final int left;
    private final int top;
    private final int maxPerRow;
    /**
     * ID der gewählten Gruppe, leer = alle Gruppen. Bewusst nicht der Listenplatz: Wird eine Gruppe freigeschaltet,
     * während die Oberfläche offen ist, verschieben sich die Plätze.
     */
    private String selected = "";

    public CategoryBar(int left, int top, int width) {
        this.left = left;
        this.top = top;
        this.maxPerRow = (width + 1) / STEP;
    }

    /** Legt die Knöpfe an; {@code onSelect} läuft nach jedem Wechsel der Gruppe, vor dem Neuaufbau. */
    public void init(CompoundTag d, Buttons buttons, Runnable onSelect) {
        ListTag categories = d.getListOrEmpty("categories");
        if (selectedIndex(categories) < 0) {
            selected = "";
        }
        Button all = buttons.add(Component.literal("∗"), x(d, 0), y(d, 0), 18, !selected.isEmpty(), () -> {
            selected = "";
            onSelect.run();
        });
        all.setTooltip(Tooltip.create(Component.translatable("dtp.market.all_categories")));
        for (int i = 0; i < categories.size(); i++) {
            String id = categories.getCompoundOrEmpty(i).getStringOr("id", "");
            Button b = buttons.add(Component.empty(), x(d, i + 1), y(d, i + 1), 18, !selected.equals(id), () -> {
                selected = id;
                onSelect.run();
            });
            b.setTooltip(Tooltip.create(name(categories.getCompoundOrEmpty(i))));
        }
    }

    /** Die Gruppensymbole auf den Knöpfen; Knöpfe zeichnen keine Items. */
    public void icons(GuiGraphicsExtractor g, CompoundTag d) {
        ListTag categories = d.getListOrEmpty("categories");
        for (int i = 0; i < categories.size(); i++) {
            g.item(Gui.stack(categories.getCompoundOrEmpty(i).getStringOr("icon", "")), x(d, i + 1) + 1, y(d, i + 1) + 1);
        }
    }

    /** Item-Tag der gewählten Gruppe, oder null für alle. */
    public @Nullable TagKey<Item> tag(CompoundTag d) {
        ListTag categories = d.getListOrEmpty("categories");
        int index = selectedIndex(categories);
        if (index < 0) {
            return null;
        }
        Identifier id = Identifier.tryParse(categories.getCompoundOrEmpty(index).getStringOr("tag", ""));
        return id == null ? null : TagKey.create(Registries.ITEM, id);
    }

    /** Ob das Item in der gewählten Gruppe liegt; ohne Auswahl immer. */
    public static boolean matches(@Nullable TagKey<Item> tag, ItemStack stack) {
        return tag == null || stack.is(tag);
    }

    /** Zusätzliche Höhe für jede Reihe über der ersten; schiebt die Liste darunter nach unten. */
    public int extraHeight(CompoundTag d) {
        return (rows(d) - 1) * STEP;
    }

    /** Zahl der Knöpfe einschließlich „Alle“. */
    private static int buttons(CompoundTag d) {
        return d.getListOrEmpty("categories").size() + 1;
    }

    private int rows(CompoundTag d) {
        return (buttons(d) + maxPerRow - 1) / maxPerRow;
    }

    private int perRow(CompoundTag d) {
        int rows = rows(d);
        return (buttons(d) + rows - 1) / rows;
    }

    /** @param slot 0 = „Alle“, danach die Gruppen */
    private int x(CompoundTag d, int slot) {
        return left + slot % perRow(d) * STEP;
    }

    private int y(CompoundTag d, int slot) {
        return top + slot / perRow(d) * STEP;
    }

    /** Listenplatz der gewählten Gruppe in der gesendeten Liste, -1 = alle oder nicht (mehr) vorhanden. */
    private int selectedIndex(ListTag categories) {
        for (int i = 0; !selected.isEmpty() && i < categories.size(); i++) {
            if (selected.equals(categories.getCompoundOrEmpty(i).getStringOr("id", ""))) {
                return i;
            }
        }
        return -1;
    }

    private static Component name(CompoundTag c) {
        Identifier id = Identifier.tryParse(c.getStringOr("id", ""));
        return id == null ? Component.empty() : Component.translatableWithFallback("dtp.category." + id.getPath(), id.getPath());
    }
}
