package dev.dtp.upgrade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.TollCurse;

/**
 * Mehrfachabbau. Baumfällen und Vein Mining: Schleichend mit dem passenden Werkzeug werden zusammenhängende Blöcke
 * derselben Art mit abgebaut. Abbaufläche 3×3 (Modul): Mit Spitzhacke oder Schaufel fallen die acht Nachbarn in der
 * Ebene quer zur getroffenen Seite mit. Greift schleichend eine Kette, gewinnt sie.
 *
 * <p>Jeder Block läuft über den normalen Abbau des Spielers — Drops, Fortune-Bonus, Haltbarkeit und Schutz-Mods
 * greifen also wie beim Einzelabbau. Das Werkzeug kann dabei zerbrechen; dann endet die Kette. Die Drops aller Blöcke
 * eines Mehrfachabbaus (auch des Startblocks) landen direkt im Inventar, Überschuss fällt um den Spieler. Nach dem
 * Fällen eines Baums sammelt der Spieler kurz in größerem Radius auf ({@link PickupBoost}).
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ChainBreaker {
    /** Verhindert, dass die mit abgebauten Blöcke ihrerseits Ketten auslösen. Nur auf dem Server-Thread benutzt. */
    private static boolean breaking;
    /** Wer gerade mehrfach abbaut — seine Drops gehen ins Inventar. */
    private static ServerPlayer chainPlayer;

    /** Der selbst abgebaute Startblock eines Mehrfachabbaus; seine Drops fallen erst nach {@link #onBreak}. */
    private record Origin(UUID player, BlockPos pos, long tick) {
    }

    private static Origin origin;

    private ChainBreaker() {
    }

    /** Läuft gerade ein Mehrfachabbau? Der Maut-Fluch lässt mit abgebaute Blöcke frei. */
    public static boolean isBreaking() {
        return breaking;
    }

    private record Kind(PlayerUpgrade upgrade, TagKey<Item> tool, TagKey<Block> blocks) {
    }

    private static final List<Kind> KINDS = List.of(
            new Kind(PlayerUpgrade.TREE_FELLING, ItemTags.AXES, BlockTags.LOGS),
            new Kind(PlayerUpgrade.VEIN_MINING, ItemTags.PICKAXES, Tags.BlockItems.ORES.block()));

    // Niedrigste Priorität: Schutz-Mods dürfen den Abbau vorher abbrechen; abgebrochen wird dann auch keine Kette.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onBreak(BreakBlockEvent event) {
        if (breaking || event.isCanceled() || TollCurse.blocksChain() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        // Kann das Werkzeug den Block nicht ernten (Holzspitzhacke an Diamanterz), gibt es keine Kette — sie würde
        // die ganze Ader zerstören, ohne dass etwas droppt.
        if (!state.canHarvestBlock(level, event.getPos(), player)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            for (Kind kind : KINDS) {
                if (state.is(kind.blocks()) && tool.is(kind.tool())) {
                    int limit = (int) PlayerUpgrades.value(player, kind.upgrade());
                    if (limit > 1) {
                        int broken = breakAll(player, event.getPos(), collect(level, event.getPos(), state.getBlock(), limit - 1));
                        if (kind.upgrade() == PlayerUpgrade.TREE_FELLING && broken > 0) {
                            fellingPickup(player);
                        }
                        return;
                    }
                }
            }
        }
        if (PlayerUpgrades.level(player, PlayerUpgrade.AREA_MINING) > 0 && (tool.is(ItemTags.PICKAXES) || tool.is(ItemTags.SHOVELS))) {
            breakAll(player, event.getPos(), areaTargets(level, event.getPos(), hitFace(player, event.getPos()), tool));
        }
    }

    /**
     * Nach dem Fällen eines Baums kurz größerer Aufsammelradius — für Setzlinge, Stöcke und Äpfel aus dem Laubzerfall.
     * Dauer und Reichweite aus {@code params} {@code pickup_seconds} und {@code pickup_range} (Default 5 s, 1 Block).
     */
    private static void fellingPickup(ServerPlayer player) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.TREE_FELLING);
        int level = PlayerUpgrades.level(player, PlayerUpgrade.TREE_FELLING);
        double seconds = def == null ? 5 : def.param("pickup_seconds", level, 5);
        double range = def == null ? 1 : def.param("pickup_range", level, 1);
        PickupBoost.grant(player, (int) Math.round(seconds * 20), range);
    }

    /**
     * Drops des Mehrfachabbaus direkt ins Inventar, Überschuss fällt um den Spieler. Gilt für die mit abgebauten Blöcke
     * und für den selbst abgebauten Startblock, dessen Drops erst nach {@link #onBreak} fallen. Erfahrung erscheint am
     * Spieler, damit Mending sie wie gewohnt aufnimmt.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        boolean chained = breaking && player == chainPlayer;
        boolean first = origin != null && origin.player().equals(player.getUUID()) && origin.pos().equals(event.getPos())
                && origin.tick() == event.getLevel().getGameTime();
        if (!chained && !first) {
            return;
        }
        if (first) {
            origin = null;
        }
        for (ItemEntity drop : event.getDrops()) {
            DirectDrops.give(player, drop.getItem());
        }
        event.getDrops().clear();
        int xp = event.getDroppedExperience();
        if (xp > 0) {
            ExperienceOrb.award(event.getLevel(), player.position(), xp);
            event.setDroppedExperience(0);
        }
    }

    /** Die getroffene Seite des Blocks — per Blickstrahl, sonst grob aus der Blickrichtung. */
    private static Direction hitFace(ServerPlayer player, BlockPos pos) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, false);
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(pos)) {
            return block.getDirection();
        }
        if (player.getXRot() > 45) {
            return Direction.UP;
        }
        if (player.getXRot() < -45) {
            return Direction.DOWN;
        }
        return player.getDirection().getOpposite();
    }

    /** Natürlich generierte Gesteine (Stein, Deepslate, Netherrack …), die die Spitzhacke gemeinsam abbaut. */
    public static final TagKey<Block> AREA_PICKAXE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Dtp.MODID, "area_mining/pickaxe"));
    /** Natürlich generierte Böden (Gras, Erde, Sand, Kies …), die die Schaufel gemeinsam abbaut. */
    public static final TagKey<Block> AREA_SHOVEL = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Dtp.MODID, "area_mining/shovel"));

    /** Obsidian bildet eine eigene Gruppe: Es nimmt nur Obsidian mit und wird von keinem Gestein mitgenommen. */
    public static final TagKey<Block> AREA_OBSIDIAN = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Dtp.MODID, "area_mining/obsidian"));

    private record AreaGroup(TagKey<Item> tool, TagKey<Block> blocks) {
    }

    private static final List<AreaGroup> AREA_GROUPS = List.of(
            new AreaGroup(ItemTags.PICKAXES, AREA_PICKAXE),
            new AreaGroup(ItemTags.PICKAXES, AREA_OBSIDIAN),
            new AreaGroup(ItemTags.SHOVELS, AREA_SHOVEL));

    /** Die Gruppe, zu der Werkzeug und Mittelblock gemeinsam passen, sonst null. */
    private static TagKey<Block> areaGroup(ItemStack tool, BlockState middle) {
        for (AreaGroup group : AREA_GROUPS) {
            if (tool.is(group.tool()) && middle.is(group.blocks())) {
                return group.blocks();
            }
        }
        return null;
    }

    /**
     * Die bis zu acht Nachbarn eines Blocks in der Ebene quer zu {@code face}. Abgebaut wird nur innerhalb einer
     * Blockgruppe ({@code #dtp:area_mining/pickaxe}, {@code /obsidian} bzw. {@code /shovel}): natürlich generierte Gesteine,
     * Obsidian für sich bzw. Böden.
     * Die Regel ist symmetrisch — Gras nimmt Erde mit und Erde Gras. Erze, Baublöcke (Steinziegel, Treppen, Mauern)
     * und alles außerhalb der Gruppe bleiben stehen. Liegt der Mittelblock in keiner Gruppe des Werkzeugs, gibt es
     * keine Fläche.
     */
    public static List<BlockPos> areaTargets(ServerLevel level, BlockPos center, Direction face, ItemStack tool) {
        List<BlockPos> targets = new ArrayList<>();
        BlockState middle = level.getBlockState(center);
        TagKey<Block> group = areaGroup(tool, middle);
        if (group == null || !tool.isCorrectToolForDrops(middle)) {
            return targets;
        }
        Direction.Axis axis = face.getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos pos = switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.is(group) && state.getDestroySpeed(level, pos) >= 0 && tool.isCorrectToolForDrops(state)) {
                    targets.add(pos);
                }
            }
        }
        return targets;
    }

    /** Baut die Ziele ab und merkt den Startblock vor, damit auch seine Drops ins Inventar gehen. @return abgebaute Blöcke */
    private static int breakAll(ServerPlayer player, BlockPos start, List<BlockPos> targets) {
        if (targets.isEmpty()) {
            return 0;
        }
        origin = new Origin(player.getUUID(), start.immutable(), player.level().getGameTime());
        ItemStack tool = player.getMainHandItem();
        int broken = 0;
        breaking = true;
        chainPlayer = player;
        try {
            for (BlockPos pos : targets) {
                if (player.getMainHandItem() != tool || tool.isEmpty()) {
                    break;
                }
                if (player.gameMode.destroyBlock(pos)) {
                    broken++;
                }
            }
        } finally {
            breaking = false;
            chainPlayer = null;
        }
        return broken;
    }

    /**
     * Breitensuche über alle 26 Nachbarn, nächste zuerst. Gesucht wird nur derselbe Block wie der abgebaute —
     * ein Eichenstamm nimmt keine Birke mit, Erz keine Deepslate-Variante.
     */
    public static List<BlockPos> collect(ServerLevel level, BlockPos origin, Block block, int max) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < max) {
            BlockPos current = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                if (found.size() >= max) {
                    break;
                }
                BlockPos candidate = next.immutable();
                if (seen.add(candidate) && level.isLoaded(candidate) && level.getBlockState(candidate).is(block)) {
                    found.add(candidate);
                    queue.add(candidate);
                }
            }
        }
        return found;
    }
}
