package dev.dtp.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import dev.dtp.Dtp;
import dev.dtp.contract.Contract;
import dev.dtp.contract.ContractClock;
import dev.dtp.contract.Contracts;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.registry.ModAttachments;

/**
 * Tooltip der Vanilla-Uhr mit dem Upgrade Auftragsuhr: Preis des gebundenen Items und die angenommenen Contracts.
 *
 * Die persönliche Contract-Uhr läuft auf dem Server weiter; synchronisiert wird nur bei Änderungen und einmal je
 * Tag. Dazwischen wird sie hier mit der Overworld-Uhr des Clients hochgerechnet — derselben Uhr, der der Server folgt.
 */
@EventBusSubscriber(modid = Dtp.MODID, value = Dist.CLIENT)
public final class ContractClockTooltip {
    private static final long TICKS_PER_DAY = 24000L;

    /** Zuletzt gesehener Stand vom Server: Jeder Sync ersetzt das Attachment durch ein neues Objekt. */
    private static PlayerContracts seen;
    private static long seenAt;

    private ContractClockTooltip() {
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (player == null || !ContractClock.isClock(stack) || !ContractClock.hasClockUpgrade(player)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        ItemStack watched = ContractClock.watched(stack);
        if (!watched.isEmpty()) {
            tooltip.add(ContractClock.priceLine(watched, ContractClock.clientPrice(watched)).copy().withStyle(ChatFormatting.GOLD));
        }
        PlayerContracts mine = player.getData(ModAttachments.CONTRACTS);
        if (mine.accepted().isEmpty()) {
            tooltip.add(Component.translatable("dtp.contract.clock.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        double now = now(player.level(), mine);
        Contract.Terms terms = Contracts.terms(player);
        for (Contract contract : mine.accepted()) {
            tooltip.add(ContractClock.line(contract, now, terms).copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** Persönliche Uhr jetzt: letzter Stand vom Server plus die seitdem vergangene Overworld-Zeit. */
    private static double now(Level level, PlayerContracts mine) {
        long ticks = level.getOverworldClockTime();
        if (mine != seen) {
            seen = mine;
            seenAt = ticks;
        }
        return mine.ownDay() + Math.max(0L, ticks - seenAt) / (double) TICKS_PER_DAY;
    }
}
