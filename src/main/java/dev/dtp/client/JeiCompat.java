package dev.dtp.client;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import dev.dtp.Dtp;

/**
 * JEI-Anbindung. Wird nur geladen, wenn JEI installiert ist.
 *
 * <p>Neben Einzahlkiste, Schwarzmarkt und Reparaturbank steht ein eigenes Panel. Dort zeigt JEI seine Itemliste in der schmalsten
 * Stufe (zwei Spalten) ganz rechts am Bildschirm. JEI legt die Liste in den Raum rechts vom rechten Rand der
 * Oberfläche ({@link IGuiProperties#guiRight()}); belegte Flächen ({@code getGuiExtraAreas}) sperren dagegen nur
 * einzelne Felder und machen die Liste nicht schmaler. Deshalb wird JEI ein rechter Rand gemeldet, hinter dem nur
 * noch Platz für zwei Spalten bleibt — mindestens aber das Ende des Panels.
 */
@JeiPlugin
public class JeiCompat implements IModPlugin {
    /** Zwei Spalten à 18 px plus JEIs Außenrand (2 × 6) und Hintergrundrand (2 × 7). Bei JEI 31.17 geprüft. */
    private static final int NARROW_LIST_WIDTH = 2 * 18 + 2 * 6 + 2 * 7;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Dtp.MODID, "side_panels");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(DepositChestScreen.class, JeiCompat::narrow);
        registration.addGuiScreenHandler(BlackmarketScreen.class, JeiCompat::narrow);
        registration.addGuiScreenHandler(RepairStationScreen.class, JeiCompat::narrow);
        // Falls die Liste auf sehr schmalen Bildschirmen doch an das Panel stößt, bleiben dessen Felder frei.
        registration.addGenericGuiContainerHandler(SidePanelChestScreen.class, new IGuiContainerHandler<SidePanelChestScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(SidePanelChestScreen<?> screen) {
                return List.of(screen.sidePanelArea());
            }
        });
        registration.addGenericGuiContainerHandler(TwoSlotScreen.class, new IGuiContainerHandler<TwoSlotScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(TwoSlotScreen<?> screen) {
                return List.of(screen.sidePanelArea());
            }
        });
    }

    private static <T extends Screen & SidePanelScreen> IGuiProperties narrow(T screen) {
        // JEI fragt schon vor init() ab; dann sind Breite und Höhe noch 0 und JEI würde die Werte als ungültig loggen.
        if (screen.width <= 1 || screen.height <= 1) {
            return null;
        }
        Rect2i main = screen.mainArea();
        Rect2i panel = screen.sidePanelArea();
        int right = Math.max(panel.getX() + panel.getWidth(), screen.width - NARROW_LIST_WIDTH);
        return new Properties(screen.getClass(), main.getX(), main.getY(), right - main.getX(), main.getHeight(), screen.width, screen.height);
    }

    private record Properties(Class<? extends Screen> screenClass, int guiLeft, int guiTop, int guiXSize, int guiYSize,
            int screenWidth, int screenHeight) implements IGuiProperties {
    }
}
