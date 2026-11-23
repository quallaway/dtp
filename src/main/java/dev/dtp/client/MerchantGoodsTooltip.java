package dev.dtp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import dev.dtp.Dtp;
import dev.dtp.market.MerchantGoods;

/** Dunkelgraue Zeile „Händlerware“ — schon im Handelsfenster sichtbar, weil die Angebote vor dem Senden markiert werden. */
@EventBusSubscriber(modid = Dtp.MODID, value = Dist.CLIENT)
public final class MerchantGoodsTooltip {
    private MerchantGoodsTooltip() {
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (MerchantGoods.isMarked(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("dtp.merchant_goods.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
