package dev.dtp.beacon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Beacon-Effekt Stallmeister (Klasse B, docs/21-beacon-effekte.md), Teil für die Zucht: kürzere Zuchtpause für alle
 * züchtbaren Tiere und bessere Vererbung bei Pferden. Reittempo, Fallschutz und Straßenbonus stehen in
 * {@link BuilderEffects}. Alles hängt an Events, die ohnehin feuern — es wird nie über Tiere im Radius iteriert, nur
 * geprüft, ob das Ereignis im Radius eines Haupt- oder Neben-Beacons liegt ({@link BeaconLogic#effectsAt}).
 *
 * <p>Frosch, Schnüffler und Schildkröte werfen kein Junges und lösen deshalb kein {@link BabyEntitySpawnEvent} aus; sie
 * setzen die Pause beim Züchten aber genauso auf 6000. Für sie prüft {@link #onEntityTick} das frisch gesetzte Alter.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class AnimalHusbandry {
    /** Vanilla setzt Eltern nach dem Züchten auf diese Pause ({@code Animal.finalizeSpawnChildFromBreeding}). */
    public static final int VANILLA_BREEDING_COOLDOWN = 6000;

    // Obergrenzen der Pferdewerte wie in AbstractHorse (dort protected): generateMaxHealth, generateJumpStrength
    // und generateSpeed mit dem jeweils höchsten Wurf. Vanilla klemmt auch die Elternwerte auf diese Spanne.
    static final double MAX_HORSE_HEALTH = 15.0 + 7 + 8;
    static final double MAX_HORSE_JUMP = 0.4 + 0.2 * 3;
    static final double MAX_HORSE_SPEED = (0.45 + 0.3 * 3) * 0.25;

    /**
     * Eltern, deren Zuchtpause am Ende dieses Ticks verkürzt wird. Das Event feuert vor {@code setAge(6000)};
     * erst danach lässt sich die Pause überschreiben.
     */
    private static final List<Animal> PENDING = new ArrayList<>();

    private AnimalHusbandry() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onBabySpawn(BabyEntitySpawnEvent event) {
        AgeableMob child = event.getChild();
        Mob parent = event.getParentA();
        if (event.isCanceled() || child == null || !(parent.level() instanceof ServerLevel level)
                || !DtpConfig.EFFECTS_ENABLED.get()) {
            return;
        }
        Set<ServerUpgrade> effects = BeaconLogic.effectsAt(level.getServer(), level.dimension(), parent.blockPosition());
        if (effects.contains(ServerUpgrade.EFFECT_STABLEMASTER)) {
            if (parent instanceof Animal a) {
                PENDING.add(a);
            }
            if (event.getParentB() instanceof Animal b) {
                PENDING.add(b);
            }
        }
        // Kamele erben keine Werte; Esel, Maultiere und Lamas teilen sich mit Pferden die Würfelregel.
        if (effects.contains(ServerUpgrade.EFFECT_STABLEMASTER)
                && (child instanceof Horse || child instanceof AbstractChestedHorse)) {
            improveFoal((AbstractHorse) child, DtpConfig.HORSE_INHERITANCE_BONUS.get() / 100.0);
        }
    }

    /**
     * Ei-Leger (Frosch, Schnüffler, Schildkröte): Vanilla setzt beim Züchten das Alter auf 6000, ohne Event. Das Alter
     * steht nur im Tick des Züchtens (oder direkt danach) so hoch; Jungtiere haben ein negatives Alter. Erst die billigen
     * Prüfungen, dann der Radius.
     */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || !(animal instanceof Frog || animal instanceof Turtle || animal instanceof Sniffer)
                || animal.getAge() < VANILLA_BREEDING_COOLDOWN - 1 || !(animal.level() instanceof ServerLevel level)
                || !DtpConfig.EFFECTS_ENABLED.get()) {
            return;
        }
        int cooldown = breedingCooldown(WorldProgress.get(level.getServer()));
        if (animal.getAge() > cooldown && BeaconLogic.effectAt(level, animal.blockPosition(), ServerUpgrade.EFFECT_STABLEMASTER)) {
            animal.setAge(cooldown);
        }
    }

    /** Am Ende jedes Server-Ticks; tut nur etwas, wenn in diesem Tick im Radius gezüchtet wurde. */
    public static void flushPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int cooldown = breedingCooldown(WorldProgress.get(server));
        for (Animal animal : PENDING) {
            // Nur kürzen: Ein Wert, den eine andere Mod schon gesenkt hat, bleibt stehen.
            if (animal.isAlive() && animal.getAge() > cooldown) {
                animal.setAge(cooldown);
            }
        }
        PENDING.clear();
    }

    /**
     * Zuchtpause im Radius des Effekts. Einzige Stelle für alle Quellen: Ein späteres Server-Upgrade
     * „Geringere Zucht-Abklingzeit“ addiert hier seinen Abzug, unter einer gemeinsamen Untergrenze
     * (Klasse-B-Regel: eine Obergrenze je Wirkung über alle Quellen, docs/62-balancing.md).
     */
    static int breedingCooldown(WorldProgress progress) {
        return Math.min(VANILLA_BREEDING_COOLDOWN, DtpConfig.BREEDING_COOLDOWN_TICKS.get());
    }

    /**
     * Rückt Gesundheit, Sprungkraft und Tempo des Fohlens um {@code bonus} der Lücke zum Vanilla-Maximum nach
     * oben. Wirkt nach Vanillas Wurf (Mittel der Eltern plus Zufall): gute Fohlen werden etwas besser, Top-Werte
     * bleiben Zuchtarbeit, das Maximum wird nie überschritten.
     */
    public static void improveFoal(AbstractHorse foal, double bonus) {
        raise(foal, Attributes.MAX_HEALTH, MAX_HORSE_HEALTH, bonus);
        raise(foal, Attributes.JUMP_STRENGTH, MAX_HORSE_JUMP, bonus);
        raise(foal, Attributes.MOVEMENT_SPEED, MAX_HORSE_SPEED, bonus);
        foal.setHealth(foal.getMaxHealth());
    }

    private static void raise(AbstractHorse foal, Holder<Attribute> attribute, double max, double bonus) {
        AttributeInstance instance = foal.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        double base = instance.getBaseValue();
        if (base < max) {
            instance.setBaseValue(base + bonus * (max - base));
        }
    }
}
