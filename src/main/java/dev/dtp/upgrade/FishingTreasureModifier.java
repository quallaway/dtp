package dev.dtp.upgrade;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import dev.dtp.Dtp;

/**
 * Angelschatz (Spieler-Upgrade): Mit einer Chance je Fang gibt es zusätzlich einen seltenen Strukturfund aus
 * {@code dtp:gameplay/fishing/structure_finds} — Töpferscherben, Rüstungsbesatz-Vorlagen, Herz des Meeres,
 * Prüfungsschlüssel, verzaubertes Buch, Silbersack; bewusst keine Schallplatten. Hängt per Datei {@code data/dtp/loot_modifiers/fishing_treasure.json} an der
 * Vanilla-Angel-Loot-Table; der Angler ist im Loot-Kontext der Besitzer des Hakens. Funde gibt es nur in offenem
 * Wasser ({@link FishingHook#isOpenWaterFishing()}), damit AFK-Farmen mit 1×1-Loch nichts davon haben.
 * <p>
 * Double Bobbers (Spieler-Upgrade): Jeder Wurf würfelt die Vanilla-Angel-Tabelle so oft, wie das Upgrade angibt
 * (Wert = Items je Wurf, Standard 2), und zwar überall, auch im 1×1-Loch. Der Angelschatz wird für jedes dieser Items
 * einzeln gewürfelt.
 */
public class FishingTreasureModifier extends LootModifier {
    public static final MapCodec<FishingTreasureModifier> CODEC = RecordCodecBuilder.mapCodec(
            i -> codecStart(i).apply(i, FishingTreasureModifier::new));

    public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "gameplay/fishing/structure_finds"));

    public FishingTreasureModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof Player player)) {
            return loot;
        }
        int catches = 1;
        if (PlayerUpgrades.level(player, PlayerUpgrade.DOUBLE_BOBBERS) > 0) {
            catches = Math.max(1, (int) PlayerUpgrades.value(player, PlayerUpgrade.DOUBLE_BOBBERS));
            // Weitere Fänge roh aus der Vanilla-Tabelle, im selben Kontext (Glück, Haken, offenes Wasser gelten gleich),
            // ohne erneut Loot-Modifier anzuwenden. Der äußere Wurf hat seinen Rekursionsschutz hier schon abgebaut.
            LootTable fishing = context.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            for (int i = 1; i < catches; i++) {
                fishing.getRandomItemsRaw(context, loot::add);
            }
        }
        // Nur in offenem Wasser (dieselbe Regel wie für Vanilla-Schätze: rund 5×5 freie Wasserfläche). Aus einem
        // 1×1-Loch, wie es AFK-Angelfarmen nutzen, gibt es keine Funde.
        if (context.getOptional(LootContextParams.THIS_ENTITY) instanceof FishingHook hook && hook.isOpenWaterFishing()) {
            double chance = PlayerUpgrades.value(player, PlayerUpgrade.FISHING_TREASURE);
            if (chance > 0) {
                LootTable finds = context.getLevel().getServer().reloadableRegistries().getLootTable(TABLE);
                // Je Fang ein eigener Wurf: mit Double Bobbers kann jedes Item ein Fund sein.
                for (int i = 0; i < catches; i++) {
                    if (context.getRandom().nextDouble() < chance) {
                        finds.getRandomItemsRaw(context, loot::add);
                    }
                }
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
