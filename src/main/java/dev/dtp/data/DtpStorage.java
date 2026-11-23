package dev.dtp.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import dev.dtp.Dtp;
import dev.dtp.registry.ModBlocks;

/**
 * Die Lager-Schnittstelle von DTP: Woran Marktstand, Bulletin-Board, Werkstatt und Beacon ihre Trade-Chests und
 * Einzahlkisten erkennen. Sie fragen nicht nach einer Blockklasse, sondern nach einer Capability. Jeder Block, der
 * sie anbietet, zählt als Lager — die eigenen Kisten genauso wie Blöcke aus anderen Mods (siehe
 * {@code dev.dtp.compat}). Der Kern kennt dadurch keinen fremden Mod.
 *
 * <p>Der Typ ist ein {@link Container}. Wer sie anbietet, muss Änderungen an Stapeln aus {@link Container#getItem}
 * spätestens bei {@link Container#setChanged()} übernehmen; DTP ruft das nach jeder Änderung auf. Für Einzahlkisten
 * muss {@link Container#canPlaceItem} Nicht-Taler ablehnen.
 */
public final class DtpStorage {
    private DtpStorage() {
    }

    /** Warenlager für Verkauf am Marktstand und Lieferungen am Bulletin-Board. */
    public static final BlockCapability<Container, Void> TRADE =
            BlockCapability.createVoid(Identifier.fromNamespaceAndPath(Dtp.MODID, "trade_storage"), Container.class);

    /** Geldlager für Käufe und Erlöse. Nimmt nur Taler an. */
    public static final BlockCapability<Container, Void> DEPOSIT =
            BlockCapability.createVoid(Identifier.fromNamespaceAndPath(Dtp.MODID, "deposit_storage"), Container.class);

    /** Ein gefundenes Lager: wo es steht und sein Inhalt. */
    public record Found(BlockPos pos, Container container) {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DtpStorage::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(TRADE, ModBlocks.TRADE_CHEST_BE.get(), (chest, context) -> chest);
        event.registerBlockEntity(DEPOSIT, ModBlocks.DEPOSIT_CHEST_BE.get(), (chest, context) -> chest);
    }
}
