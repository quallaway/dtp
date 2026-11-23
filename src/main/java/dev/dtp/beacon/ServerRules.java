package dev.dtp.beacon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

import dev.dtp.Dtp;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Serverweite Regeln aus Beacon-Upgrades, ohne Radius (docs/20-territorium-und-serverweite-effekte.md).
 *
 * <p>Endermänner und Creeper fragen vor jedem Eingriff in Blöcke {@code EventHooks.canEntityGrief}: Endermänner
 * beim Aufheben und Absetzen, Creeper über {@code ExplosionInteraction.MOB} — abgelehnt explodieren sie mit
 * {@code BlockInteraction.KEEP}, verletzen also weiter, zerstören aber nichts. Gefiltert wird nur auf diese beiden
 * Mobs; die Gamerule {@code mobGriefing}, TNT und Ghast-Feuerbälle bleiben unberührt.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ServerRules {
    private ServerRules() {
    }

    @SubscribeEvent
    static void onGrief(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        ServerUpgrade rule = entity instanceof Enderman ? ServerUpgrade.ENDERMAN_GRIEFING
                : entity instanceof Creeper ? ServerUpgrade.CREEPER_GRIEFING : null;
        if (rule != null && entity.level() instanceof ServerLevel level
                && WorldProgress.get(level.getServer()).level(rule) > 0) {
            event.setCanGrief(false);
        }
    }
}
