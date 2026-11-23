package dev.dtp.beacon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Beacon-Effekte mit Attribut- und Fallwirkung (docs/21-beacon-effekte.md): Baumeister-Reichweite, Federfall, Ruhige
 * Nächte und der Reittier-Teil des Stallmeisters (Reittempo, Fallschutz, Straßenbonus, Ruhe nach dem Absteigen). Attribut-Wirkungen laufen alle
 * 10 Ticks über die Spieler (nie über Blöcke oder Tiere im Radius); alles andere hängt an Events, die ohnehin feuern.
 * Der Rückbau des Baumeisters steht in {@link Rebuild}, die Zucht des Stallmeisters in {@link AnimalHusbandry}.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class BuilderEffects {
    /** Blöcke, auf denen der Straßenbonus des Stallmeisters wirkt. Standard: Trampelpfad, Kies, Grobe Erde, Wurzelerde; per Datapack änderbar. */
    public static final TagKey<Block> ROAD = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Dtp.MODID, "road"));
    /** Reittiere des Stallmeisters: Tempo beim Lenken, kein Fallschaden. Per Datapack erweiterbar, etwa um Mod-Reittiere. */
    public static final TagKey<EntityType<?>> MOUNTS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Dtp.MODID, "mounts"));

    public static final Identifier BUILDER_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "beacon_builder");
    public static final Identifier ROAD_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "beacon_road");
    public static final Identifier MOUNT_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "beacon_mount");
    public static final Identifier REST_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "beacon_mount_rest");

    /** Reittiere, die gerade einen Modifier tragen — damit er nach dem Absteigen wieder verschwindet. */
    private static final Set<LivingEntity> RIDDEN = Collections.newSetFromMap(new WeakHashMap<>());
    /** Reittiere in der Ruhe nach dem Absteigen, mit dem Gametick, an dem sie endet. */
    private static final Map<LivingEntity, Long> RESTING = new WeakHashMap<>();

    private BuilderEffects() {
    }

    /** Alle 10 Ticks aus {@code GameEvents}: setzt oder entfernt die Modifier aller Spieler und ihrer Reittiere. */
    public static void update(MinecraftServer server) {
        Set<LivingEntity> ridden = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player, ridden);
        }
        for (LivingEntity mount : Set.copyOf(RIDDEN)) {
            if (!ridden.contains(mount)) {
                Holder<Attribute> speed = speedAttribute(mount);
                setModifier(mount, speed, ROAD_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                setModifier(mount, speed, MOUNT_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                RIDDEN.remove(mount);
            }
        }
        for (Map.Entry<LivingEntity, Long> rest : Map.copyOf(RESTING).entrySet()) {
            LivingEntity mount = rest.getKey();
            if (!mount.isAlive() || mount.isVehicle() || mount.level().getGameTime() >= rest.getValue()
                    || mount instanceof Leashable leashable && leashable.isLeashed()) {
                endRest(mount);
            }
        }
    }

    /**
     * Stallmeister (Ruhe): Steigt ein Spieler im Radius von einem Reittier ab, bleibt es eine Weile stehen, damit man es
     * in Ruhe anbinden kann. Tempo −100 % auf Lauf- und Fluggeschwindigkeit; die Ruhe endet nach der Zeit, beim Anleinen
     * und beim Aufsteigen. Der Modifier ist flüchtig, ein Neuladen beendet sie also auch.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntityBeingMounted() instanceof LivingEntity mount) || !isMount(mount)) {
            return;
        }
        if (event.isMounting()) {
            endRest(mount);
            return;
        }
        int seconds = DtpConfig.MOUNT_REST_SECONDS.get();
        if (seconds > 0 && event.getEntityMounting() instanceof Player
                && BeaconLogic.effectAt(mount.level(), mount.blockPosition(), ServerUpgrade.EFFECT_STABLEMASTER)) {
            setModifier(mount, Attributes.MOVEMENT_SPEED, REST_ID, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            setModifier(mount, Attributes.FLYING_SPEED, REST_ID, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            RESTING.put(mount, mount.level().getGameTime() + seconds * 20L);
        }
    }

    /** Beendet die Ruhe eines Reittiers: Es läuft wieder normal. */
    public static void endRest(LivingEntity mount) {
        setModifier(mount, Attributes.MOVEMENT_SPEED, REST_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(mount, Attributes.FLYING_SPEED, REST_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        RESTING.remove(mount);
    }

    /** Ein Spieler: außerhalb der Oberwelt oder des Radius werden die Modifier entfernt (Betrag 0). */
    public static void update(Player player, Set<LivingEntity> ridden) {
        Level level = player.level();
        boolean builder = !player.isSpectator() && BeaconLogic.effectAt(level, player.blockPosition(), ServerUpgrade.EFFECT_BUILDER);
        setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BUILDER_ID, builder ? DtpConfig.BUILDER_REACH.get() : 0,
                AttributeModifier.Operation.ADD_VALUE);

        double road = DtpConfig.ROAD_SPEED_BONUS.get() / 100.0;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity mount && isMount(mount) && mount.getControllingPassenger() == player) {
            // Beim Reiten zählt der Boden unter dem Tier; der Spieler selbst bekommt nichts.
            setModifier(player, Attributes.MOVEMENT_SPEED, ROAD_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            Holder<Attribute> speed = speedAttribute(mount);
            boolean stable = BeaconLogic.effectAt(level, mount.blockPosition(), ServerUpgrade.EFFECT_STABLEMASTER);
            setModifier(mount, speed, ROAD_ID, onRoad(mount) ? road : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            setModifier(mount, speed, MOUNT_ID, stable ? DtpConfig.MOUNT_SPEED_BONUS.get() / 100.0 : 0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            RIDDEN.add(mount);
            ridden.add(mount);
        } else {
            setModifier(player, Attributes.MOVEMENT_SPEED, ROAD_ID, vehicle == null && onRoad(player) ? road : 0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
    }

    /** Steht das Wesen auf einem Straßenblock, an dem der Stallmeister wirkt? Trampelpfad ist niedriger: getOnPos trifft ihn selbst. */
    public static boolean onRoad(LivingEntity entity) {
        BlockPos on = entity.getOnPos();
        return entity.level().getBlockState(on).is(ROAD) && BeaconLogic.effectAt(entity.level(), on, ServerUpgrade.EFFECT_STABLEMASTER);
    }

    /** Ein Reittier des Stallmeisters (Tag {@code dtp:mounts}). */
    public static boolean isMount(Entity entity) {
        return entity.is(MOUNTS);
    }

    /** Das Attribut, aus dem das Reittier sein Reittempo rechnet: der Glückliche Ghast fliegt ({@code FLYING_SPEED}), alle anderen laufen oder schwimmen. */
    public static Holder<Attribute> speedAttribute(LivingEntity mount) {
        return mount instanceof HappyGhast ? Attributes.FLYING_SPEED : Attributes.MOVEMENT_SPEED;
    }

    /** Setzt einen flüchtigen Modifier auf {@code amount}, bei 0 wird er entfernt. Ändert nur, wenn sich etwas ändert. */
    static void setModifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) {
                instance.removeModifier(id);
            }
        } else if (current == null || current.amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    /**
     * Federfall halbiert den Fallschaden von Spielern im Radius. Wer auf einem Reittier im Radius des Stallmeisters sitzt,
     * nimmt keinen: Das Reittier gibt seinen Fallschaden an die Reiter weiter, und der soll mit dem des Tiers entfallen.
     */
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (player.getVehicle() instanceof Entity mount && isMount(mount)
                && BeaconLogic.effectAt(player.level(), mount.blockPosition(), ServerUpgrade.EFFECT_STABLEMASTER)) {
            event.setCanceled(true);
            return;
        }
        if (BeaconLogic.effectAt(player.level(), player.blockPosition(), ServerUpgrade.EFFECT_FEATHER_FALL)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * DtpConfig.FALL_DAMAGE_PERCENT.get() / 100f);
        }
    }

    /**
     * Stallmeister: Reittiere im Radius nehmen keinen Fallschaden, auch unberitten. Pferde überschreiben
     * {@code causeFallDamage} ohne {@link LivingFallEvent}, deshalb greift das hier beim Schaden selbst.
     */
    @SubscribeEvent
    static void onMountDamage(LivingIncomingDamageEvent event) {
        LivingEntity mount = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FALL) && isMount(mount)
                && BeaconLogic.effectAt(mount.level(), mount.blockPosition(), ServerUpgrade.EFFECT_STABLEMASTER)) {
            event.setCanceled(true);
        }
    }

    /** Ruhige Nächte: Für Spieler im Radius erscheinen keine Phantome. */
    @SubscribeEvent
    static void onPhantoms(PlayerSpawnPhantomsEvent event) {
        Player player = event.getEntity();
        if (BeaconLogic.effectAt(player.level(), player.blockPosition(), ServerUpgrade.EFFECT_QUIET_NIGHTS)) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
        }
    }
}
