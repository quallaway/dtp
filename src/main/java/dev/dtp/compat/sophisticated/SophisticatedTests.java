package dev.dtp.compat.sophisticated;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Taler;
import dev.dtp.market.Sales;

/**
 * GameTest-Teil der Sophisticated-Backpacks-Anbindung. Wird von {@code DtpGameTests} nur aufgerufen, wenn der Mod
 * geladen ist; so bleiben dessen Klassen aus den Tests heraus, solange er fehlt.
 */
public final class SophisticatedTests {
    private SophisticatedTests() {
    }

    /** Ein platzierter Rucksack zählt erst mit eingesetztem Upgrade als Lager — dann als Trade-Chest bzw. Einzahlkiste. */
    public static void backpackStorage(GameTestHelper helper, BlockPos backpackRel, BlockPos center) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(backpackRel, ModBlocks.BACKPACK.get());
        BackpackBlockEntity backpack = helper.getBlockEntity(backpackRel, BackpackBlockEntity.class);
        backpack.setBackpack(new ItemStack(ModItems.BACKPACK.get()));
        IStorageWrapper wrapper = backpack.getStorageWrapper();
        try {
            wrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.WHEAT, 64));
            wrapper.getInventoryHandler().setStackInSlot(1, new ItemStack(Taler.SILVER_TALER.item(), 1));
            helper.assertTrue(Sales.tradeChests(level, center).isEmpty(), "without an upgrade the backpack is no trade chest");
            helper.assertTrue(DepositChests.find(level, center).chests().isEmpty(), "without an upgrade the backpack is no deposit chest");

            setUpgrade(helper, wrapper, new ItemStack(SophisticatedCompat.TRADE_CHEST_UPGRADE.get()));
            helper.assertTrue(Sales.tradeChests(level, center).size() == 1, "with the trade upgrade the backpack is a trade chest");
            helper.assertTrue(DepositChests.find(level, center).chests().isEmpty(), "the trade upgrade does not make a deposit chest");
            helper.assertTrue(Sales.countForDelivery(level, center, s -> s.is(Items.WHEAT)) == 64, "the market sees the wheat in the backpack");
            int taken = Sales.takeForDelivery(level, center, s -> s.is(Items.WHEAT), 10);
            helper.assertTrue(taken == 10 && wrapper.getInventoryHandler().getStackInSlot(0).getCount() == 54,
                    "taking from the backpack writes back: " + wrapper.getInventoryHandler().getStackInSlot(0));

            setUpgrade(helper, wrapper, new ItemStack(SophisticatedCompat.DEPOSIT_CHEST_UPGRADE.get()));
            helper.assertTrue(Sales.tradeChests(level, center).isEmpty(), "with the deposit upgrade it is no trade chest");
            long silver = Taler.SILVER_TALER.value;
            helper.assertTrue(DepositChests.find(level, center).balance() == silver, "the backpack's taler count as deposit");
            helper.assertTrue(DepositChests.pay(level, center, 1) instanceof DepositChests.Paid, "paying from the backpack works");
            helper.assertTrue(DepositChests.find(level, center).balance() == silver - 1, "the change goes back into the backpack");
        } finally {
            helper.setBlock(backpackRel, Blocks.AIR);
        }
    }

    /**
     * Setzt das Upgrade in Slot 0 — vorher mit genau den Prüfungen, die der Upgrade-Slot beim Einlegen von Hand macht
     * ({@code StorageUpgradeSlot.mayPlace}), damit der Test nicht an ihnen vorbei einsetzt.
     */
    private static void setUpgrade(GameTestHelper helper, IStorageWrapper wrapper, ItemStack upgrade) {
        wrapper.getUpgradeHandler().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(wrapper.getUpgradeHandler().isValid(0, ItemResource.of(upgrade)),
                "the upgrade slot rejects " + upgrade + " (storage wrapper: " + wrapper.getClass().getName() + ")");
        UpgradeSlotChangeResult result = ((StorageUpgradeItem) upgrade.getItem()).canAddUpgradeTo(wrapper, upgrade, true, false);
        helper.assertTrue(result.successful(), "cannot add " + upgrade + ": " + result.getErrorMessage().map(c -> c.getString()).orElse("?"));
        wrapper.getUpgradeHandler().setStackInSlot(0, upgrade);
        wrapper.getUpgradeHandler().refreshUpgradeWrappers();
    }
}
