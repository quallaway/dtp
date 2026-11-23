package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.dtp.Dtp;

/**
 * Aktive Skills der persönlichen Module: höchstens einer ist ausgerüstet und wird über die eine Fähigkeitstaste
 * ausgelöst (der Doppelsprung auch über die Sprungtaste in der Luft), mit einer gemeinsamen Abklingzeit als einzigen
 * Kosten. Der Client meldet nur den Tastendruck ({@link Payload}); alle Prüfungen und Wirkungen laufen hier
 * auf dem Server. Werte je Stufe stehen in {@code params} der Upgrade-Definition
 * (docs/31-module-und-faehigkeiten.md).
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class Skills {
    /** Wer in der Luft schon doppelt gesprungen ist. Geleert, sobald der Spieler den Boden berührt. Nicht gespeichert. */
    private static final Set<UUID> AIR_JUMPED = new HashSet<>();

    private Skills() {
    }

    public sealed interface Result permits Used, NotEquipped, Cooldown, Invalid {
    }

    public record Used() implements Result {
    }

    public record NotEquipped() implements Result {
    }

    public record Cooldown(long ticks) implements Result {
    }

    /** Kein Skill, nicht konfiguriert oder gerade nicht möglich (Doppelsprung am Boden, Zuschauer …). */
    public record Invalid() implements Result {
    }

    /** Löst den ausgerüsteten Skill aus — das, was die Fähigkeitstaste tut. */
    public static Result activateEquipped(Player player) {
        PlayerUpgrade skill = PlayerUpgrades.of(player).equippedSkill();
        return skill == null ? new NotEquipped() : activate(player, skill);
    }

    /**
     * Löst einen Skill aus. Die einzigen Kosten sind die Abklingzeit, und sie fällt nur bei Erfolg an. Bewusst kein
     * Hunger: Sollen Skills einmal etwas kosten, dann Taler.
     */
    public static Result activate(Player player, PlayerUpgrade skill) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(skill);
        if (!skill.isSkill() || def == null || !player.isAlive() || player.isSpectator()) {
            return new Invalid();
        }
        int level = PlayerUpgrades.level(player, skill);
        if (level == 0) {
            return new NotEquipped();
        }
        long rest = ModuleCooldowns.remaining(player, skill);
        if (rest > 0) {
            return new Cooldown(rest);
        }
        if (!apply(player, skill, def, level)) {
            return new Invalid();
        }
        // Ohne Abklingzeit (Doppelsprung: sperrt nur bis zur Landung) gar nicht erst starten — das spart den Sync und
        // ließe sonst einen noch laufenden gemeinsamen Skill-Cooldown verfallen.
        double cooldown = def.param("cooldown", level, 0);
        if (cooldown > 0) {
            ModuleCooldowns.start(player, skill, cooldown);
        }
        return new Used();
    }

    private static boolean apply(Player player, PlayerUpgrade skill, PlayerUpgradeDefinitions.Definition def, int level) {
        return switch (skill) {
            case DASH -> dash(player, def, level);
            case DOUBLE_JUMP -> doubleJump(player, def, level);
            case GUARD -> {
                player.addEffect(effect(MobEffects.RESISTANCE, def, "resistance", level));
                yield true;
            }
            case HEAL_PULSE -> healPulse(player, def, level);
            case FISHING_NET -> FishingNet.cast(player, def.param("radius", level, 3));
            default -> false;
        };
    }

    /**
     * Statuseffekt aus den Parametern: {@code <name>} ist die Effektstufe wie im Spiel angezeigt (Resistenz 2 = 2),
     * {@code duration} die Dauer in Sekunden.
     */
    static MobEffectInstance effect(Holder<MobEffect> type,
            PlayerUpgradeDefinitions.Definition def, String name, int level) {
        int ticks = (int) Math.round(def.param("duration", level, 5) * 20);
        int amplifier = Math.max(0, (int) def.param(name, level, 1) - 1);
        return new MobEffectInstance(type, ticks, amplifier, false, true);
    }

    /**
     * Schub waagerecht in Blickrichtung mit einem kleinen Anheben, damit die Bodenreibung ihn nicht sofort schluckt.
     * {@code syncVelocity} schickt die neue Bewegung auch an den eigenen Client — der Spieler bewegt sich ja selbst
     * ({@code needsSync} erreicht nur die anderen Spieler; Vanilla-Knockback nutzt dasselbe Flag).
     */
    private static boolean dash(Player player, PlayerUpgradeDefinitions.Definition def, int level) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-4) {
            return false;
        }
        Vec3 push = flat.normalize().scale(def.param("strength", level, 1.0));
        double lift = Math.max(player.getDeltaMovement().y, def.param("lift", level, 0.25));
        player.setDeltaMovement(push.x, lift, push.z);
        player.syncVelocity = true;
        player.resetFallDistance();
        return true;
    }

    /**
     * Ein zusätzlicher Sprung in der Luft, einmal bis zur nächsten Landung. Nicht beim Fliegen, Schwimmen, Klettern.
     * Kein Fallschutz: Wer kurz vor dem Aufprall springt, bremst den Fall ohnehin selbst ab.
     */
    private static boolean doubleJump(Player player, PlayerUpgradeDefinitions.Definition def, int level) {
        if (player.onGround() || player.getAbilities().flying || player.isInWater() || player.onClimbable()
                || player.isPassenger() || player.isFallFlying() || AIR_JUMPED.contains(player.getUUID())) {
            return false;
        }
        double strength = def.param("strength", level, 0.6);
        airJump(player, strength);
        // Kein syncVelocity: Das schickte den ganzen Vektor, also auch die waagerechte Bewegung der Server-Kopie, die
        // beim Spieler veraltet ist und etwa den Sprint-Schwung nicht kennt — der zweite Sprung bremste dadurch.
        // Der Client setzt nur die Höhe auf seine echte Bewegung.
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new JumpPayload(strength));
        }
        AIR_JUMPED.add(player.getUUID());
        return true;
    }

    /** Schub nach vorn beim Sprinten, wie ihn der Vanilla-Sprung gibt ({@code LivingEntity.jumpFromGround}). */
    private static final double SPRINT_BOOST = 0.2;

    /**
     * Der eigentliche Luftsprung: neue senkrechte Geschwindigkeit, die waagerechte bleibt erhalten. Beim Sprinten kommt
     * wie beim Vanilla-Sprung ein Schub in Blickrichtung dazu — ohne ihn baut die längere Flugzeit den Sprint-Schwung
     * ab (in der Luft je Tick ×0.91, nur +0.026 Nachschub), und der Doppelsprung fühlt sich wie ein Bremsen an.
     */
    private static void airJump(Player player, double y) {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, y, motion.z);
        if (player.isSprinting()) {
            float angle = player.getYRot() * Mth.DEG_TO_RAD;
            player.addDeltaMovement(new Vec3(-Mth.sin(angle) * SPRINT_BOOST, 0, Mth.cos(angle) * SPRINT_BOOST));
        }
    }

    /** Regeneration für den Auslöser und alle Spieler im Radius. */
    private static boolean healPulse(Player player, PlayerUpgradeDefinitions.Definition def, int level) {
        List<Player> nearby = new ArrayList<>(player.level().players());
        for (Player target : healTargets(player, nearby, def.param("radius", level, 6))) {
            target.addEffect(effect(MobEffects.REGENERATION, def, "regeneration", level));
        }
        return true;
    }

    /** Der Auslöser und alle lebenden Spieler im Radius (gleiche Dimension). */
    public static List<Player> healTargets(Player self, List<? extends Player> candidates, double radius) {
        List<Player> targets = new ArrayList<>();
        targets.add(self);
        for (Player other : candidates) {
            if (other != self && other.isAlive() && !other.isSpectator() && other.level() == self.level()
                    && other.distanceToSqr(self) <= radius * radius) {
                targets.add(other);
            }
        }
        return targets;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.onGround()) {
            AIR_JUMPED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AIR_JUMPED.remove(event.getEntity().getUUID());
    }

    /** Tastendruck vom Client: Fähigkeitstaste bzw. Sprung in der Luft — welcher Skill, weiß der Server selbst. */
    public record Payload() implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "skill"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.unit(new Payload());

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // Der Doppelsprung meldet sich bei jedem Sprung in der Luft — Ablehnungen bleiben dort stumm.
            boolean quiet = PlayerUpgrades.of(player).equippedSkill() == PlayerUpgrade.DOUBLE_JUMP;
            switch (activateEquipped(player)) {
                case Cooldown c when !quiet ->
                        player.sendOverlayMessage(Component.translatable("dtp.skill.cooldown", (c.ticks() + 19) / 20));
                default -> {
                }
            }
        }
    }

    /** Antwort an den Client auf einen gültigen Doppelsprung: die senkrechte Geschwindigkeit, die er übernimmt. */
    public record JumpPayload(double strength) implements CustomPacketPayload {
        public static final Type<JumpPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "double_jump"));
        public static final StreamCodec<ByteBuf, JumpPayload> STREAM_CODEC =
                ByteBufCodecs.DOUBLE.map(JumpPayload::new, JumpPayload::strength);

        @Override
        public Type<JumpPayload> type() {
            return TYPE;
        }

        public static void handle(JumpPayload payload, IPayloadContext context) {
            airJump(context.player(), payload.strength());
        }
    }
}
