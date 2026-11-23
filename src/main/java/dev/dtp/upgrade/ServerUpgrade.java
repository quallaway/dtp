package dev.dtp.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Die Server-Upgrades aus v1. Hier steht, was ein Upgrade <em>tut</em> (siehe die Verwender);
 * Kosten und Stufenzahl kommen aus dem Datapack ({@link UpgradeCosts}).
 */
public enum ServerUpgrade {
    BORDER("border"),
    CONTRACT_SLOTS("contract_slots"),
    CONTRACT_TIER("contract_tier"),
    PRICE_STABILITY("price_stability"),
    PRICE_RECOVERY("price_recovery"),
    // Hebt die Würfelchancen für höhere Contract-Stufen an (ContractBoard.tierChance).
    CONTRACT_QUALITY("contract_quality"),
    MARKET_INFO("market_info"),
    DEEPSLATE("deepslate"),
    NETHER_ACCESS("nether_access"),
    EFFECT_SLOTS("effect_slots"),
    EFFECT_JUMP_BOOST("effect_jump_boost", MobEffects.JUMP_BOOST),
    EFFECT_NIGHT_VISION("effect_night_vision", MobEffects.NIGHT_VISION),
    EFFECT_WATER_BREATHING("effect_water_breathing", MobEffects.WATER_BREATHING),
    EFFECT_RESISTANCE("effect_resistance", MobEffects.RESISTANCE),
    // Eigene Beacon-Effekte ohne Vanilla-Statuseffekt: wirken über Events im Radius (siehe AnimalHusbandry, Farming).
    // Landwirtschaft: Trampelschutz, Pflanzenwachstum. Hieß bis WorldProgress-Version 3 effect_breeding.
    EFFECT_AGRICULTURE("effect_agriculture", Items.WHEAT),
    // Stallmeister: Zuchtpause, Fohlen, Reittiere, Straßenbonus. Bis WorldProgress-Version 4 effect_horse_breeding
    // (plus effect_roads, das darin aufgegangen ist).
    EFFECT_STABLEMASTER("effect_stablemaster", Items.SADDLE),
    EFFECT_BEEKEEPING("effect_beekeeping", Items.HONEYCOMB),
    // Bauen und Expedition (siehe BuilderEffects, Rebuild, ExpeditionPrep).
    // Baumeister: Reichweite und Rückbau (bis WorldProgress-Version 4 eigener Effekt effect_rebuild).
    EFFECT_BUILDER("effect_builder", Items.SCAFFOLDING),
    EFFECT_FEATHER_FALL("effect_feather_fall", Items.FEATHER),
    EFFECT_QUIET_NIGHTS("effect_quiet_nights", Items.PHANTOM_MEMBRANE),
    EFFECT_EXPEDITION("effect_expedition", Items.BREWING_STAND),
    // Server-Upgrades ohne Slot. Weltweit, ohne Ort: Griefing (ServerRules) und Meisterschmied (AnvilCap).
    // Im Beacon-Gebiet (BeaconLogic.inBeaconArea: Hauptbeacon und jeder Neben-Beacon, ohne Auswahl): Ofentempo
    // (ServerUpgradeValue mit "local") und Mending-Aura (BeaconLogic.applyMendingAura).
    ENDERMAN_GRIEFING("enderman_griefing"),
    CREEPER_GRIEFING("creeper_griefing"),
    FURNACE_SPEED("furnace_speed"),
    MENDING_AURA("mending_aura"),
    ANVIL_CAP("anvil_cap");

    public final String id;
    /** Für Vanilla-Effekte der Statuseffekt, den der Beacon an Spieler verteilt, sonst null. */
    public final Holder<MobEffect> effect;
    /** Für eigene Beacon-Effekte das Symbol in der Effektauswahl, sonst null. */
    public final Item icon;

    ServerUpgrade(String id) {
        this.id = id;
        this.effect = null;
        this.icon = null;
    }

    ServerUpgrade(String id, Holder<MobEffect> effect) {
        this.id = id;
        this.effect = effect;
        this.icon = null;
    }

    ServerUpgrade(String id, Item icon) {
        this.id = id;
        this.effect = null;
        this.icon = icon;
    }

    /** Ein Beacon-Effekt: belegt einen Effektslot, wählbar am Haupt- und Neben-Beacon. */
    public boolean isEffect() {
        return effect != null || icon != null;
    }

    /** Ein Beacon-Effekt, der als Vanilla-Statuseffekt an Spieler im Radius verteilt wird. */
    public boolean isVanillaEffect() {
        return effect != null;
    }

    public String translationKey() {
        return "upgrade.dtp." + id;
    }

    public static ServerUpgrade byId(String id) {
        for (ServerUpgrade u : values()) {
            if (u.id.equals(id)) {
                return u;
            }
        }
        return null;
    }
}
