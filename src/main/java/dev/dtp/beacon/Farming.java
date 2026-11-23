package dev.dtp.beacon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Beacon-Effekt Landwirtschaft ({@link ServerUpgrade#EFFECT_AGRICULTURE}, Klasse B, docs/21-beacon-effekte.md):
 * Trampelschutz für Ackerboden und Feldfrüchte, schnelleres Wachstum (auch Netherwarzen) und mehr Kompost. Den Kompost-Bonus liefert
 * {@link CompostChance} über die überschriebenen Vanilla-Werte {@code minecraft:compostable/*}. Die Zuchtpause gehört zum Stallmeister
 * ({@link AnimalHusbandry}).
 * Beides hängt an Events, die ohnehin feuern; über Blöcke im Radius wird nie iteriert.
 *
 * <p>Der Wachstums-Bonus hängt an {@link CropGrowEvent.Post}, nicht an {@code Pre}: {@code Pre} kennt Vanillas Wurf nicht,
 * ein erzwungenes Wachsen würde schlecht angelegte Felder deshalb überproportional beschleunigen. Eine Zusatzstufe nach
 * jedem echten Schritt ist dagegen ein fester Faktor auf Vanillas Tempo, egal wie gut das Feld angelegt ist.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class Farming {
    private Farming() {
    }

    /**
     * Ackerboden im Radius wird nicht zu Erde — damit bleiben auch die Pflanzen darauf stehen. Gilt für jede Entity, die
     * laut Vanilla trampeln darf: Spieler, Mobs, Villager, Tiere, Reittiere.
     */
    @SubscribeEvent
    static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && BeaconLogic.effectAt(level, event.getPos(), ServerUpgrade.EFFECT_AGRICULTURE)) {
            event.setCanceled(true);
        }
    }

    /** Nach einem Wachstumsschritt im Radius mit {@code beacon.cropGrowthBonus} Prozent Chance eine Stufe mehr. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onCropGrown(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isFieldCrop(event.getState())) {
            return;
        }
        int chance = DtpConfig.CROP_GROWTH_BONUS.get();
        // Erst der billige Wurf, dann die Radius-Prüfung.
        if (chance > 0 && level.getRandom().nextInt(100) < chance
                && BeaconLogic.effectAt(level, event.getPos(), ServerUpgrade.EFFECT_AGRICULTURE)) {
            bonusStage(level, event.getPos());
        }
    }

    private static boolean isFieldCrop(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof NetherWartBlock;
    }

    /**
     * Lässt die Feldfrucht (auch Netherwarze) an {@code pos} um eine Stufe wachsen, höchstens bis zur Reife; true bei
     * Erfolg. Stängel nur bis Stufe 7 — das Ansetzen der Frucht bleibt Vanilla. Ohne Partikel und ohne erneutes
     * Wachstums-Event.
     */
    public static boolean bonusStage(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState grown = null;
        if (state.getBlock() instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) {
                grown = crop.getStateForAge(crop.getAge(state) + 1);
            }
        } else if (state.getBlock() instanceof StemBlock) {
            int age = state.getValue(StemBlock.AGE);
            if (age < StemBlock.MAX_AGE) {
                grown = state.setValue(StemBlock.AGE, age + 1);
            }
        } else if (state.getBlock() instanceof NetherWartBlock) {
            int age = state.getValue(NetherWartBlock.AGE);
            if (age < NetherWartBlock.MAX_AGE) {
                grown = state.setValue(NetherWartBlock.AGE, age + 1);
            }
        }
        if (grown == null) {
            return false;
        }
        level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
        return true;
    }
}
