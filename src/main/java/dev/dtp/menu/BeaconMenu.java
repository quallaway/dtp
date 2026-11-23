package dev.dtp.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import dev.dtp.DtpConfig;
import dev.dtp.beacon.BeaconLogic;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Money;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.UpgradeCosts;
import dev.dtp.upgrade.WorldProgress;

/**
 * Beacon: Start des Durchlaufs, Server-Upgrades als Baum auf den Reitern Welt, Markt und Module (wie in der Werkstatt,
 * {@link UpgradeTree}) und die Effektauswahl als vierter Reiter.
 * Knopf-IDs: 1 starten, 2 abbrechen, 3 gewähltes Upgrade kaufen, 20+n Reiter wählen (n = 3: Effekte),
 * 100+n / 1000+16n+s Feld wählen, 200+n Effekt umschalten ({@link EffectSelection}).
 */
public class BeaconMenu extends BlockBoundMenu {
    public static final int CONFIRM = 1;
    public static final int CANCEL = 2;
    public static final int BUY = 3;
    public static final int TAB = 20;
    /** Reiter-Index der Effektauswahl, nach den Baum-Reitern aus {@link UpgradeCosts.Tab}. */
    public static final int EFFECTS_TAB = UpgradeCosts.Tab.values().length;

    private int tab;
    private ServerUpgrade selected;
    private int selectedLevel;
    private Component message;

    public BeaconMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.BEACON.get(), id, inventory, pos, ModBlocks.BEACON.get());
    }

    public static BeaconMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BeaconMenu(id, inventory, buf.readBlockPos());
    }

    private boolean effectsTab() {
        return tab == EFFECTS_TAB;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerUpgrade[] upgrades = ServerUpgrade.values();
        WorldProgress progress = WorldProgress.get(level.getServer());
        if (buttonId == CONFIRM) {
            BeaconLogic.confirm(level, pos, player);
        } else if (buttonId == CANCEL) {
            if (BeaconLogic.cancel(level, pos, player)) {
                player.closeContainer();
            }
        } else if (buttonId == BUY) {
            if (selected != null) {
                Component refusal = UpgradeTree.levelCheck(selectedLevel, progress.level(selected));
                message = refusal != null ? refusal : describe(BeaconLogic.buy(level, pos, player, selected));
            }
        } else if (buttonId >= TAB && buttonId <= TAB + EFFECTS_TAB) {
            tab = buttonId - TAB;
            selected = null;
            selectedLevel = 0;
            message = null;
        } else if (UpgradeTree.selectedOrdinal(buttonId, upgrades.length) >= 0) {
            ServerUpgrade upgrade = upgrades[UpgradeTree.selectedOrdinal(buttonId, upgrades.length)];
            int lvl = UpgradeTree.selectedLevel(buttonId);
            UpgradeCosts.Definition def = UpgradeCosts.get(upgrade);
            if (effectsTab() || def == null || def.tab().ordinal() != tab || (lvl > 0 && (!def.split() || lvl > def.maxLevel()))) {
                return false;
            }
            selected = upgrade;
            selectedLevel = lvl;
            message = null;
        } else if (EffectSelection.byButton(buttonId) != null) {
            ServerUpgrade effect = EffectSelection.byButton(buttonId);
            message = progress.toggleEffect(effect) ? null
                    : Component.translatable(progress.level(effect) == 0 ? "dtp.effects.locked" : "dtp.beacon.no_effect_slot");
        } else {
            return false;
        }
        sync.markChanged();
        return true;
    }

    private static Component describe(BeaconLogic.BuyResult result) {
        return switch (result) {
            case BeaconLogic.Bought b -> Component.translatable("dtp.upgrade.bought_self", b.level(), Money.component(b.cost()));
            case BeaconLogic.NotStarted n -> Component.translatable("dtp.upgrade.not_started");
            case BeaconLogic.Maxed m -> Component.translatable("dtp.upgrade.maxed");
            case BeaconLogic.NotConfigured n -> Component.translatable("dtp.upgrade.not_configured");
            case BeaconLogic.Insufficient i -> Component.translatable("dtp.upgrade.insufficient", Money.component(i.missing()));
            case BeaconLogic.ContractsMissing c -> Component.translatable("dtp.upgrade.contracts_missing", c.missing());
            case BeaconLogic.EffectSlotMissing e -> Component.translatable("dtp.upgrade.effect_slot_missing");
            case BeaconLogic.RequirementsMissing r -> Component.translatable("dtp.workshop.requirements_missing");
        };
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("started", progress.started());
        tag.putBoolean("border_removed", progress.borderRemoved());
        tag.putInt("radius", progress.territoryRadius());
        tag.putInt("tab", tab);
        tag.putBoolean("effects_tab", effectsTab());
        tag.putInt("selected", selected == null ? -1 : selected.ordinal());
        tag.putInt("selected_level", selectedLevel);
        EffectSelection.put(tag, progress, progress.activeEffects(), progress.effectSlots());

        DepositChests.Pool pool = DepositChests.find(level, pos, DtpConfig.BEACON_DEPOSIT_RADIUS.get());
        tag.putLong("balance", pool.balance());
        tag.putInt("chests", pool.chests().size());

        ListTag nodes = new ListTag();
        if (!effectsTab()) {
            for (ServerUpgrade u : ServerUpgrade.values()) {
                UpgradeCosts.Definition def = UpgradeCosts.get(u);
                if (def == null || def.tab().ordinal() != tab) {
                    continue;
                }
                int lvl = progress.level(u);
                if (def.split()) {
                    for (int nodeLevel = 1; nodeLevel <= def.maxLevel(); nodeLevel++) {
                        CompoundTag e = node(u, def, lvl, nodeLevel, lvl >= nodeLevel ? -1 : def.cost(nodeLevel), progress);
                        ListTag requires = nodeLevel == 1 ? requirements(def, progress) : new ListTag();
                        if (nodeLevel > 1) {
                            requires.add(UpgradeTree.requirement(u.id, nodeLevel - 1, u.ordinal(), nodeLevel - 1, lvl >= nodeLevel - 1));
                        }
                        e.put("requires", requires);
                        nodes.add(e);
                    }
                } else {
                    CompoundTag e = node(u, def, lvl, 0, lvl >= def.maxLevel() ? -1 : def.cost(lvl + 1), progress);
                    e.put("requires", requirements(def, progress));
                    nodes.add(e);
                }
            }
        }
        tag.put("nodes", nodes);
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }

    /**
     * Ein Feld. Sperrgründe, die nicht im Datapack stehen, kommen als {@code block} dazu: fehlende Contract-Abschlüsse
     * für die nächste Contract-Stufe und fehlender Effektslot für Effekte.
     */
    private static CompoundTag node(ServerUpgrade u, UpgradeCosts.Definition def, int lvl, int nodeLevel, long cost, WorldProgress progress) {
        CompoundTag e = UpgradeTree.node(u.ordinal(), u.id, def.icon(Math.max(1, nodeLevel)), def.position(Math.max(1, nodeLevel)), nodeLevel, lvl,
                def.maxLevel(), cost);
        boolean next = cost >= 0 && (nodeLevel == 0 || nodeLevel == lvl + 1);
        if (next && u == ServerUpgrade.CONTRACT_TIER && progress.missingForNextContractTier() > 0) {
            MenuSync.putComponent(e, "block", Component.translatable("dtp.beacon.block.contracts", progress.missingForNextContractTier()));
        } else if (next && u.isEffect() && progress.effectSlots() < 1) {
            MenuSync.putComponent(e, "block", Component.translatable("dtp.upgrade.effect_slot_missing"));
        }
        return e;
    }

    private static ListTag requirements(UpgradeCosts.Definition def, WorldProgress progress) {
        ListTag requires = new ListTag();
        for (PlayerUpgradeDefinitions.Requirement r : def.requires()) {
            ServerUpgrade other = ServerUpgrade.byId(r.upgrade());
            UpgradeCosts.Definition otherDef = other == null ? null : UpgradeCosts.get(other);
            int target = otherDef != null && otherDef.split() ? Math.min(r.level(), otherDef.maxLevel()) : 0;
            requires.add(UpgradeTree.requirement(r.upgrade(), r.level(), other == null ? -1 : other.ordinal(), target,
                    other != null && progress.level(other) >= r.level()));
        }
        return requires;
    }
}
