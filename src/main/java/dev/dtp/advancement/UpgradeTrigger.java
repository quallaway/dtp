package dev.dtp.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Trigger für gekaufte Upgrades ({@code dtp:player_upgrade}, {@code dtp:server_upgrade}). Bedingungen, beide
 * optional: {@code upgrade} (die ID, z. B. {@code "border"}) und {@code level} (erreichte Stufe, wie beim
 * Vanilla-Beacon-Trigger, z. B. {@code {"min": 11}}).
 */
public class UpgradeTrigger extends SimpleCriterionTrigger<UpgradeTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String upgrade, int level) {
        trigger(player, instance -> instance.matches(upgrade, level));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<String> upgrade, MinMaxBounds.Ints level)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.optionalFieldOf("upgrade").forGetter(TriggerInstance::upgrade),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::level)
        ).apply(i, TriggerInstance::new));

        public boolean matches(String upgrade, int level) {
            return this.upgrade.map(upgrade::equals).orElse(true) && this.level.matches(level);
        }
    }
}
