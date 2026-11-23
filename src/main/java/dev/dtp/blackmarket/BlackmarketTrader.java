package dev.dtp.blackmarket;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.dtp.Dtp;

/**
 * Der Schwarzmarkthändler (Todo v4, Punkt 5): ein fahrender Händler, der von Talern in Einzahlkisten angelockt wird.
 * KI, Händleroberfläche und Unsichtbarkeit in der Nacht kommen vom Vanilla-Händler. Die Angebote kommen aus den
 * Datenpack-Sets {@code dtp:blackmarket/*} (Registries {@code trade_set} und {@code villager_trade}); bezahlt wird
 * mit Silberbeuteln.
 *
 * <p>Der Händler verschwindet zu einem festen Spielzeitpunkt ({@link #expiresAt}), auch wenn sein Chunk zwischendurch
 * nicht geladen war. Sonst könnte ein vergessener Händler in einem entladenen Chunk ewig liegen bleiben.
 */
public class BlackmarketTrader extends WanderingTrader {
    public static final List<ResourceKey<TradeSet>> TRADE_SETS = List.of(
            set("blackmarket/crates"), set("blackmarket/contraband"), set("blackmarket/rare"));

    /** Spielzeit (Overworld-Ticks), ab der der Händler verschwindet; 0 = nur Vanilla-Despawn. */
    private long expiresAt;

    public BlackmarketTrader(EntityType<? extends BlackmarketTrader> type, Level level) {
        super(type, level);
    }

    private static ResourceKey<TradeSet> set(String path) {
        return ResourceKey.create(Registries.TRADE_SET, Identifier.fromNamespaceAndPath(Dtp.MODID, path));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = getOffers();
        for (ResourceKey<TradeSet> set : TRADE_SETS) {
            addOffersFromTradeSet(level, offers, set);
        }
    }

    /** Lässt den Händler {@code ticks} Spielticks bleiben. */
    public void stayFor(int ticks) {
        setDespawnDelay(ticks);
        expiresAt = level().getGameTime() + ticks;
    }

    public long expiresAt() {
        return expiresAt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && expiresAt > 0 && level().getGameTime() >= expiresAt && !isTrading()) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("dtp_expires_at", expiresAt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        expiresAt = input.getLongOr("dtp_expires_at", 0L);
    }
}
