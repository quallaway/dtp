package dev.dtp.beacon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.phys.Vec3;

import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Zahlenwert {@code dtp:compost_chance} für Vanillas Kompost-Chancen ({@code minecraft:compostable/low} usw., per
 * Datapack überschrieben): 1 mit {@code chance} Prozent, sonst 0. Steht der Komposter ({@code ORIGIN}) im Radius des
 * Beacon-Effekts Landwirtschaft, kommen {@code beacon.compostBonus} Prozentpunkte dazu, additiv: Samen 30 → 40 %.
 *
 * <p>{@code ComposterBlock.addLayer} wertet die Chance für jeden Befüllweg aus — Spieler, Trichter, Bauern-Villager —,
 * deshalb braucht der Bonus weder Event noch Mixin (docs/21-beacon-effekte.md).
 */
public record CompostChance(int chance) implements ContextIntProvider {
    public static final MapCodec<CompostChance> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 100).fieldOf("chance").forGetter(CompostChance::chance)
    ).apply(i, CompostChance::new));

    @Override
    public int getIntUnsafe(LootContext context) {
        int roll = context.getRandom().nextInt(100);
        if (roll < chance) {
            return 1;
        }
        // Erst der billige Wurf, dann die Radius-Prüfung.
        int bonus = DtpConfig.COMPOST_BONUS.get();
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        return bonus > 0 && roll < chance + bonus && origin != null
                && BeaconLogic.effectAt(context.getLevel(), BlockPos.containing(origin), ServerUpgrade.EFFECT_AGRICULTURE)
                ? 1 : 0;
    }

    @Override
    public MapCodec<CompostChance> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
    }
}
