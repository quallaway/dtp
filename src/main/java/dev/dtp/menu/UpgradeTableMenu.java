package dev.dtp.menu;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Money;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.PlayerUpgradeLogic;
import dev.dtp.upgrade.PlayerUpgrades;

/**
 * Werkstatt: Upgrade-Baum des Spielers, ein Tab je Pfad, dazu die Modulansicht zum Ausrüsten persönlicher Module.
 * Knopf-IDs: 1 ausgewähltes Upgrade kaufen, 10+n Tab wählen, 20 Modulansicht, 200+n Modul aus- oder abrüsten;
 * Auswahl im Baum siehe {@link UpgradeTree}.
 *
 * <p>Aufgeteilte Upgrades ({@code level_positions} im Datapack) erscheinen mit einem Feld je Stufe; jede Stufe setzt
 * die vorige voraus. Gekauft wird weiterhin immer die nächste Stufe — es ändert sich nur die Darstellung.
 */
public class UpgradeTableMenu extends BlockBoundMenu {
    public static final int BUY = 1;
    public static final int TAB = 10;
    public static final int MODULES = 20;
    public static final int TOGGLE_MODULE = 200;

    private PlayerUpgradeDefinitions.Tab tab = PlayerUpgradeDefinitions.Tab.BODY;
    private boolean modulesView;
    private PlayerUpgrade selected;
    /** Gewählte Stufe eines aufgeteilten Upgrades; 0 = ganzes Upgrade. */
    private int selectedLevel;
    private Component message;

    public UpgradeTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.UPGRADE_TABLE.get(), id, inventory, pos, ModBlocks.UPGRADE_TABLE.get());
    }

    public static UpgradeTableMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new UpgradeTableMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        PlayerUpgradeDefinitions.Tab[] tabs = PlayerUpgradeDefinitions.Tab.values();
        PlayerUpgrade[] upgrades = PlayerUpgrade.values();
        if (buttonId == BUY) {
            if (selected != null) {
                Component refusal = UpgradeTree.levelCheck(selectedLevel, PlayerUpgrades.of(player).level(selected));
                message = refusal != null ? refusal : describe(PlayerUpgradeLogic.buy(level, pos, player, selected));
            }
        } else if (buttonId >= TAB && buttonId < TAB + tabs.length) {
            tab = tabs[buttonId - TAB];
            modulesView = false;
            selected = null;
            selectedLevel = 0;
            message = null;
        } else if (buttonId == MODULES) {
            modulesView = true;
            selected = null;
            selectedLevel = 0;
            message = null;
        } else if (buttonId >= TOGGLE_MODULE && buttonId < TOGGLE_MODULE + upgrades.length) {
            if (!modulesView) {
                return false;
            }
            message = describe(PlayerUpgradeLogic.toggleModule(level, pos, player, upgrades[buttonId - TOGGLE_MODULE]));
        } else if (UpgradeTree.selectedOrdinal(buttonId, upgrades.length) >= 0) {
            PlayerUpgrade upgrade = upgrades[UpgradeTree.selectedOrdinal(buttonId, upgrades.length)];
            int lvl = UpgradeTree.selectedLevel(buttonId);
            PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(upgrade);
            if (modulesView || def == null || def.tab() != tab || (lvl > 0 && (!def.split() || lvl > def.maxLevel()))) {
                return false;
            }
            selected = upgrade;
            selectedLevel = lvl;
            message = null;
        } else {
            return false;
        }
        sync.markChanged();
        return true;
    }

    private static Component describe(PlayerUpgradeLogic.BuyResult result) {
        return switch (result) {
            case PlayerUpgradeLogic.Bought b -> Component.translatable("dtp.upgrade.bought_self", b.level(), Money.component(b.cost()));
            case PlayerUpgradeLogic.Outside o -> Component.translatable("dtp.workshop.outside");
            case PlayerUpgradeLogic.NotConfigured n -> Component.translatable("dtp.upgrade.not_configured");
            case PlayerUpgradeLogic.Maxed m -> Component.translatable("dtp.upgrade.maxed");
            case PlayerUpgradeLogic.RequirementsMissing r -> Component.translatable("dtp.workshop.requirements_missing");
            case PlayerUpgradeLogic.Insufficient i -> Component.translatable("dtp.upgrade.insufficient", Money.component(i.missing()));
            case PlayerUpgradeLogic.ModuleSlotMissing m -> Component.translatable("dtp.workshop.modules.no_slot");
        };
    }

    /** Erfolgreiches Umstecken braucht keine Meldung — die Slotanzeige zeigt es. */
    private static Component describe(PlayerUpgradeLogic.ToggleResult result) {
        return switch (result) {
            case PlayerUpgradeLogic.Equipped e -> null;
            case PlayerUpgradeLogic.Unequipped u -> null;
            case PlayerUpgradeLogic.ToggleOutside o -> Component.translatable("dtp.workshop.modules.outside");
            case PlayerUpgradeLogic.Locked l -> Component.translatable("dtp.workshop.modules.locked");
            case PlayerUpgradeLogic.NoFreeSlot n -> Component.translatable("dtp.workshop.modules.full");
        };
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tab", tab.ordinal());
        tag.putBoolean("modules_view", modulesView);
        tag.putBoolean("usable", PlayerUpgradeLogic.usable(level, pos));
        tag.putBoolean("modules_usable", PlayerUpgradeLogic.modulesUsable(level, pos));
        tag.putInt("selected", selected == null ? -1 : selected.ordinal());
        tag.putInt("selected_level", selectedLevel);

        DepositChests.Pool pool = DepositChests.find(level, pos);
        tag.putLong("balance", pool.balance());
        tag.putInt("chests", pool.chests().size());

        PlayerUpgrades mine = PlayerUpgrades.of(player);
        if (modulesView) {
            putModules(tag, mine);
            MenuSync.putComponent(tag, "message", message);
            return tag;
        }
        ListTag nodes = new ListTag();
        for (PlayerUpgrade u : PlayerUpgrade.values()) {
            PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(u);
            if (def == null || def.tab() != tab) {
                continue;
            }
            int lvl = mine.level(u);
            if (def.split()) {
                for (int nodeLevel = 1; nodeLevel <= def.maxLevel(); nodeLevel++) {
                    CompoundTag e = node(u, def, lvl, nodeLevel, lvl >= nodeLevel ? -1 : def.cost(nodeLevel));
                    ListTag requires = nodeLevel == 1 ? requirements(def, mine) : new ListTag();
                    if (nodeLevel > 1) {
                        // Jede Stufe setzt die vorige voraus — Linie zum Feld davor.
                        requires.add(UpgradeTree.requirement(u.id, nodeLevel - 1, u.ordinal(), nodeLevel - 1, lvl >= nodeLevel - 1));
                    }
                    e.put("requires", requires);
                    nodes.add(e);
                }
            } else {
                CompoundTag e = node(u, def, lvl, 0, lvl >= def.maxLevel() ? -1 : def.cost(lvl + 1));
                e.put("requires", requirements(def, mine));
                nodes.add(e);
            }
        }
        tag.put("nodes", nodes);
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }

    /** Ein Feld im Baum: ganzes Upgrade ({@code nodeLevel} 0) oder eine Stufe. {@code cost} −1 = gekauft bzw. voll. */
    private static CompoundTag node(PlayerUpgrade u, PlayerUpgradeDefinitions.Definition def, int lvl, int nodeLevel, long cost) {
        CompoundTag e = UpgradeTree.node(u.ordinal(), u.id, def.icon(Math.max(1, nodeLevel)), def.position(Math.max(1, nodeLevel)), nodeLevel, lvl,
                def.maxLevel(), cost);
        e.putBoolean("module", u.isModule());
        e.putBoolean("skill", u.isSkill());
        return e;
    }

    /** Voraussetzungen aus dem Datapack, mit Ziel-Feld für die Linie und Erfüllt-Markierung für Farbe und Hinweis. */
    private static ListTag requirements(PlayerUpgradeDefinitions.Definition def, PlayerUpgrades mine) {
        ListTag requires = new ListTag();
        for (PlayerUpgradeDefinitions.Requirement r : def.requires()) {
            PlayerUpgrade other = PlayerUpgrade.byId(r.upgrade());
            PlayerUpgradeDefinitions.Definition otherDef = other == null ? null : PlayerUpgradeDefinitions.get(other);
            int target = otherDef != null && otherDef.split() ? Math.min(r.level(), otherDef.maxLevel()) : 0;
            requires.add(UpgradeTree.requirement(r.upgrade(), r.level(), other == null ? -1 : other.ordinal(), target,
                    other != null && mine.level(other) >= r.level()));
        }
        return requires;
    }

    /**
     * Modulansicht im Format der Beacon-Effektauswahl ({@link EffectSelection}), damit derselbe Picker sie zeichnet.
     * Statt eines Effekt-Sprites tragen die Einträge ein Item-Symbol und den Namensschlüssel des Upgrades.
     */
    private static void putModules(CompoundTag tag, PlayerUpgrades mine) {
        List<PlayerUpgrade> equipped = mine.equippedModules();
        tag.putInt("effect_slots", mine.moduleSlots());
        tag.putString("label_active", "dtp.workshop.modules.active");
        tag.putString("label_pick", "dtp.workshop.modules.pick");
        tag.putString("label_locked", "dtp.workshop.modules.locked");
        ListTag all = new ListTag();
        ListTag active = new ListTag();
        for (PlayerUpgrade u : PlayerUpgrade.values()) {
            PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(u);
            if (!u.isModule() || def == null) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putInt("ordinal", u.ordinal());
            e.putString("id", u.id);
            def.icon().write(e);
            e.putString("name", u.translationKey());
            e.putBoolean("unlocked", mine.level(u) > 0);
            e.putBoolean("active", equipped.contains(u));
            all.add(e);
        }
        // Slots in Ausrüstungsreihenfolge, nicht in Enum-Reihenfolge.
        for (PlayerUpgrade u : equipped) {
            for (int i = 0; i < all.size(); i++) {
                if (all.getCompoundOrEmpty(i).getIntOr("ordinal", -1) == u.ordinal()) {
                    active.add(all.getCompoundOrEmpty(i).copy());
                }
            }
        }
        tag.put("effects", all);
        tag.put("active_effects", active);
    }
}
