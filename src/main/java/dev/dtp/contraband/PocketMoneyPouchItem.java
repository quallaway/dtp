package dev.dtp.contraband;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import dev.dtp.Dtp;

/**
 * Taschengeld-Beutel: Dorfbewohner im Radius laufen für {@code seconds} zum Spieler.
 *
 * <p>Ein einmal gesetztes Laufziel reicht nicht, weil Vanilla es auf zwei Wegen verliert:
 * <ul>
 * <li>Ist der Dorfbewohner angekommen, löscht {@code MoveToTargetSink} das Ziel. Ohne Ziel startet sofort ein
 * Zufallsspaziergang ({@code VillageBoundRandomStroll}), und er läuft wieder weg.</li>
 * <li>Die Wege zu Arbeitsplatz und Treffpunkt ({@code StrollToPoi}) überschreiben ein gesetztes Ziel jederzeit.</li>
 * </ul>
 * Deshalb prüft {@link #onEntityTick} direkt nach dem Gehirn-Takt jedes angelockten Dorfbewohners, ob ein fremdes Ziel
 * gesetzt wurde: Weiter weg wird es durch den Spieler ersetzt, in der Nähe gelöscht — dann bleibt er stehen und schaut
 * den Spieler an. Das läuft nur für angelockte Dorfbewohner und nur für die Dauer der Wirkung; ohne laufende Wirkung
 * kostet der Handler einen Blick in eine leere Map.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public class PocketMoneyPouchItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(8, 1, 25.0, 800, false,
            Map.of("radius", 32.0, "seconds", 30.0));

    private static final float SPEED = 0.6F;
    /** Ab hier gilt ein Dorfbewohner als angekommen ({@code closeEnoughDist} des Laufziels). */
    private static final int CLOSE_ENOUGH = 2;
    /** Näher als das wird kein neues Ziel gesetzt, sondern jeder Spaziergang abgebrochen. */
    private static final double STAY_DISTANCE = 3.5;

    private record Lure(Player player, long until) {
    }

    /** Angelockte Dorfbewohner. Nur der Server-Thread greift darauf zu. */
    private static final Map<Villager, Lure> LURED = new IdentityHashMap<>();

    public PocketMoneyPouchItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    /** Dorfbewohner in der Kugel um {@code player}. */
    public static List<Villager> villagers(ServerLevel level, Player player, double radius) {
        return level.getEntitiesOfClass(Villager.class, new AABB(player.blockPosition()).inflate(radius),
                v -> v.isAlive() && !v.isBaby() && v.distanceToSqr(player) <= radius * radius);
    }

    /** Lockt {@code villagers} für {@code ticks} Ticks zu {@code player}. */
    public static void lure(List<Villager> villagers, Player player, int ticks) {
        for (Villager villager : villagers) {
            LURED.put(villager, new Lure(player, villager.level().getGameTime() + ticks));
            follow(villager, player);
        }
    }

    public static boolean isLured(Villager villager) {
        return LURED.containsKey(villager);
    }

    /** Ein Takt der Wirkung für einen Dorfbewohner; liefert {@code false}, wenn sie vorbei ist. */
    public static boolean enforce(Villager villager) {
        Lure lure = LURED.get(villager);
        if (lure == null) {
            return false;
        }
        Player player = lure.player;
        if (!villager.isAlive() || player.isRemoved() || player.level() != villager.level() || villager.level().getGameTime() >= lure.until) {
            LURED.remove(villager);
            return false;
        }
        follow(villager, player);
        return true;
    }

    /** Ersetzt ein fremdes Laufziel: weiter weg durch den Spieler, in der Nähe durch Stehenbleiben. */
    private static void follow(Villager villager, Player player) {
        Brain<Villager> brain = villager.getBrain();
        Optional<WalkTarget> target = brain.getMemory(MemoryModuleType.WALK_TARGET);
        boolean ours = target.isPresent() && target.get().getTarget() instanceof EntityTracker tracker && tracker.getEntity() == player;
        if (villager.distanceToSqr(player) > STAY_DISTANCE * STAY_DISTANCE) {
            if (!ours) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, SPEED, CLOSE_ENOUGH));
            }
        } else if (target.isPresent()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getNavigation().stop();
        }
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!LURED.isEmpty() && event.getEntity() instanceof Villager villager && !villager.level().isClientSide()) {
            enforce(villager);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        LURED.clear();
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        double radius = Math.clamp(def.param("radius", 32.0), 1.0, 96.0);
        int ticks = (int) Math.round(Math.clamp(def.param("seconds", 30.0), 1.0, 300.0) * 20.0);
        List<Villager> villagers = villagers(level, player, radius);
        if (villagers.isEmpty()) {
            return UseOutcome.refused("dtp.contraband.pocket_money_pouch.nobody");
        }
        for (Villager villager : villagers) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 2.0, villager.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
        }
        lure(villagers, player, ticks);
        player.sendOverlayMessage(Component.translatable("dtp.contraband.pocket_money_pouch.lured", villagers.size()).withStyle(ChatFormatting.GOLD));
        return UseOutcome.USED;
    }
}
