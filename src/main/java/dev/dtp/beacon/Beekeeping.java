package dev.dtp.beacon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Beacon-Effekt Imkerei (Klasse B, docs/21-beacon-effekte.md): mehr Honig, mehr Ertrag beim Ernten, breitere Bestäubung und
 * friedliche Bienen im Radius. Alles hängt an Events, die ohnehin feuern; vom Beacon aus wird nie über Bienen iteriert.
 *
 * <p>Nestfüllung und Ernte-Bonus werden am Ende des Ticks verrechnet ({@link #flushPending}): Vanilla setzt die
 * Honigstufe in {@code BeehiveBlockEntity.releaseOccupant} mit einem für den ganzen Tick gemerkten Blockzustand —
 * ein sofortiges Erhöhen könnte von der nächsten Biene desselben Ticks überschrieben werden. Die Biene selbst lässt
 * sich über den Stock nicht verfolgen (Vanilla verwirft ihre UUID beim Einzug), deshalb dient das Block-Update der
 * Honigstufe als Signal — aber nur, wenn im selben Tick auch eine Biene aus genau diesem Stock in die Welt kam.
 * Sonst ließe sich jedes Block-Update (Redstone, Neu-Setzen) als Ablieferung ausgeben.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class Beekeeping {
    /** Wie oft eine Biene mit Nektar im Radius eine Pflanze wachsen lässt (Ticks). */
    public static final int POLLINATION_INTERVAL = 40;
    /** So tief unter der Biene wird gesucht (Vanilla: 2). */
    static final int POLLINATION_DEPTH = 3;
    /** Zufällige Stellen je Versuch: findet auf lückigen Feldern meist etwas, ohne den Radius abzusuchen. */
    static final int POLLINATION_TRIES = 4;

    /** Stöcke, deren Honigstufe in diesem Tick durch eine Nektar-Ablieferung gestiegen ist. */
    private static final Set<GlobalPos> FILLED = new LinkedHashSet<>();
    /** Stöcke, aus denen in diesem Tick eine Biene in die Welt kam — Beleg, dass ein Block-Update eine Ablieferung war. */
    private static final Set<GlobalPos> RELEASED = new HashSet<>();
    /** Stöcke, die ein Spieler in diesem Tick im Radius ohne Lagerfeuer erntet; ihre Bienen bleiben drin. */
    private static final Set<GlobalPos> CALMED = new HashSet<>();
    /** Volle Stöcke, die ein Spieler in diesem Tick zu ernten versucht hat; true = Schere, false = Glasflasche. */
    private static final Map<GlobalPos, Boolean> HARVESTED = new HashMap<>();
    /** Unterdrückt das eigene Block-Update beim Aufstocken. */
    private static boolean raising;

    private Beekeeping() {
    }

    // --- Nestfüllung ---

    /**
     * Vanilla erhöht die Honigstufe nur beim Abliefern von Nektar (mit Block-Update); beim Ernten fällt sie auf 0. Eine
     * Stufe über 0 zeigt also eine Ablieferung an. Die Höhe der Chance und der Radius werden erst am Tickende geprüft.
     */
    @SubscribeEvent
    static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        BlockState state = event.getState();
        if (raising || !(state.getBlock() instanceof BeehiveBlock) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honey > 0 && honey < BeehiveBlock.MAX_HONEY_LEVELS) {
            FILLED.add(GlobalPos.of(level.dimension(), event.getPos().immutable()));
        }
    }

    /** Erhöht die Honigstufe um eine Stufe, wenn der Effekt dort wirkt und der Wurf gelingt. */
    public static void raiseHoney(ServerLevel level, BlockPos pos, int chancePercent) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BeehiveBlock)) {
            return;
        }
        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        // Stufe 0: im selben Tick schon geerntet — dort gibt es nichts aufzustocken.
        if (honey == 0 || honey >= BeehiveBlock.MAX_HONEY_LEVELS || !roll(level.getRandom(), chancePercent)
                || !BeaconLogic.effectAt(level, pos, ServerUpgrade.EFFECT_BEEKEEPING)) {
            return;
        }
        raising = true;
        try {
            level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, honey + 1));
        } finally {
            raising = false;
        }
    }

    // --- Ernte-Bonus ---

    /**
     * Merkt sich den Ernteversuch an einem vollen Stock. Ob Vanilla wirklich geerntet hat (nicht beim Schleichen,
     * nicht nach Abbruch durch eine andere Mod), zeigt erst die Honigstufe 0 am Tickende.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BeehiveBlock)
                || state.getValue(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.MAX_HONEY_LEVELS) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean shears = stack.canPerformAction(ItemAbilities.SHEARS_HARVEST);
        if (!shears && !stack.is(Items.GLASS_BOTTLE)) {
            return;
        }
        GlobalPos pos = GlobalPos.of(level.dimension(), event.getPos().immutable());
        HARVESTED.put(pos, shears);
        if (!CampfireBlock.isSmokeyPos(level, event.getPos()) && BeaconLogic.effectAt(level, event.getPos(), ServerUpgrade.EFFECT_BEEKEEPING)) {
            CALMED.add(pos);
        }
    }

    /**
     * Ohne Lagerfeuer wirft Vanilla beim Ernten alle Bienen hinaus. Im Radius sind sie friedlich, und wer Nektar trägt
     * oder bei Nacht und Regen fliegt sofort wieder hinein — das blitzte für einen Tick vor dem Stock auf und setzte
     * die Brutzeit zurück. Deshalb bleiben die Bienen im Stock, wie unter einem Lagerfeuer: Ein abgebrochener Beitritt
     * lässt {@code addFreshEntity} scheitern, und Vanilla behält den Bewohner samt Brutzeit.
     *
     * <p>Nur solange die Honigstufe 0 ist, also direkt nach der Ernte. Eine echte Ablieferung im selben Tick hebt die
     * Stufe vor dem Beitritt an und bleibt unberührt; sonst stünde die Biene mit ihrem Nektar noch im Stock und
     * lieferte ein zweites Mal ab.
     *
     * <p>Zugleich merkt sich der Hook jeden Stock, aus dem eine Biene kommt — Beleg für die Nestfüllung.
     */
    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Bee bee) || bee.getHivePos() == null
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GlobalPos hive = GlobalPos.of(level.dimension(), bee.getHivePos());
        BlockState state = level.getBlockState(hive.pos());
        if (CALMED.contains(hive) && state.getBlock() instanceof BeehiveBlock && state.getValue(BeehiveBlock.HONEY_LEVEL) == 0) {
            event.setCanceled(true);
            return;
        }
        RELEASED.add(hive);
    }

    /** Wirft nach einer gelungenen Ernte im Radius mit der Chance eine Wabe bzw. Honigflasche zusätzlich aus. */
    public static void harvestBonus(ServerLevel level, BlockPos pos, boolean shears, int chancePercent) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BeehiveBlock) || state.getValue(BeehiveBlock.HONEY_LEVEL) != 0
                || !roll(level.getRandom(), chancePercent) || !BeaconLogic.effectAt(level, pos, ServerUpgrade.EFFECT_BEEKEEPING)) {
            return;
        }
        Block.popResource(level, pos, new ItemStack(shears ? Items.HONEYCOMB : Items.HONEY_BOTTLE));
    }

    // --- Bestäubung ---

    /** Jede Biene mit Nektar im Radius lässt alle {@link #POLLINATION_INTERVAL} Ticks eine Pflanze wachsen. */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee) || (bee.tickCount + bee.getId()) % POLLINATION_INTERVAL != 0
                || !bee.hasNectar() || !(bee.level() instanceof ServerLevel level)) {
            return;
        }
        int radius = DtpConfig.BEE_POLLINATION_RADIUS.get();
        if (radius > 0 && BeaconLogic.effectAt(level, bee.blockPosition(), ServerUpgrade.EFFECT_BEEKEEPING)) {
            pollinate(level, bee.blockPosition(), radius, level.getRandom());
        }
    }

    /** Lässt eine zufällig gefundene Pflanze im Radius unter {@code center} um eine Stufe wachsen; true bei Erfolg. */
    public static boolean pollinate(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int i = 0; i < POLLINATION_TRIES; i++) {
            BlockPos pos = center.offset(random.nextIntBetweenInclusive(-radius, radius),
                    -random.nextIntBetweenInclusive(1, POLLINATION_DEPTH), random.nextIntBetweenInclusive(-radius, radius));
            if (grow(level, pos, random)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dieselbe Wirkung wie Vanillas {@code BeeGrowCropGoal}: eine Stufe, mit Partikeln. Dieselben Pflanzen plus die
     * Kannenpflanze, die Vanilla trotz {@code #bee_growables} auslässt.
     */
    public static boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.BEE_GROWABLES)) {
            return false;
        }
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
        } else if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            if (age < SweetBerryBushBlock.MAX_AGE) {
                grown = state.setValue(SweetBerryBushBlock.AGE, age + 1);
            }
        } else if ((state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) && state.getBlock() instanceof BonemealableBlock vines
                && vines.isValidBonemealTarget(level, pos, state, BonemealSource.MOB)) {
            vines.performBonemeal(level, random, pos, state, BonemealSource.MOB);
            grown = level.getBlockState(pos);
        } else if (state.getBlock() instanceof PitcherCropBlock pitcher
                && pitcher.isValidBonemealTarget(level, pos, state, BonemealSource.MOB)) {
            // Steht in #bee_growables, Vanillas BeeGrowCropGoal übergeht sie aber. Knochenmehl wächst genau eine Stufe
            // und kümmert sich um beide Hälften, auch wenn die Biene die obere trifft.
            pitcher.performBonemeal(level, random, pos, state, BonemealSource.MOB);
            grown = level.getBlockState(pos);
        }
        if (grown == null) {
            return false;
        }
        level.levelEvent(2011, pos, 15);
        level.setBlockAndUpdate(pos, grown);
        return true;
    }

    // --- Friedliche Bienen ---

    /** Bienen im Radius nehmen keinen Spieler ins Visier — auch nicht beim Ernten ohne Lagerfeuer. */
    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Bee bee && event.getNewAboutToBeSetTarget() instanceof Player
                && BeaconLogic.effectAt(bee.level(), bee.blockPosition(), ServerUpgrade.EFFECT_BEEKEEPING)) {
            event.setCanceled(true);
            bee.stopBeingAngry();
        }
    }

    // --- Tickende ---

    /** Am Ende jedes Server-Ticks; tut nur etwas, wenn in diesem Tick ein Stock gefüllt oder geerntet wurde. */
    public static void flushPending(MinecraftServer server) {
        if (FILLED.isEmpty() && HARVESTED.isEmpty() && RELEASED.isEmpty() && CALMED.isEmpty()) {
            return;
        }
        if (DtpConfig.EFFECTS_ENABLED.get()) {
            int honeyChance = DtpConfig.BEE_HONEY_CHANCE.get();
            for (GlobalPos pos : FILLED) {
                ServerLevel level = server.getLevel(pos.dimension());
                if (level != null && RELEASED.contains(pos)) {
                    raiseHoney(level, pos.pos(), honeyChance);
                }
            }
            int harvestChance = DtpConfig.BEE_HARVEST_BONUS.get();
            HARVESTED.forEach((pos, shears) -> {
                ServerLevel level = server.getLevel(pos.dimension());
                if (level != null) {
                    harvestBonus(level, pos.pos(), shears, harvestChance);
                }
            });
        }
        clear();
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    private static void clear() {
        FILLED.clear();
        RELEASED.clear();
        CALMED.clear();
        HARVESTED.clear();
    }

    private static boolean roll(RandomSource random, int chancePercent) {
        return chancePercent > 0 && random.nextInt(100) < chancePercent;
    }
}
