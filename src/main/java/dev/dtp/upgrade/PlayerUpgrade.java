package dev.dtp.upgrade;

/**
 * Die Spieler-Upgrades, gekauft an der Werkstatt. Hier steht, was ein Upgrade <em>ist</em> — auch seine Art
 * ({@link Kind}); Kosten, Stufen, Wirkungswerte, Position im Baum und Voraussetzungen kommen aus dem Datapack
 * ({@link PlayerUpgradeDefinitions}).
 */
public enum PlayerUpgrade {
    CONTRACT_CAPACITY("contract_capacity"),
    QUICK_DELIVERY("quick_delivery"),
    MAX_HEALTH("max_health"),
    LOOTING_BONUS("looting_bonus"),
    FORTUNE_BONUS("fortune_bonus"),
    LUCK_OF_THE_SEA_BONUS("luck_of_the_sea_bonus"),
    LURE_BONUS("lure_bonus"),
    TREE_FELLING("tree_felling"),
    VEIN_MINING("vein_mining"),
    MODULE_SLOTS("module_slots"),
    RUSH("rush", Kind.MODULE),
    BARRIER("barrier", Kind.MODULE),
    AREA_MINING("area_mining", Kind.MODULE),
    UPDRAFT("updraft", Kind.MODULE),
    DASH("dash", Kind.SKILL),
    DOUBLE_JUMP("double_jump", Kind.SKILL),
    GUARD("guard", Kind.SKILL),
    HEAL_PULSE("heal_pulse", Kind.SKILL),
    WOOL_BONUS("wool_bonus"),
    BABY_GROWTH("baby_growth"),
    HOE_HARVEST("hoe_harvest"),
    HARVEST_BONUS("harvest_bonus"),
    SWIM_SPEED("swim_speed"),
    UNBREAKING_BONUS("unbreaking_bonus"),
    EFFICIENCY_BONUS("efficiency_bonus"),
    PROTECTION_BONUS("protection_bonus"),
    FISHING_TREASURE("fishing_treasure"),
    DOUBLE_BOBBERS("double_bobbers"),
    FISHING_NET("fishing_net", Kind.SKILL),
    CONTRACT_CLOCK("contract_clock"),
    REMOTE_DELIVERY("remote_delivery");

    /**
     * Passiv wirkt, sobald gekauft. Ein Modul wirkt nur, solange es in einem persönlichen Modulslot steckt —
     * für Wirkungen, die nicht in jeder Spielphase erwünscht sind. Ein Skill ist ein Modul, das per Taste ausgelöst
     * wird (nur Abklingzeit, siehe {@link Skills}); höchstens einer ist ausgerüstet. Die Art steht bewusst im Code: ein Datapack soll ein
     * passives Upgrade nicht stillschweigend in ein Modul verwandeln.
     */
    public enum Kind {
        PASSIVE,
        MODULE,
        SKILL
    }

    public final String id;
    public final Kind kind;

    PlayerUpgrade(String id) {
        this(id, Kind.PASSIVE);
    }

    PlayerUpgrade(String id, Kind kind) {
        this.id = id;
        this.kind = kind;
    }

    /** Belegt einen Modulslot — automatische Module und Skills. */
    public boolean isModule() {
        return kind != Kind.PASSIVE;
    }

    public boolean isSkill() {
        return kind == Kind.SKILL;
    }

    public String translationKey() {
        return "player_upgrade.dtp." + id;
    }

    public static PlayerUpgrade byId(String id) {
        for (PlayerUpgrade u : values()) {
            if (u.id.equals(id)) {
                return u;
            }
        }
        return null;
    }
}
