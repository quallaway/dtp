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
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.repair.RepairStation;

/**
 * Reparaturbank: ein Slot für das beschädigte Werkzeug oder Rüstungsteil (oder ein Contraband-Item zum Aufladen), einer
 * für die Taler. Das Panel zeigt Kurs, Preis der vollen Reparatur und was das eingelegte Geld bewirkt; der Knopf
 * repariert bzw. lädt auf.
 */
public class RepairStationMenu extends TwoSlotMenu {
    private static final Identifier PAYMENT_ICON = Identifier.fromNamespaceAndPath(Dtp.MODID, "container/slot/taler");

    public RepairStationMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.REPAIR_STATION.get(), id, inventory, pos, ModBlocks.REPAIR_STATION.get());
    }

    public static RepairStationMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new RepairStationMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    protected boolean acceptsItem(ItemStack stack) {
        return RepairStation.accepts(stack);
    }

    @Override
    protected boolean acceptsPayment(ItemStack stack) {
        return RepairStation.isPayment(stack);
    }

    @Override
    protected Identifier paymentIcon() {
        return PAYMENT_ICON;
    }

    @Override
    protected Component perform(ServerLevel level, Player player) {
        return RepairStation.repair(level.getServer(), player, table);
    }

    @Override
    protected void fill(CompoundTag tag, ServerLevel level) {
        ItemStack item = table.getItem(RepairStation.SLOT_ITEM);
        ItemStack payment = table.getItem(RepairStation.SLOT_PAYMENT);
        RepairStation.Blocked blocked = RepairStation.check(item, payment);
        if (blocked != null) {
            tag.putString("blocked", blocked.translationKey());
        }
        if (RepairStation.accepts(item)) {
            tag.putBoolean("contraband", RepairStation.isContraband(item));
            tag.putLong("price_per_point", RepairStation.unitPriceMilli(item));
            tag.putLong("full_price", RepairStation.fullPrice(item));
        }
        if (blocked == null) {
            RepairStation.Quote quote = RepairStation.quote(item, payment);
            tag.putInt("points", quote.points());
            tag.putLong("cost", quote.cost());
            tag.putLong("change", quote.change());
            tag.putBoolean("full", quote.full());
        }
    }
}
