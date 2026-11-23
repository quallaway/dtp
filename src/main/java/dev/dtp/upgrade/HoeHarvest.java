package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import dev.dtp.Dtp;

/**
 * Zwei Spieler-Upgrades für die Hacke. Flächenernte: Rechtsklick auf eine reife Pflanze erntet alle reifen Pflanzen
 * im Quadrat und pflanzt nach; jede kostet 1 Haltbarkeit (Unbreaking wirkt wie üblich). Mehr Ertrag: Chance je reifer
 * Pflanze auf eine zweite Ernte — nur der Ertrag, kein zusätzliches Saatgut; gilt für die Flächenernte und für das
 * Abbauen mit der Hacke. Reif: Weizen, Karotten, Kartoffeln, Rote Bete (und andere {@link CropBlock}s), Netherwarze,
 * Kakao. Ein Klick auf Kakao erntet statt des waagrechten Quadrats eine senkrechte Wand an der Stammseite, nur Kakao
 * mit derselben Ausrichtung. Zuckerrohr und Kaktus erntet die Flächenernte als Säulen: alles über dem untersten Block
 * (beim Kaktus samt Blüte), 1 Haltbarkeit je Säule, ohne Mehr Ertrag. Alle Drops landen direkt im Inventar, was nicht
 * passt, fällt um den Spieler zu Boden.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class HoeHarvest {
    private HoeHarvest() {
    }

    public static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        }
        return state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
    }

    private static BlockState replanted(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        // Kakao behält seine Ausrichtung zum Stamm.
        if (state.getBlock() instanceof CocoaBlock) {
            return state.setValue(CocoaBlock.AGE, 0);
        }
        return state.setValue(NetherWartBlock.AGE, 0);
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack hoe = event.getItemStack();
        if (!(event.getLevel() instanceof ServerLevel level)) {
            // Client-Vorhersage wie beim Platzieren eines Blocks: SUCCESS lässt den eigenen Client die Hand schwingen
            // (Ego- und eigene Third-Person-Ansicht); das Paket geht trotzdem an den Server, der die Ernte ausführt
            // und den Schwung an die anderen Spieler schickt. Die Upgrade-Werte kennt nur der Server, die Stufe ist
            // synchronisiert.
            if (PlayerUpgrades.level(player, PlayerUpgrade.HOE_HARVEST) > 0 && hoe.is(ItemTags.HOES)
                    && canHarvest(event.getLevel(), event.getPos())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        int radius = (int) PlayerUpgrades.value(player, PlayerUpgrade.HOE_HARVEST);
        if (radius <= 0 || !hoe.is(ItemTags.HOES) || !canHarvest(level, event.getPos())) {
            return;
        }
        int harvested = columnPlant(level.getBlockState(event.getPos())) != null
                ? harvestColumns(level, player, event.getPos(), hoe, event.getHand(), radius)
                : harvest(level, player, event.getPos(), hoe, event.getHand(), radius);
        if (harvested > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Erntet alle reifen Pflanzen im Quadrat mit Halbmesser {@code radius} um {@code center} (gleiche Höhe) und pflanzt
     * nach. Ist {@code center} Kakao, steht das Quadrat senkrecht in der Ebene der Stammseite (Breite × Höhe), und
     * geerntet wird nur Kakao mit derselben Ausrichtung. Drops laufen über die Loot-Table mit Spieler und Hacke, Fortune
     * greift also; sie landen direkt im Inventar. Zerbricht die Hacke, endet die Ernte. Gibt die Zahl der geernteten
     * Pflanzen zurück.
     */
    public static int harvest(ServerLevel level, Player player, BlockPos center, ItemStack hoe, InteractionHand hand, int radius) {
        double bonusChance = PlayerUpgrades.value(player, PlayerUpgrade.HARVEST_BONUS);
        BlockState clicked = level.getBlockState(center);
        Direction cocoaFacing = clicked.getBlock() instanceof CocoaBlock ? clicked.getValue(CocoaBlock.FACING) : null;
        Iterable<BlockPos> area;
        if (cocoaFacing != null) {
            // Senkrechte Wand an der Stammseite: Breite quer zur Ausrichtung, dazu die Höhe.
            Direction side = cocoaFacing.getClockWise();
            area = BlockPos.betweenClosed(center.relative(side, -radius).below(radius), center.relative(side, radius).above(radius));
        } else {
            area = BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius));
        }
        int harvested = 0;
        for (BlockPos p : area) {
            if (hoe.isEmpty()) {
                break;
            }
            BlockPos pos = p.immutable();
            BlockState state = level.getBlockState(pos);
            if (!level.isLoaded(pos) || !isMature(state)) {
                continue;
            }
            if (cocoaFacing != null && (!(state.getBlock() instanceof CocoaBlock) || state.getValue(CocoaBlock.FACING) != cocoaFacing)) {
                continue;
            }
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, hoe));
            drops.addAll(bonusDrops(drops, bonusChance, level.getRandom()));
            takeOne(drops, state.getBlock().asItem());
            level.setBlock(pos, replanted(state), Block.UPDATE_ALL);
            level.levelEvent(2001, pos, Block.getId(state));
            // Direkt ins Inventar: am Feld blieben die Items sonst verstreut liegen, an der Kakaoschote obendrein auf
            // ihr. Was nicht passt, fällt um den Spieler zu Boden.
            DirectDrops.giveAll(player, drops);
            hoe.hurtAndBreak(1, player, hand);
            harvested++;
        }
        return harvested;
    }

    /** Löst ein Klick auf {@code pos} die Flächenernte aus? Reife Pflanze oder Säulenpflanze mit erntbarem Teil. */
    public static boolean canHarvest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isMature(state)) {
            return true;
        }
        Block plant = columnPlant(state);
        BlockPos base = plant == null ? null : columnBase(level, pos, plant);
        return base != null && !column(level, base, plant).isEmpty();
    }

    /** Die Säulenpflanze, zu der der Block gehört: Zuckerrohr, Kaktus (auch dessen Blüte), sonst null. */
    public static Block columnPlant(BlockState state) {
        if (state.is(Blocks.SUGAR_CANE)) {
            return Blocks.SUGAR_CANE;
        }
        if (state.is(Blocks.CACTUS) || state.is(Blocks.CACTUS_FLOWER)) {
            return Blocks.CACTUS;
        }
        return null;
    }

    /** Unterster Block der Säule, in der {@code pos} steht (eine Kaktusblüte zählt zur Säule darunter), sonst null. */
    private static BlockPos columnBase(Level level, BlockPos pos, Block plant) {
        BlockPos p = level.getBlockState(pos).is(Blocks.CACTUS_FLOWER) ? pos.below() : pos;
        if (!level.getBlockState(p).is(plant)) {
            return null;
        }
        while (level.getBlockState(p.below()).is(plant)) {
            p = p.below();
        }
        return p;
    }

    /** Alles über der Basis, von unten nach oben: weitere Blöcke der Pflanze, beim Kaktus samt Blüte obenauf. */
    private static List<BlockPos> column(Level level, BlockPos base, Block plant) {
        List<BlockPos> parts = new ArrayList<>();
        BlockPos p = base.above();
        while (level.isLoaded(p)) {
            BlockState state = level.getBlockState(p);
            if (state.is(plant)) {
                parts.add(p);
            } else {
                if (plant == Blocks.CACTUS && state.is(Blocks.CACTUS_FLOWER)) {
                    parts.add(p);
                }
                break;
            }
            p = p.above();
        }
        return parts;
    }

    /**
     * Erntet Zuckerrohr bzw. Kaktus im Quadrat mit Halbmesser {@code radius} um die angeklickte Säule: je Spalte alles
     * über dem untersten Block, der stehen bleibt und nachwächst. Die Basis einer Spalte darf bis zu einen Block höher
     * oder tiefer liegen als die der angeklickten. Nur dieselbe Pflanze wie angeklickt. Drops über die Loot-Table mit
     * Spieler und Hacke, direkt ins Inventar; jede Säule kostet 1 Haltbarkeit, zerbricht die Hacke, endet die Ernte.
     * Mehr Ertrag gilt nicht. Gibt die Zahl der geernteten Säulen zurück.
     */
    public static int harvestColumns(ServerLevel level, Player player, BlockPos center, ItemStack hoe, InteractionHand hand, int radius) {
        Block plant = columnPlant(level.getBlockState(center));
        BlockPos clickedBase = plant == null ? null : columnBase(level, center, plant);
        if (clickedBase == null) {
            return 0;
        }
        int harvested = 0;
        for (BlockPos p : BlockPos.betweenClosed(clickedBase.offset(-radius, 0, -radius), clickedBase.offset(radius, 0, radius))) {
            if (hoe.isEmpty()) {
                break;
            }
            BlockPos base = null;
            for (int dy : new int[] {0, -1, 1}) {
                BlockPos candidate = p.offset(0, dy, 0);
                if (level.isLoaded(candidate) && level.getBlockState(candidate).is(plant)
                        && !level.getBlockState(candidate.below()).is(plant)) {
                    base = candidate;
                    break;
                }
            }
            List<BlockPos> parts = base == null ? List.of() : column(level, base, plant);
            if (parts.isEmpty()) {
                continue;
            }
            List<ItemStack> drops = new ArrayList<>();
            // Von oben nach unten: eine Kaktusblüte oder ein Halm verliert nie zuerst seinen Halt.
            for (int i = parts.size() - 1; i >= 0; i--) {
                BlockPos pos = parts.get(i);
                BlockState state = level.getBlockState(pos);
                drops.addAll(Block.getDrops(state, level, pos, null, player, hoe));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(2001, pos, Block.getId(state));
            }
            DirectDrops.giveAll(player, drops);
            hoe.hurtAndBreak(1, player, hand);
            harvested++;
        }
        return harvested;
    }

    /** Das Saatgut fürs Nachpflanzen aus den Drops nehmen; fehlt es (sehr selten), wird trotzdem nachgepflanzt. */
    private static void takeOne(List<ItemStack> drops, Item seed) {
        for (ItemStack stack : drops) {
            if (stack.is(seed)) {
                stack.shrink(1);
                return;
            }
        }
    }

    /** Mit {@code chance} eine Kopie des Ertrags — alle Drops außer Saatgut. Sonst nichts. */
    public static List<ItemStack> bonusDrops(List<ItemStack> drops, double chance, RandomSource random) {
        List<ItemStack> bonus = new ArrayList<>();
        if (chance > 0 && random.nextDouble() < chance) {
            for (ItemStack stack : drops) {
                if (!stack.isEmpty() && !stack.is(Tags.Items.SEEDS)) {
                    bonus.add(stack.copy());
                }
            }
        }
        return bonus;
    }

    /** Mehr Ertrag beim Abbauen einer reifen Pflanze mit der Hacke. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || !event.getTool().is(ItemTags.HOES) || !isMature(event.getState())) {
            return;
        }
        double chance = PlayerUpgrades.value(player, PlayerUpgrade.HARVEST_BONUS);
        List<ItemStack> stacks = event.getDrops().stream().map(ItemEntity::getItem).toList();
        BlockPos pos = event.getPos();
        for (ItemStack stack : bonusDrops(stacks, chance, event.getLevel().getRandom())) {
            ItemEntity entity = new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            event.getDrops().add(entity);
        }
    }
}
