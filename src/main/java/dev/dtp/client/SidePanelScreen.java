package dev.dtp.client;

import net.minecraft.client.renderer.Rect2i;

/** Eine Oberfläche mit Panel rechts daneben; beide Flächen in Bildschirmkoordinaten, etwa für Rezept-Mods. */
public interface SidePanelScreen {
    /** Die eigentliche Oberfläche ohne Panel. */
    Rect2i mainArea();

    Rect2i sidePanelArea();
}
