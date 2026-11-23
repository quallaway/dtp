package dev.dtp.registry;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.advancement.UpgradeTrigger;

/**
 * Advancement-Trigger für DTP-Aktionen. Die Advancements selbst sind reine Daten unter {@code data/dtp/advancement/}
 * — Vanilla zeigt sie an, Better Advancements u. ä. ohne jede Anbindung genauso. Ausgelöst über
 * {@link dev.dtp.advancement.DtpAdvancements}.
 */
public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Dtp.MODID);

    /** Verkauf am Marktstand, aus der Hand oder aus Trade-Chests (nicht per Redstone, da fehlt der Spieler). */
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> SOLD = TRIGGERS.register("sold", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> CONTRACT_COMPLETED =
            TRIGGERS.register("contract_completed", PlayerTrigger::new);
    /** Der Hauptbeacon ist bestätigt, der Durchlauf hat begonnen. */
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BEACON_STARTED =
            TRIGGERS.register("beacon_started", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, UpgradeTrigger> PLAYER_UPGRADE =
            TRIGGERS.register("player_upgrade", UpgradeTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, UpgradeTrigger> SERVER_UPGRADE =
            TRIGGERS.register("server_upgrade", UpgradeTrigger::new);

    private ModTriggers() {
    }

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }
}
