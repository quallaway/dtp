package dev.dtp.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import dev.dtp.Dtp;
import dev.dtp.upgrade.ModuleCooldowns;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.Skills;

/**
 * Die eine Fähigkeitstaste (Steuerung → „DTP — Fähigkeiten“) für den ausgerüsteten Skill, dazu der Doppelsprung über die Sprungtaste
 * in der Luft. Gesendet wird nur mit ausgerüstetem Skill; ob der Einsatz gilt, entscheidet der Server ({@link Skills}).
 */
@EventBusSubscriber(modid = Dtp.MODID, value = Dist.CLIENT)
public final class SkillKeys {
    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Dtp.MODID, "skills"));

    static final KeyMapping SKILL = new KeyMapping("key.dtp.skill", InputConstants.KEY_R, CATEGORY);

    /** Sprungtaste im letzten Tick gedrückt — ausgelöst wird beim neuen Drücken, nicht beim Halten. */
    private static boolean jumpWasDown;
    /** In dieser Luftphase schon doppelt gesprungen (Gegenstück zur Prüfung auf dem Server). */
    private static boolean airJumped;
    /** Sprungtaste seit dem Abheben einmal losgelassen. */
    private static boolean jumpReleasedInAir;

    private SkillKeys() {
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(SKILL);
    }

    /** Wie der Skill ausgelöst wird, für Tooltips: Fähigkeitstaste bzw. Sprungtaste. */
    static Component trigger(PlayerUpgrade skill) {
        KeyMapping key = skill == PlayerUpgrade.DOUBLE_JUMP ? Minecraft.getInstance().options.keyJump : SKILL;
        return key.getTranslatedKeyMessage();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        while (SKILL.consumeClick()) {
            if (mc.gui.screen() == null && mine.equippedSkill() != null) {
                send();
            }
        }
        doubleJump(mc, player, mine);
    }

    private static void doubleJump(Minecraft mc, LocalPlayer player, PlayerUpgrades mine) {
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean pressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;
        if (player.onGround()) {
            airJumped = false;
            jumpReleasedInAir = false;
            return;
        }
        // Der normale Absprung drückt die Taste im selben Tick, in dem der Spieler abhebt — zählt erst, wenn die
        // Taste in der Luft einmal losgelassen wurde.
        if (!jumpDown) {
            jumpReleasedInAir = true;
        }
        if (!pressed || !jumpReleasedInAir || airJumped || mc.gui.screen() != null || mine.equippedSkill() != PlayerUpgrade.DOUBLE_JUMP
                || player.getAbilities().flying || player.isInWater() || player.onClimbable() || player.isPassenger()
                || player.isFallFlying() || !ModuleCooldowns.ready(player, PlayerUpgrade.DOUBLE_JUMP)) {
            return;
        }
        airJumped = true;
        send();
    }

    private static void send() {
        ClientPacketDistributor.sendToServer(new Skills.Payload());
    }
}
