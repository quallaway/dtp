package dev.dtp.beacon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Beacon-Effekt Expeditionsvorbereitung (docs/21-beacon-effekte.md): Die Basis ist der Ort, an dem man sich für den Aufbruch
 * rüstet. Im Radius getrunkene Tränke halten länger — auch nach dem Verlassen — und bleiben manchmal erhalten; Essen
 * sättigt stärker. Tränke bleiben Tränke: Der Effekt senkt den Aufwand drumherum, Beacon-Effekte sind nicht betroffen.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ExpeditionPrep {
    private ExpeditionPrep() {
    }

    @SubscribeEvent
    static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !BeaconLogic.effectAt(player.level(), player.blockPosition(), ServerUpgrade.EFFECT_EXPEDITION)) {
            return;
        }
        ItemStack used = event.getItem();
        if (used.is(Items.POTION)) {
            extendPotion(player, used);
            if (player.getRandom().nextInt(100) < DtpConfig.POTION_KEEP_CHANCE.get()) {
                event.setResultStack(used.copy());
            }
        } else {
            FoodProperties food = used.get(DataComponents.FOOD);
            if (food != null) {
                feed(player, food);
            }
        }
    }

    /**
     * Vergibt die Effekte des Tranks noch einmal mit verlängerter Dauer. Vanilla hat sie gerade vergeben; gleiche Stufe
     * mit längerer Dauer ersetzt den Effekt. Sofortwirkungen (Heilung, Schaden) bleiben einmalig.
     */
    public static void extendPotion(Player player, ItemStack potion) {
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        float scale = potion.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0f) * DtpConfig.POTION_DURATION_PERCENT.get() / 100f;
        contents.forEachEffect(effect -> {
            if (!effect.getEffect().value().isInstantaneous()) {
                player.addEffect(new MobEffectInstance(effect));
            }
        }, scale);
    }

    /** Zusätzliche Sättigung, höchstens bis zur Hungerleiste (wie Vanilla). */
    public static void feed(Player player, FoodProperties food) {
        FoodData data = player.getFoodData();
        float extra = food.saturation() * DtpConfig.FOOD_SATURATION_BONUS.get() / 100f;
        data.setSaturation(Math.min(data.getSaturationLevel() + extra, data.getFoodLevel()));
    }
}
