package dev.dtp.client;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import dev.dtp.Dtp;
import dev.dtp.upgrade.ModuleCooldowns;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.UpgradeIcon;

/**
 * Anzeige unten links: je ausgerüstetes Modul mit Abklingzeit (der eine Skill, Barriere) ein Symbol. Goldener Rahmen = bereit,
 * grauer Rahmen mit Restsekunden darüber = Abklingzeit läuft. Ohne solche Module wird nichts gezeichnet.
 */
@EventBusSubscriber(modid = Dtp.MODID, value = Dist.CLIENT)
public final class ModuleHud {
    private static final int BOX = 20;
    private static final int STEP = 22;
    private static final int COOLING = 0xFF505050;
    private static final int INNER = 0xC0202020;

    /**
     * Symbole für die Anzeige. Die Symbole im Werkstatt-Baum kommen aus dem Datapack, das auf einem dedizierten Server
     * nur der Server kennt — daher hier fest, passend zu den Standard-Dateien in data/dtp/dtp/player_upgrade.
     */
    private static final Map<PlayerUpgrade, UpgradeIcon> ICONS = new EnumMap<>(Map.of(
            PlayerUpgrade.BARRIER, UpgradeIcon.sprite(Identifier.withDefaultNamespace("mob_effect/absorption")),
            PlayerUpgrade.DASH, UpgradeIcon.sprite(Identifier.fromNamespaceAndPath(Dtp.MODID, "upgrade/dash")),
            PlayerUpgrade.DOUBLE_JUMP, UpgradeIcon.sprite(Identifier.fromNamespaceAndPath(Dtp.MODID, "upgrade/double_jump")),
            PlayerUpgrade.GUARD, UpgradeIcon.sprite(Identifier.withDefaultNamespace("mob_effect/resistance")),
            PlayerUpgrade.HEAL_PULSE, UpgradeIcon.sprite(Identifier.fromNamespaceAndPath(Dtp.MODID, "upgrade/heal_pulse")),
            PlayerUpgrade.FISHING_NET, UpgradeIcon.sprite(Identifier.fromNamespaceAndPath(Dtp.MODID, "upgrade/fishing_net"))));

    private ModuleHud() {
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(Dtp.MODID, "modules"), ModuleHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        int x = 4;
        int y = g.guiHeight() - BOX - 4;
        for (PlayerUpgrade module : PlayerUpgrades.of(player).equippedModules()) {
            UpgradeIcon icon = ICONS.get(module);
            if (icon == null) {
                continue;
            }
            long rest = ModuleCooldowns.remaining(player, module);
            g.fill(x, y, x + BOX, y + BOX, rest == 0 ? Gui.GOLD : COOLING);
            g.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, INNER);
            Gui.icon(g, icon, x, y, BOX, 1.0f);
            if (rest > 0) {
                String seconds = String.valueOf((rest + 19) / 20);
                g.text(mc.font, seconds, x + (BOX - mc.font.width(seconds)) / 2, y - 9, Gui.TEXT, true);
            }
            x += STEP;
        }
    }
}
