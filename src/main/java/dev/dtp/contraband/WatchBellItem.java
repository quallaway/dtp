package dev.dtp.contraband;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Wachglocke: Monster im Radius leuchten (Vanilla-Effekt, für alle Spieler sichtbar), beim Schleichen stattdessen Tiere.
 * Ohne Ziel kostet sie nichts.
 */
public class WatchBellItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(16, 1, 10.0, 400, false,
            Map.of("radius", 32.0, "seconds", 15.0));

    public WatchBellItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        boolean animals = player.isShiftKeyDown();
        double radius = Math.clamp(def.param("radius", 32.0), 1.0, 128.0);
        int ticks = (int) Math.round(Math.clamp(def.param("seconds", 15.0), 1.0, 600.0) * 20.0);
        int lit = ring(level, player, radius, ticks, animals);
        if (lit == 0) {
            return UseOutcome.refused(animals ? "dtp.contraband.watch_bell.no_animals" : "dtp.contraband.watch_bell.no_monsters");
        }
        player.sendOverlayMessage(Component.translatable(animals ? "dtp.contraband.watch_bell.animals" : "dtp.contraband.watch_bell.monsters", lit)
                .withStyle(ChatFormatting.GOLD));
        return UseOutcome.USED;
    }

    /** Lässt Monster bzw. Tiere in der Kugel um {@code player} leuchten; liefert ihre Zahl. */
    public static int ring(ServerLevel level, Player player, double radius, int ticks, boolean animals) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius
                        && (animals ? e instanceof Animal : e instanceof Enemy));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), player);
        }
        return targets.size();
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.BELL_BLOCK;
    }
}
