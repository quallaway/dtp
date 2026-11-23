package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import dev.dtp.Dtp;

/**
 * Spieler-Upgrades für die Tierhaltung: mehr Wolle beim Scheren und schneller wachsende Jungtiere beim Füttern.
 * Beides hängt an der Interaktion des Spielers — gilt also nur, wenn der Spieler schert oder füttert, nicht für
 * Dispenser. Mit Mehr Wolle übernimmt die Mod das Scheren selbst, damit die ganze Wolle direkt ins Inventar geht.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class AnimalUpgrades {
    private AnimalUpgrades() {
    }

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.isCanceled()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (event.getTarget() instanceof Sheep sheep && stack.is(Tags.Items.TOOLS_SHEAR) && sheep.readyForShearing()) {
            if (shear(level, player, sheep, stack, event.getHand())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else if (event.getTarget() instanceof Animal animal && animal.isBaby() && animal.isFood(stack)) {
            growFaster(player, animal);
        }
    }

    /**
     * Schert mit Mehr Wolle wie {@code ShearsItem.interactLivingEntity}: Vanilla-Wolle (per Loot-Table, eingefangen von
     * NeoForges {@link IShearable#onSheared}) und Zusatzwolle gehen direkt ins Inventar, was nicht passt, fällt um den
     * Spieler. Die Schere verliert 1 Haltbarkeit. Ohne Upgrade nichts — dann schert Vanilla wie gewohnt.
     *
     * @return ob geschoren wurde
     */
    public static boolean shear(ServerLevel level, Player player, Sheep sheep, ItemStack shears, InteractionHand hand) {
        BlockPos pos = sheep.blockPosition();
        if (PlayerUpgrades.value(player, PlayerUpgrade.WOOL_BONUS) <= 0 || !(sheep instanceof IShearable target)
                || !target.isShearable(player, shears, level, pos)) {
            return false;
        }
        List<ItemStack> drops = new ArrayList<>(target.onSheared(player, shears, level, pos));
        drops.addAll(extraWool(player, sheep));
        DirectDrops.giveAll(player, drops);
        sheep.gameEvent(GameEvent.SHEAR, player);
        shears.hurtAndBreak(1, player, hand.asEquipmentSlot());
        return true;
    }

    /** Zusätzliche Wolle in der Farbe des Schafs. */
    public static List<ItemStack> extraWool(Player player, Sheep sheep) {
        List<ItemStack> wool = new ArrayList<>();
        int extra = (int) PlayerUpgrades.value(player, PlayerUpgrade.WOOL_BONUS);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(sheep.getColor().getSerializedName() + "_wool"));
        if (extra > 0 && item != Items.AIR) {
            wool.add(new ItemStack(item, extra));
        }
        return wool;
    }

    /**
     * Ein Jungtier wird beim Füttern zusätzlich um einen Anteil seiner Restwachstumszeit älter (Vanilla: 10 %).
     * Bei 100 % ist es sofort erwachsen.
     */
    public static void growFaster(Player player, Animal animal) {
        double share = PlayerUpgrades.value(player, PlayerUpgrade.BABY_GROWTH);
        if (share <= 0 || animal.getAge() >= 0) {
            return;
        }
        int seconds = (int) Math.ceil(-animal.getAge() / 20.0 * Math.min(1.0, share));
        animal.ageUp(seconds, true);
    }
}
