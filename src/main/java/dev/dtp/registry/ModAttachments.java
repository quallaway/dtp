package dev.dtp.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dev.dtp.Dtp;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.upgrade.ModuleCooldowns;
import dev.dtp.upgrade.PlayerUpgrades;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Dtp.MODID);

    // copyOnDeath: Contract-Fortschritt muss den Tod überleben — in NeoForge eine bewusste Einstellung.
    // Synchronisiert an den eigenen Client: Der Tooltip der Auftragsuhr zeigt die angenommenen Contracts.
    public static final Supplier<AttachmentType<PlayerContracts>> CONTRACTS = ATTACHMENTS.register("contracts",
            () -> AttachmentType.builder(PlayerContracts::new)
                    .serialize(PlayerContracts.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(ModAttachments::ownClient, ByteBufCodecs.fromCodecWithRegistries(PlayerContracts.CODEC))
                    .build());

    // Spieler-Upgrades sind dauerhafter persönlicher Fortschritt und überleben den Tod ebenfalls.
    // Synchronisiert an den eigenen Client: Tasten und HUD der Module müssen wissen, was ausgerüstet ist.
    public static final Supplier<AttachmentType<PlayerUpgrades>> PLAYER_UPGRADES = ATTACHMENTS.register("player_upgrades",
            () -> AttachmentType.builder(PlayerUpgrades::new)
                    .serialize(PlayerUpgrades.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(ModAttachments::ownClient, ByteBufCodecs.fromCodecWithRegistries(PlayerUpgrades.CODEC))
                    .build());

    // Abklingzeiten der Module: überleben Tod und Relog, synchronisiert für die Anzeige im HUD.
    public static final Supplier<AttachmentType<ModuleCooldowns>> MODULE_COOLDOWNS = ATTACHMENTS.register("module_cooldowns",
            () -> AttachmentType.builder(ModuleCooldowns::new)
                    .serialize(ModuleCooldowns.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(ModAttachments::ownClient, ByteBufCodecs.fromCodecWithRegistries(ModuleCooldowns.CODEC))
                    .build());

    // Handbuch und Grundrezepte wurden schon vergeben (docs/01-mod-und-vision.md, Einstieg). Überlebt den Tod,
    // sonst gäbe es nach jedem Tod ein neues Buch.
    public static final Supplier<AttachmentType<Boolean>> ONBOARDED = ATTACHMENTS.register("onboarded",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("done"))
                    .copyOnDeath()
                    .build());

    /**
     * Nur an den eigenen Client. Attrappen aus den GameTests haben keine Verbindung — NeoForge synchronisiert schon
     * beim ersten Zugriff, ohne diese Prüfung bräche jeder Test mit einem Mock-Spieler ab.
     */
    private static boolean ownClient(IAttachmentHolder holder, ServerPlayer to) {
        return holder == to && to.connection != null;
    }

    /**
     * Schickt ein verändertes Attachment an den Client. Attrappen aus den GameTests haben keine Verbindung —
     * für sie entfällt das Senden.
     */
    public static void sync(Player player, Supplier<? extends AttachmentType<?>> type) {
        if (player instanceof ServerPlayer serverPlayer
                && (serverPlayer.connection == null || serverPlayer.connection.getConnection() == null)) {
            return;
        }
        player.syncData(type);
    }

    private ModAttachments() {
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
