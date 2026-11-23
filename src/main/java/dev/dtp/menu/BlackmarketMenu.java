package dev.dtp.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.Blackmarket;
import dev.dtp.blackmarket.BlackmarketData;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;

/**
 * Schwarzmarkt: ein Slot für den verzauberten Gegenstand, einer für den Silberbeutel, darunter das Inventar —
 * im Stil der Einzahlkiste mit Panel daneben. Der Knopf löst den Handel aus; was liegen bleibt, geht beim Schließen zurück.
 */
public class BlackmarketMenu extends TwoSlotMenu {
    private static final Identifier PAYMENT_ICON = Identifier.fromNamespaceAndPath(Dtp.MODID, "container/slot/silver_pouch");

    public BlackmarketMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.BLACKMARKET.get(), id, inventory, pos, ModBlocks.BLACKMARKET.get());
    }

    public static BlackmarketMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BlackmarketMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    protected boolean acceptsItem(ItemStack stack) {
        return Blackmarket.accepts(stack);
    }

    @Override
    protected boolean acceptsPayment(ItemStack stack) {
        return Blackmarket.isPayment(stack);
    }

    @Override
    protected Identifier paymentIcon() {
        return PAYMENT_ICON;
    }

    @Override
    protected Component perform(ServerLevel level, Player player) {
        return Blackmarket.trade(level.getServer(), player, table, level.getRandom());
    }

    @Override
    protected void fill(CompoundTag tag, ServerLevel level) {
        Blackmarket.Blocked blocked = Blackmarket.check(level.getServer(), table.getItem(Blackmarket.SLOT_ITEM),
                table.getItem(Blackmarket.SLOT_PAYMENT));
        if (blocked != null) {
            tag.putString("blocked", blocked.translationKey());
        }
        tag.putBoolean("used_today", BlackmarketData.get(level.getServer()).usedToday(level.getServer()));
        tag.putLong("next_day_ticks", BlackmarketData.ticksUntilNextDay(level.getServer()));
    }
}
