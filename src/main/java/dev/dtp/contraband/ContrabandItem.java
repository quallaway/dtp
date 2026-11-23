package dev.dtp.contraband;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.currency.Money;
import dev.dtp.registry.ModDataComponents;

/**
 * Grundgerüst der Contraband-Items vom Schwarzmarkthändler (Todo v4, Punkt 6). Gemeinsam ist allen:
 * <ul>
 * <li>Rechtsklick löst {@link #activate} aus; nur ein {@link UseOutcome.Used} kostet Ladungen und startet den Cooldown.</li>
 * <li>Ladungen statt Haltbarkeit ({@link Charges}): Bei 0 bleibt das Item erhalten und verweigert die Benutzung,
 * außer bei Verbrauchsitems ({@code consume_when_empty}).</li>
 * <li>Aufladen mit Talern an der Reparaturbank ({@link dev.dtp.repair.RepairStation}), mit Wechselgeld.</li>
 * <li>Nicht verzauberbar und ohne Nutzen von Verzauberungen.</li>
 * </ul>
 * Die Zahlen kommen aus dem Datapack ({@link ContrabandDefinitions}), die Wirkung aus der Unterklasse.
 */
public abstract class ContrabandItem extends Item {
    /** Alle Contraband-Items — für Systeme, die sie erkennen oder ausschließen müssen (z. B. die Mending-Aura). */
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Dtp.MODID, "contraband"));

    private static final int BAR_COLOR = 0x4FC3F7;

    private final ContrabandDefinition defaults;

    protected ContrabandItem(Properties properties, ContrabandDefinition defaults) {
        super(properties.stacksTo(1).component(ModDataComponents.CHARGES.get(), Charges.full(defaults)));
        this.defaults = defaults;
    }

    /** Werte, wenn das Datapack keine Definition für dieses Item mitbringt. */
    public ContrabandDefinition defaults() {
        return defaults;
    }

    public ContrabandDefinition definition() {
        return ContrabandDefinitions.get(this);
    }

    /**
     * Die eigentliche Wirkung, nur auf dem Server. Ladungen und Cooldown regelt {@link #use}. Der Spieler ist im Spiel
     * immer ein {@code ServerPlayer}; nur GameTests reichen einfache Mock-Spieler ohne Verbindung herein.
     */
    protected abstract UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def);

    /**
     * Wirkung bei einem Rechtsklick auf einen Block, nur wenn {@link #usesBlocks()}. Ohne Überschreiben gilt der Klick
     * als Klick in die Luft.
     */
    protected UseOutcome activateOn(ServerLevel level, Player player, ItemStack stack, UseOnContext context, ContrabandDefinition def) {
        return activate(level, player, stack, def);
    }

    /** Ob Klicks auf Blöcke über {@link #activateOn} laufen (z. B. Fackel). Sonst reicht Vanilla sie an {@link #use} weiter. */
    protected boolean usesBlocks() {
        return false;
    }

    /** Sound nach einer erfolgreichen Benutzung; {@code null}, wenn die Wirkung ihren Ton selbst spielt. */
    protected SoundEvent useSound() {
        return SoundEvents.EXPERIENCE_ORB_PICKUP;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        return tryUse(serverLevel, player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!usesBlocks() || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        return tryUse(serverLevel, player, stack, def -> activateOn(serverLevel, player, stack, context, def))
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    /**
     * Der gemeinsame Ablauf einer Benutzung: Schalter, Cooldown, Ladungen, Wirkung, Abzug. Öffentlich, damit
     * GameTests ihn ohne Hand und Netzwerk aufrufen können.
     */
    public boolean tryUse(ServerLevel level, Player player, ItemStack stack) {
        return tryUse(level, player, stack, def -> activate(level, player, stack, def));
    }

    /** Wie {@link #tryUse(ServerLevel, Player, ItemStack)} mit einer anderen Wirkung (Klick auf einen Block). */
    public boolean tryUse(ServerLevel level, Player player, ItemStack stack, Function<ContrabandDefinition, UseOutcome> effect) {
        if (!DtpConfig.CONTRABAND_ENABLED.get()) {
            player.sendOverlayMessage(Component.translatable("dtp.contraband.disabled").withStyle(ChatFormatting.RED));
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        ContrabandDefinition def = definition();
        Charges charges = refresh(stack, def);
        boolean free = player.hasInfiniteMaterials();
        if (!free && charges.charges() < def.costPerUse()) {
            player.sendOverlayMessage(Component.translatable("dtp.contraband.empty").withStyle(ChatFormatting.RED));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
            return false;
        }
        UseOutcome outcome = effect.apply(def);
        if (outcome instanceof UseOutcome.Refused(Component reason)) {
            if (reason != null) {
                player.sendOverlayMessage(reason.copy().withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        if (!free) {
            Charges left = charges.withCharges(charges.charges() - def.costPerUse());
            stack.set(ModDataComponents.CHARGES.get(), left);
            if (left.charges() == 0 && def.consumeWhenEmpty()) {
                stack.shrink(1);
            }
        }
        if (def.cooldownTicks() > 0) {
            player.getCooldowns().addCooldown(stack, def.cooldownTicks());
        }
        SoundEvent sound = useSound();
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    // --- Ladungen ---------------------------------------------------------------------------------

    public static Charges charges(ItemStack stack) {
        Charges charges = stack.get(ModDataComponents.CHARGES.get());
        if (charges != null) {
            return charges;
        }
        return stack.getItem() instanceof ContrabandItem item ? Charges.full(item.definition()) : new Charges(0, 1, 1);
    }

    /** Gleicht Höchstwert und Preis im Stapel an die aktuelle Definition an und liefert die Ladungen. */
    public static Charges refresh(ItemStack stack, ContrabandDefinition def) {
        Charges old = charges(stack);
        Charges now = new Charges(Math.min(old.charges(), def.maxCharges()), def.maxCharges(), def.chargePrice());
        if (!now.equals(old)) {
            stack.set(ModDataComponents.CHARGES.get(), now);
        }
        return now;
    }

    /** Ein neuer Stapel mit {@code charges} Ladungen (z. B. für Händlerangebote). */
    public static ItemStack create(ContrabandItem item, int charges) {
        ItemStack stack = new ItemStack(item);
        ContrabandDefinition def = item.definition();
        stack.set(ModDataComponents.CHARGES.get(), Charges.full(def).withCharges(charges));
        return stack;
    }

    // --- Anzeige ----------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !charges(stack).full();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Charges c = charges(stack);
        return Mth.clamp(Math.round(13.0F * c.charges() / c.max()), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Charges c = charges(stack);
        builder.accept(Component.translatable("dtp.contraband.charges", c.charges(), c.max()).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("dtp.contraband.recharge", Money.format(c.price())).withStyle(ChatFormatting.DARK_GRAY));
    }

    // --- Keine Verzauberungen ---------------------------------------------------------------------

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    /** Auch per Befehl aufgebrachte Verzauberungen wirken nicht. */
    @Override
    public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        return 0;
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        return ItemEnchantments.EMPTY;
    }
}
