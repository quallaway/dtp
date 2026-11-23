package dev.dtp.upgrade;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.phys.Vec3;

import dev.dtp.beacon.BeaconLogic;

/**
 * Zahlenwert {@code dtp:server_upgrade} für Vanillas datengetriebene Werte (z. B. die Ofengeschwindigkeit
 * {@code minecraft:cooking/speed_default}): liefert {@code values[Stufe]} des Server-Upgrades, ab der letzten
 * Angabe bleibt der Wert stehen. Ohne Server oder bei unbekanntem Upgrade gilt {@code values[0]}.
 *
 * <p>Mit {@code "local": true} gilt die Stufe nur im Beacon-Gebiet ({@link BeaconLogic#inBeaconArea}), gemessen am
 * Ursprung des Kontexts (beim Ofen seine Position). Außerhalb oder ohne Ursprung gilt {@code values[0]}.
 */
public record ServerUpgradeValue(String upgrade, List<Float> values, boolean local) implements ContextFloatProvider {
    public static final MapCodec<ServerUpgradeValue> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("upgrade").forGetter(ServerUpgradeValue::upgrade),
            ExtraCodecs.nonEmptyList(Codec.FLOAT.listOf()).fieldOf("values").forGetter(ServerUpgradeValue::values),
            Codec.BOOL.optionalFieldOf("local", false).forGetter(ServerUpgradeValue::local)
    ).apply(i, ServerUpgradeValue::new));

    @Override
    public float getFloatUnsafe(LootContext context) {
        ServerUpgrade u = ServerUpgrade.byId(upgrade);
        if (u == null || context.getLevel() == null) {
            return values.getFirst();
        }
        if (local) {
            Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            if (origin == null || !BeaconLogic.inBeaconArea(context.getLevel(), BlockPos.containing(origin))) {
                return values.getFirst();
            }
        }
        int level = WorldProgress.get(context.getLevel().getServer()).level(u);
        return values.get(Math.min(Math.max(level, 0), values.size() - 1));
    }

    @Override
    public MapCodec<ServerUpgradeValue> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
    }
}
