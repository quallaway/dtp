package dev.dtp.guide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.registry.ModAttachments;
import dev.dtp.registry.ModItems;

/**
 * Erstes Betreten: Handbuch ins Inventar und die Grundrezepte ins Rezeptbuch. Genau einmal pro Spieler —
 * auch wer schon vor dem Handbuch gespielt hat, bekommt es beim nächsten Login einmal nachgereicht.
 */
public final class Onboarding {
    /**
     * Rezepte, die beim ersten Betreten im Rezeptbuch stehen. Neben-Beacon, Marktwaage und Schwarzmarkt fehlen
     * bewusst — das Handbuch zeigt sie, das Rezeptbuch lässt sie zum Entdecken offen.
     */
    public static final List<String> STARTER_RECIPES = List.of(
            "beacon", "market_stand", "trade_chest", "deposit_chest", "bulletin_board", "upgrade_table", "repair_station", "guide");
    /** Rezeptgruppe der Taler-Umrechnungen; alle stehen ebenfalls von Anfang an im Rezeptbuch. */
    public static final String TALER_GROUP = "dtp_taler";

    private Onboarding() {
    }

    /** Begrüßt den Spieler, falls noch nicht geschehen. Gibt zurück, ob etwas vergeben wurde. */
    public static boolean welcome(ServerPlayer player) {
        if (!DtpConfig.GIVE_GUIDE.get() || player.getData(ModAttachments.ONBOARDED)) {
            return false;
        }
        player.setData(ModAttachments.ONBOARDED, true);
        ItemStack guide = new ItemStack(ModItems.GUIDE.get());
        if (!player.getInventory().add(guide)) {
            player.drop(guide, false, Prediction.SERVER_ONLY);
        }
        List<ResourceKey<Recipe<?>>> recipes = starterRecipes(player.level().getServer());
        if (player.connection == null) {
            // Attrappen aus den GameTests haben keine Verbindung; Vanilla würde beim Freischalten ein Paket senden.
            recipes.forEach(player.getRecipeBook()::add);
        } else {
            player.awardRecipesByKey(recipes);
            player.sendSystemMessage(Component.translatable("dtp.guide.received").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    public static List<ResourceKey<Recipe<?>>> starterRecipes(MinecraftServer server) {
        List<ResourceKey<Recipe<?>>> keys = new ArrayList<>();
        STARTER_RECIPES.forEach(path -> keys.add(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Dtp.MODID, path))));
        server.getRecipeManager().getRecipes().forEach(holder -> {
            if (holder.id().identifier().getNamespace().equals(Dtp.MODID)
                    && holder.value() instanceof CraftingRecipe crafting && TALER_GROUP.equals(crafting.group())) {
                keys.add(holder.id());
            }
        });
        return keys;
    }
}
