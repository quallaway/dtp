package dev.dtp.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.EnchantedBlockLootEvent;
import net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import dev.dtp.Dtp;
import dev.dtp.registry.ModAttachments;

/**
 * Laufende Wirkungen der Spieler-Upgrades: Attribute, Verzauberungsboni und Module.
 * Der Kettenabbau steht in {@link ChainBreaker}, die Contract-Upgrades in {@code Contracts}.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class PlayerUpgradeEffects {
    private static final Identifier MAX_HEALTH_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "max_health");
    private static final Identifier SWIM_SPEED_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "swim_speed");
    private static final Identifier EFFICIENCY_ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "efficiency_bonus");

    private PlayerUpgradeEffects() {
    }

    /**
     * Setzt die Attribut-Modifier auf den aktuellen Stand und schickt Upgrades und Abklingzeiten an den Client
     * (Tasten und HUD der Module). Permanent, damit die Gesundheit beim Laden nicht auf 20 gekappt wird; beim
     * gewöhnlichen Respawn kopiert Vanilla permanente Modifier aber nicht mit — daher auch nach dem Respawn neu setzen.
     */
    public static void refresh(ServerPlayer player) {
        ModAttachments.sync(player, ModAttachments.PLAYER_UPGRADES);
        ModAttachments.sync(player, ModAttachments.MODULE_COOLDOWNS);
        updateSwimSpeed(player);
        // Effizienz: prozentualer Aufschlag aufs Abbautempo statt zusätzlicher Efficiency-Stufen. Efficiency wirkt
        // quadratisch (Stufe² + 1), schon V → VI hebt Diamant auf 45 = Sofortabbau von Stein (30 × Härte 1,5).
        AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (breakSpeed != null) {
            double bonus = PlayerUpgrades.value(player, PlayerUpgrade.EFFICIENCY_BONUS);
            if (bonus > 0) {
                breakSpeed.addOrReplacePermanentModifier(new AttributeModifier(EFFICIENCY_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else {
                breakSpeed.removeModifier(EFFICIENCY_ID);
            }
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hearts = PlayerUpgrades.value(player, PlayerUpgrade.MAX_HEALTH);
            if (hearts > 0) {
                maxHealth.addOrReplacePermanentModifier(new AttributeModifier(MAX_HEALTH_ID, hearts * 2.0, AttributeModifier.Operation.ADD_VALUE));
            } else {
                maxHealth.removeModifier(MAX_HEALTH_ID);
            }
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * Schwimmtempo gilt nur in der Schwimmhaltung, nicht beim Gehen am Grund. Vanilla multipliziert {@code SWIM_SPEED}
     * nach Depth Strider, ohne diese Bedingung wäre Depth Strider III mit dem Upgrade am Grund schneller als Gehen an Land.
     * Transient und nur bei Änderung gesetzt, sonst ginge jeden Tick ein Attribut-Paket an den Client. Ein permanenter
     * Modifier aus älteren Spielständen hat dieselbe ID und verschwindet beim ersten Tick außerhalb der Schwimmhaltung.
     */
    public static void updateSwimSpeed(Player player) {
        AttributeInstance swim = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim == null) {
            return;
        }
        double bonus = player.isSwimming() ? PlayerUpgrades.value(player, PlayerUpgrade.SWIM_SPEED) : 0.0;
        AttributeModifier current = swim.getModifier(SWIM_SPEED_ID);
        if (bonus > 0) {
            if (current == null || current.amount() != bonus) {
                swim.removeModifier(SWIM_SPEED_ID);
                swim.addTransientModifier(new AttributeModifier(SWIM_SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else if (current != null) {
            swim.removeModifier(SWIM_SPEED_ID);
        }
    }

    /** Nur auf dem Server — die Upgrade-Definitionen kennt der Client nicht; das Attribut wird synchronisiert. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updateSwimSpeed(player);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
            // Vanilla füllt die Gesundheit vor unserem Modifier auf; nach einem Tod mit vollen Herzen starten.
            if (!event.isEndConquered()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    // --- Verzauberungsboni ------------------------------------------------------------------
    // Der Bonus addiert sich zur Verzauberung des Werkzeugs — bewusst auch über das Vanilla-Maximum hinaus.
    // Er gilt nur für Werkzeuge, die die Verzauberung tragen könnten, und wird nie ins Item geschrieben.

    private static int bonus(Player player, PlayerUpgrade upgrade, ItemInstance tool, Holder<Enchantment> enchantment) {
        if (!(tool instanceof ItemStack stack) || !stack.supportsEnchantment(enchantment)) {
            return 0;
        }
        return (int) PlayerUpgrades.value(player, upgrade);
    }

    /** Fortune: bei Block-Loot, abgebaut von einem Spieler. */
    @SubscribeEvent
    static void onBlockLoot(EnchantedBlockLootEvent event) {
        if (event.getEntity() instanceof Player player && event.getEnchantment().is(Enchantments.FORTUNE)) {
            event.setEnchantmentLevel(event.getEnchantmentLevel() + fortuneBonus(player, event.getTool(), event.getEnchantment()));
        }
    }

    /**
     * Fortune-Bonus für das Werkzeug. Mit Silk Touch gibt es keinen: Vanilla schließt beide Verzauberungen aus,
     * und nicht jede Loot-Table (etwa aus anderen Mods) prüft Silk Touch vor Fortune — sonst würde der Bonus
     * den abgebauten Block selbst vervielfachen.
     */
    public static int fortuneBonus(Player player, ItemInstance tool, Holder<Enchantment> fortune) {
        if (tool instanceof ItemStack stack) {
            var silkTouch = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH);
            if (silkTouch.isPresent() && stack.getEnchantmentLevel(silkTouch.get()) > 0) {
                return 0;
            }
        }
        return bonus(player, PlayerUpgrade.FORTUNE_BONUS, tool, fortune);
    }

    /** Looting: bei Mob-Loot, getötet von einem Spieler — auch per Projektil, gezählt wird die Hauptwaffe. */
    @SubscribeEvent
    static void onEntityLoot(EnchantedEntityLootEvent event) {
        if (event.getDamageSource().getEntity() instanceof Player player && event.getEnchantment().is(Enchantments.LOOTING)) {
            int bonus = bonus(player, PlayerUpgrade.LOOTING_BONUS, player.getMainHandItem(), event.getEnchantment());
            event.setEnchantmentLevel(event.getEnchantmentLevel() + bonus);
        }
    }

    /** Wo ein Stapel gerade steckt: bei welchem Spieler, in welchem Slot. */
    public record Holding(Player player, EquipmentSlot slot) {
    }

    /** Sucht den Spieler, der genau diesen Stapel (nicht nur einen gleichen) in der Hand oder als Rüstung trägt. */
    public static Holding holder(ItemStack stack, Iterable<? extends Player> players) {
        for (Player player : players) {
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                if ((slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                        && player.getItemBySlot(slot) == stack) {
                    return new Holding(player, slot);
                }
            }
        }
        return null;
    }

    /**
     * Verzauberungsboni auf getragene Gegenstände. Vanilla fragt die Stufen ab, ohne den Träger mitzuteilen (Angel beim
     * Auswerfen, Haltbarkeit, Schutz) — zugeordnet wird über den Stapel selbst: Lure und Luck of the Sea auf Angeln,
     * Unbreaking auf Hände und Rüstung, Protection nur auf Rüstung. Effizienz ist ein Attribut, siehe {@link #refresh}. Das Event feuert sehr oft; nicht beschädigbare Stapel steigen sofort aus.
     */
    @SubscribeEvent
    static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (!(event.getStack() instanceof ItemStack stack) || stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return;
        }
        Holding holding = holder(stack, server.getPlayerList().getPlayers());
        if (holding != null) {
            addBonuses(event, stack, holding);
        }
    }

    /** Die Boni für einen zugeordneten Stapel — getrennt, damit GameTests ohne Spielerliste prüfen können. */
    public static void addBonuses(GetEnchantmentLevelEvent event, ItemStack stack, Holding holding) {
        Player player = holding.player();
        if (stack.getItem() instanceof FishingRodItem) {
            addBonus(event, stack, player, Enchantments.LURE, PlayerUpgrade.LURE_BONUS);
            addBonus(event, stack, player, Enchantments.LUCK_OF_THE_SEA, PlayerUpgrade.LUCK_OF_THE_SEA_BONUS);
        }
        addBonus(event, stack, player, Enchantments.UNBREAKING, PlayerUpgrade.UNBREAKING_BONUS);
        if (holding.slot().isArmor()) {
            addBonus(event, stack, player, Enchantments.PROTECTION, PlayerUpgrade.PROTECTION_BONUS);
        }
    }

    /** Addiert den Bonus — bewusst auch über das Vanilla-Maximum hinaus, aber nur, wo die Verzauberung passt. */
    private static void addBonus(GetEnchantmentLevelEvent event, ItemStack stack, Player player, ResourceKey<Enchantment> key,
            PlayerUpgrade upgrade) {
        int bonus = (int) PlayerUpgrades.value(player, upgrade);
        if (bonus > 0 && event.isTargetting(key)) {
            event.getHolder(key).filter(stack::supportsEnchantment).ifPresent(holder ->
                    event.getEnchantments().set(holder, event.getEnchantments().getLevel(holder) + bonus));
        }
    }

    // --- Module ------------------------------------------------------------------------------
    // Module lesen ihren Wert wie alle Upgrades über PlayerUpgrades.value — der ist 0, solange das Modul
    // nicht ausgerüstet ist. Eine eigene Slotprüfung braucht es hier nicht.

    /**
     * Rausch: Nach einem Monster-Kill gibt es Speed I für {@code value} Sekunden. Zählt auch Kills per Projektil.
     * Niedrigste Priorität, damit ein von anderer Stelle abgebrochener Tod keinen Rausch auslöst.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof Player player && !player.level().isClientSide()) {
            rush(player);
        }
    }

    /**
     * Wirkung von Rausch für einen Kill; ohne ausgerüstetes Modul nichts. Dauer aus {@code values}, Effektstufen je
     * Stufe aus {@code params} {@code speed} und {@code strength} (wie im Spiel angezeigt, 0 = kein Effekt). Für dieselbe
     * Dauer sammelt der Spieler in größerem Radius auf ({@code params.pickup_range} Blöcke mehr, siehe {@link PickupBoost}).
     */
    public static void rush(Player player) {
        int level = PlayerUpgrades.level(player, PlayerUpgrade.RUSH);
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.RUSH);
        int seconds = (int) PlayerUpgrades.value(player, PlayerUpgrade.RUSH);
        if (level == 0 || def == null || seconds <= 0) {
            return;
        }
        int speed = (int) def.param("speed", level, 1);
        int strength = (int) def.param("strength", level, 0);
        if (speed > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, seconds * 20, speed - 1, false, true));
        }
        if (strength > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, seconds * 20, strength - 1, false, true));
        }
        PickupBoost.grant(player, seconds * 20, def.param("pickup_range", level, 1));
    }

    /**
     * Barriere: Nach einem Treffer im Kampf oder durch Feuer gibt es kurz Absorption I (2 Schildherzen), auf Stufe 2
     * Absorption II mit kürzerer Abklingzeit ({@code params.absorption}, {@code params.cooldown}); die Abklingzeit
     * läuft ab dem Auslösen. Fall, Ertrinken, Hunger und Ähnliches lösen nicht aus — sonst wäre der
     * Schild schon verbraucht, wenn der Kampf beginnt.
     */
    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getInflictedDamage() > 0
                && triggersBarrier(event.getSource())) {
            barrier(player);
        }
    }

    /** Kampfschaden (mit verursachendem Wesen, auch per Projektil oder Mob-Explosion) und Feuer. */
    public static boolean triggersBarrier(DamageSource source) {
        return source.getEntity() != null || source.is(DamageTypeTags.IS_FIRE);
    }

    /**
     * Aufwind: Eine Rakete beim Elytra-Flug bleibt mit {@code params.chance} Prozent erhalten. Das Event feuert vor
     * {@code Item.use}; der Stapel wird vorab um eins erhöht, Vanilla verbraucht danach eins — ohne Mixin.
     */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            updraft(player, event.getItemStack(), player.getRandom().nextInt(100));
        }
    }

    /** @param roll Wurf von 0 bis 99; die Rakete bleibt, wenn er unter der Chance liegt. @return ob die Rakete bleibt */
    public static boolean updraft(Player player, ItemStack stack, int roll) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.UPDRAFT);
        if (def == null || !stack.is(Items.FIREWORK_ROCKET) || !player.isFallFlying() || player.isCreative()
                || PlayerUpgrades.level(player, PlayerUpgrade.UPDRAFT) == 0 || roll >= def.param("chance", 50)) {
            return false;
        }
        stack.grow(1);
        return true;
    }

    /** Wirkung der Barriere für einen auslösenden Treffer; ohne ausgerüstetes Modul oder während der Abklingzeit nichts. */
    public static void barrier(Player player) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.BARRIER);
        int level = PlayerUpgrades.level(player, PlayerUpgrade.BARRIER);
        if (def == null || level == 0 || !ModuleCooldowns.ready(player, PlayerUpgrade.BARRIER)) {
            return;
        }
        int seconds = (int) def.param("duration", level, 10);
        int amplifier = Math.max(0, (int) def.param("absorption", level, 1) - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, seconds * 20, amplifier, false, true));
        ModuleCooldowns.start(player, PlayerUpgrade.BARRIER, def.param("cooldown", level, 60));
    }
}
