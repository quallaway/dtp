package dev.dtp.menu;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import dev.dtp.contract.Contract;
import dev.dtp.contract.ContractBoard;
import dev.dtp.contract.Contracts;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.currency.DepositChests;
import dev.dtp.market.MarketData;
import dev.dtp.market.Sales;
import dev.dtp.market.TradeList;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;

/**
 * Bulletin-Board: Aushang und angenommene Contracts. Geliefert wird aus den Trade-Chests in Reichweite.
 * Knopf-IDs: 100+i annehmen, 200+i liefern, 400+i abbrechen.
 *
 * Ein Knopf trägt nur die Zeile i. Aufgelöst wird sie gegen den Stand, den dieser Spieler zuletzt gesehen hat —
 * so trifft ein Klick immer den angezeigten Contract, auch wenn ein Mitspieler die Liste gerade verändert.
 */
public class BulletinBoardMenu extends BlockBoundMenu {
    public static final int ACCEPT = 100;
    public static final int DELIVER = 200;
    public static final int CANCEL = 400;

    private Component message;
    /** Contract-IDs in der Reihenfolge des zuletzt gesendeten Datenstands. */
    private List<UUID> shownOffers = List.of();
    private List<UUID> shownAccepted = List.of();

    public BulletinBoardMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.BULLETIN_BOARD.get(), id, inventory, pos, ModBlocks.BULLETIN_BOARD.get());
        if (player.level() instanceof ServerLevel level) {
            ContractBoard.get(level.getServer()).rotateIfDue(level.getServer());
        }
    }

    public static BulletinBoardMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BulletinBoardMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        int action = buttonId / 100 * 100;
        int index = buttonId % 100;
        List<UUID> shown = action == ACCEPT ? shownOffers : shownAccepted;
        if (index >= shown.size()) {
            message = Component.translatable("dtp.contract.gone");
            sync.markChanged();
            return true;
        }
        UUID id = shown.get(index);
        message = switch (action) {
            case ACCEPT -> Contracts.accept(level, pos, player, id);
            case DELIVER -> Contracts.deliver(level, pos, player, id);
            case CANCEL -> Contracts.cancel(server, player, id);
            default -> null;
        };
        sync.markChanged();
        return message != null;
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ContractBoard board = ContractBoard.get(server);
        board.rotateIfDue(server);
        double day = MarketData.get(server).contractDay();
        int maxTier = ContractBoard.serverMaxTier(server);
        List<Contract> offers = board.offers();
        PlayerContracts mine = Contracts.of(player);
        List<Contract> accepted = mine.accepted();
        shownOffers = offers.stream().map(Contract::id).toList();
        shownAccepted = accepted.stream().map(Contract::id).toList();
        CompoundTag tag = new CompoundTag();
        // Auch Angebote mit den eigenen Bedingungen: Wer Quick Delivery hat, sieht, was er tatsächlich bekäme.
        Contract.Terms terms = Contracts.terms(player);
        tag.put("offers", list(offers, day, terms, maxTier, null));
        // Angenommene Contracts altern auf der persönlichen Uhr — nur, solange ihr Besitzer online ist.
        tag.put("accepted", list(accepted, mine.ownDay(), terms, Integer.MAX_VALUE, level));
        tag.putDouble("rotation_in", board.daysUntilRotation(server));
        tag.putBoolean("outside", !Contracts.usable(level, pos));
        tag.putInt("tier", maxTier);
        tag.putInt("slots", Contracts.slots(player));
        // Feste Fenstergröße: Platz für den voll ausgebauten Aushang und alle möglichen Slots.
        tag.putInt("board_max", ContractBoard.maxBoardSize());
        tag.putInt("slots_max", Contracts.maxSlots());
        tag.putInt("trade_chests", Sales.tradeChests(level, pos).size());
        tag.putInt("deposit_chests", DepositChests.find(level, pos).chests().size());
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }

    /** @param level nur für angenommene Contracts: dann wird der Bestand in den Trade-Chests mitgeschickt */
    private ListTag list(List<Contract> contracts, double day, Contract.Terms terms, int maxTier, ServerLevel level) {
        ListTag list = new ListTag();
        for (Contract c : contracts) {
            CompoundTag e = new CompoundTag();
            e.putString("item", c.item().toString());
            c.key().ifPresent(key -> {
                // Gemeinsame Ware: Gruppenname und alle Mitglieder, damit das Symbol am Aushang durchwechselt.
                e.putString("key", key.toString());
                ListTag members = new ListTag();
                TradeList.members(key).forEach(item -> members.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString())));
                e.put("members", members);
            });
            if (!c.match().isEmpty()) {
                MenuSync.putStack(e, "stack", c.displayStack(player.registryAccess()), player.registryAccess());
                e.putBoolean("min_enchant", c.requiresEnchantments());
                e.putBoolean("undamaged", c.requiresUndamaged());
            }
            e.putInt("count", c.count());
            e.putInt("delivered", c.delivered());
            e.putInt("tier", c.tier());
            e.putLong("reward", c.payout(day, terms));
            e.putDouble("factor", c.factor(day, terms));
            e.putDouble("next_drop", c.daysUntilNextDrop(day, terms));
            e.putDouble("drop_step", c.nextDropStep(day, terms));
            e.putBoolean("in_bonus", day <= c.createdDay() + c.bonusDays());
            e.putBoolean("locked", c.tier() > maxTier);
            if (level != null) {
                e.putInt("in_stock", Sales.countForDelivery(level, pos, c::matches));
            }
            list.add(e);
        }
        return list;
    }
}
