package dev.dtp.compat.sophisticated;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

/**
 * Rucksack-Upgrade „Trade-Chest" bzw. „Einzahlkiste". Hat keine eigene Logik: Ob ein platzierter Rucksack als Lager
 * zählt, fragt {@link SophisticatedCompat} beim Suchen ab. Je Rucksack höchstens eins davon — ein Rucksack ist
 * entweder Warenlager oder Geldlager, wie bei den Kisten.
 */
public class StorageUpgradeItem extends UpgradeItemBase<StorageUpgradeItem.Wrapper> {
    public enum Kind {
        TRADE, DEPOSIT;

        final UpgradeType<Wrapper> type = new UpgradeType<>(Wrapper::new);
    }

    /** Höchstens ein DTP-Lager-Upgrade je Rucksack; die Gruppenlimits von Sophisticated Core bleiben unberührt. */
    private static final IUpgradeCountLimitConfig ONE_PER_STORAGE = new IUpgradeCountLimitConfig() {
        @Override
        public int getMaxUpgradesPerStorage(String storageType, Identifier upgradeId) {
            return 1;
        }

        @Override
        public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup group) {
            return Integer.MAX_VALUE;
        }
    };

    private final Kind kind;
    private final List<UpgradeConflictDefinition> conflicts;

    public StorageUpgradeItem(Properties properties, Kind kind) {
        super(ONE_PER_STORAGE, properties);
        this.kind = kind;
        this.conflicts = List.of(new UpgradeConflictDefinition(
                item -> item instanceof StorageUpgradeItem other && other.kind != kind, 0,
                Component.translatable("dtp.compat.sophisticated.storage_upgrade_conflict")));
    }

    @Override
    public UpgradeType<Wrapper> getType() {
        return kind.type;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return conflicts;
    }

    /** Nur Rucksäcke: Die Lager-Capability gibt es nur für platzierte Rucksäcke, in anderen Speichern wäre es wirkungslos. */
    @Override
    public boolean isCompatibleWith(IStorageWrapper storageWrapper) {
        return storageWrapper instanceof IBackpackWrapper;
    }

    public static class Wrapper extends UpgradeWrapperBase<Wrapper, StorageUpgradeItem> {
        protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
            super(storageWrapper, upgrade, upgradeSaveHandler);
        }
    }
}
