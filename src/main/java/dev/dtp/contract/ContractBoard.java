package dev.dtp.contract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.data.Versioned;
import dev.dtp.market.MarketData;
import dev.dtp.market.TradeList;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.UpgradeCosts;
import dev.dtp.upgrade.WorldProgress;

/**
 * Der serverweite Aushang am Bulletin-Board. Angenommene Contracts wandern an den Spieler;
 * abgebrochene kehren ohne Lieferfortschritt hierher zurück.
 *
 * Der Aushang rotiert in festen Abständen: Alle unangenommenen Angebote werden durch neue ersetzt.
 * Dazwischen wird nicht nachgefüllt — ein angenommenes Angebot hinterlässt eine Lücke bis zur nächsten
 * Rotation.
 */
public class ContractBoard extends SavedData {
    private static final Codec<ContractBoard> INNER = RecordCodecBuilder.create(i -> i.group(
            Contract.CODEC.listOf().optionalFieldOf("offers", List.of()).forGetter(d -> d.offers),
            Codec.DOUBLE.optionalFieldOf("next_rotation", 0.0).forGetter(d -> d.nextRotation),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(d -> d.rotation)
    ).apply(i, ContractBoard::new));

    public static final Codec<ContractBoard> CODEC = Versioned.codec(1, INNER, List.of());

    public static final SavedDataType<ContractBoard> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "contract_board"), ContractBoard::new, CODEC, (DataFixTypes) null);

    private final List<Contract> offers;
    /** Marktuhr-Zeitpunkt der nächsten Rotation; 0 = noch nie rotiert. */
    private double nextRotation;
    /** Zählt jedes Neuwürfeln; die Flagge eines Boards steht, solange es eine neuere Rotation noch nicht gezeigt hat. */
    private int rotation;

    public ContractBoard() {
        this(List.of(), 0.0, 0);
    }

    private ContractBoard(List<Contract> offers, double nextRotation, int rotation) {
        this.offers = new ArrayList<>(offers);
        this.nextRotation = nextRotation;
        this.rotation = rotation;
    }

    public static ContractBoard get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Contract> offers() {
        return offers;
    }

    /** Nummer der aktuellen Rotation, für die Flagge der Boards. */
    public int rotation() {
        return rotation;
    }

    public Optional<Contract> take(UUID id) {
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).id().equals(id)) {
                setDirty();
                return Optional.of(offers.remove(i));
            }
        }
        return Optional.empty();
    }

    /** Abbruch: ohne Lieferfortschritt zurück als gewöhnliches Angebot, das die nächste Rotation ersetzt. */
    public void returnCancelled(Contract contract) {
        offers.add(contract.asPlainOffer());
        setDirty();
    }

    /** Tage bis zur nächsten Rotation, für die Anzeige. */
    public double daysUntilRotation(MinecraftServer server) {
        return Math.max(0.0, nextRotation - MarketData.get(server).contractDay());
    }

    /**
     * Rotiert den Aushang, falls fällig: unangenommene Angebote raus, neue rein. Wird beim Öffnen eines
     * Boards und bei jedem Datenstand geprüft — dazwischen braucht der Aushang keine Uhr.
     *
     * @return ob rotiert wurde
     */
    public boolean rotateIfDue(MinecraftServer server) {
        double day = MarketData.get(server).contractDay();
        // Ohne freigeschaltete Stufe hängt nichts aus — auch keine Angebote aus einem älteren Spielstand.
        if (serverMaxTier(server) < 1 && !offers.isEmpty()) {
            offers.clear();
            setDirty();
        }
        if (nextRotation > 0.0 && day < nextRotation) {
            return false;
        }
        reroll(server);
        return true;
    }

    /** Würfelt den Aushang sofort neu, etwa nach dem Kauf einer Contract-Stufe; die Rotationsfrist beginnt neu. */
    public void reroll(MinecraftServer server) {
        double day = MarketData.get(server).contractDay();
        offers.clear();
        generate(server, day);
        nextRotation = day + DtpConfig.ROTATION_DAYS.get();
        rotation++;
        setDirty();
    }

    /** Angebote pro Rotation: Grundgröße plus Server-Upgrade. */
    public static int boardSize(MinecraftServer server) {
        return DtpConfig.BOARD_SIZE.get() + WorldProgress.get(server).level(ServerUpgrade.CONTRACT_SLOTS);
    }

    /** Größter mögliche Aushang: Grundgröße plus das voll ausgebaute Server-Upgrade. */
    public static int maxBoardSize() {
        UpgradeCosts.Definition def = UpgradeCosts.get(ServerUpgrade.CONTRACT_SLOTS);
        return DtpConfig.BOARD_SIZE.get() + (def == null ? 0 : def.maxLevel());
    }

    /** Höchste Stufe, die der Server derzeit zulässt; 0 = Contracts sind noch nicht freigeschaltet. */
    public static int serverMaxTier(MinecraftServer server) {
        return DtpConfig.BASE_MAX_TIER.get() + WorldProgress.get(server).level(ServerUpgrade.CONTRACT_TIER);
    }

    /**
     * Würfelt neue Angebote, bis der Aushang voll ist. Zuerst die garantierten Plätze je freigeschalteter Stufe
     * (reihum, damit bei knappem Platz jede Stufe vertreten ist), dann die freien Plätze nach
     * {@link #tierChance} (Config plus Server-Upgrade Contract-Qualität). Zwei Angebote fordern nie dasselbe. Das Ergebnis ist nach Stufe sortiert.
     * Ohne freigeschaltete Stufe bleibt der Aushang leer.
     */
    private void generate(MinecraftServer server, double day) {
        int size = boardSize(server);
        int maxTier = serverMaxTier(server);
        if (maxTier < 1) {
            return;
        }
        RandomSource random = server.overworld().getRandom();
        int guaranteed = DtpConfig.GUARANTEED_PER_TIER.get();
        int attempts = 0;
        while (offers.size() < size && attempts++ < 50) {
            int slot = offers.size();
            int tier = slot < guaranteed * maxTier ? 1 + slot % maxTier : randomTier(server, random, maxTier);
            if (!add(server, tier, maxTier, random, day)) {
                break;
            }
        }
        offers.sort(Comparator.comparingInt(Contract::tier));
    }

    /** Von der höchsten Stufe abwärts je ein Wurf mit {@link #tierChance}; schlägt alles fehl, Stufe 1. */
    private static int randomTier(MinecraftServer server, RandomSource random, int maxTier) {
        for (int tier = maxTier; tier > 1; tier--) {
            if (random.nextDouble() < tierChance(server, tier)) {
                return tier;
            }
        }
        return 1;
    }

    /**
     * Würfelchance für einen freien Platz der Stufe {@code tier} (ab 2): {@link DtpConfig#TIER_CHANCE} plus je Stufe
     * von {@link ServerUpgrade#CONTRACT_QUALITY} {@link DtpConfig#TIER_CHANCE_PER_LEVEL}, höchstens 1.
     */
    public static double tierChance(MinecraftServer server, int tier) {
        double base = DtpConfig.listValue(DtpConfig.TIER_CHANCE.get(), tier - 2, 0.5);
        int level = WorldProgress.get(server).level(ServerUpgrade.CONTRACT_QUALITY);
        return Math.min(1.0, base + level * DtpConfig.TIER_CHANCE_PER_LEVEL.get());
    }

    /**
     * Fügt ein Angebot der gewünschten Stufe hinzu; sind dort alle Anforderungen schon vergeben, eines einer
     * anderen freigeschalteten Stufe. Dasselbe Item mit verschiedenen Bedingungen (Mending-Buch, beliebiges Buch)
     * darf gleichzeitig aushängen, dieselbe Ware in verschiedenen Stufen nicht. Vorlagen mit Freischaltung
     * ({@code unlock}) bleiben bis dahin außen vor.
     *
     * @return false, wenn es gar keine passende Vorlage mehr gibt
     */
    private boolean add(MinecraftServer server, int tier, int maxTier, RandomSource random, double day) {
        HolderLookup.Provider registries = server.registryAccess();
        Set<String> used = new HashSet<>();
        offers.forEach(c -> used.add(c.requirement(registries)));
        // Je Zufallsvorlage höchstens ein Angebot: Ihre Angebote tragen die gewürfelte Bedingung, nicht den Vorlagenschlüssel.
        ContractTemplates.all().stream()
                .filter(t -> t.isRandom() && offers.stream().anyMatch(c -> t.produced(c, registries)))
                .forEach(t -> used.add(t.requirement(registries)));
        List<ContractTemplates.Template> open = ContractTemplates.all().stream()
                .filter(t -> !used.contains(t.requirement(registries)) && t.unlock().test(server))
                .toList();
        List<ContractTemplates.Template> candidates = open.stream().filter(t -> t.hasTier(tier)).toList();
        int chosenTier = tier;
        if (candidates.isEmpty()) {
            candidates = open.stream().filter(t -> t.lowestTier() <= maxTier).toList();
            chosenTier = -1;
        }
        if (candidates.isEmpty()) {
            return false;
        }
        ContractTemplates.Template template = pickWeighted(candidates, random);
        if (chosenTier < 0) {
            List<Integer> fitting = template.tiers().stream().filter(t -> t <= maxTier).toList();
            chosenTier = fitting.get(random.nextInt(fitting.size()));
        }
        create(template, chosenTier, random, day, registries).ifPresent(offers::add);
        return true;
    }

    /**
     * Würfelt Menge und Belohnung. Ohne feste Menge in der Vorlage folgt die Menge aus dem Wertziel der Stufe:
     * Marktwert {@code contracts.tierValue} ± {@code contracts.tierValueSpread}, geteilt durch den Basiswert —
     * so wächst dieselbe Ware von Stufe zu Stufe mit, und teure Waren werden in kleinerer Zahl gefordert.
     * Zufallsvorlagen ({@code random_enchantments}) würfeln hier ihre Bedingung; deren Werte kommen auf
     * {@code reward_per_item} drauf. Zufallstränke ({@code random_potion}) rechnen Wert und Lohn aus der Tranktabelle.
     */
    public static Optional<Contract> create(ContractTemplates.Template template, int tier, RandomSource random, double day,
            HolderLookup.Provider registries) {
        if (template.randomPotion().isPresent()) {
            return createPotion(template, tier, random, day, registries);
        }
        DataComponentMatchers match = template.match();
        double extra = 0;
        if (template.randomEnchantments().isPresent()) {
            Optional<ContractTemplates.Rolled> rolled = template.randomEnchantments().get().roll(template.item(), registries, random);
            if (rolled.isEmpty()) {
                return Optional.empty();
            }
            match = rolled.get().match();
            extra = rolled.get().value();
        }
        Optional<TradeList.Entry> entry = TradeList.lookup(BuiltInRegistries.ITEM.getValue(template.item()));
        int count;
        if (template.fixedCount()) {
            int min = template.fixedMin();
            int max = template.fixedMax();
            count = min + random.nextInt(max - min + 1);
        } else if (entry.isPresent() && entry.get().base() > 0) {
            double spread = DtpConfig.TIER_VALUE_SPREAD.get();
            double value = DtpConfig.listValue(DtpConfig.TIER_VALUE.get(), tier - 1, 700.0) * (1.0 + spread * (2.0 * random.nextDouble() - 1.0));
            count = roundCount(value / entry.get().base());
        } else {
            Dtp.LOGGER.warn("Contract template for {} has no fixed count and no trade list entry", template.item());
            return Optional.empty();
        }
        double perItem;
        if (template.rewardPerItem().isPresent()) {
            perItem = template.rewardPerItem().get() + extra;
        } else if (entry.isPresent()) {
            // Fixiert über dem neutralen Marktwert — sonst gäbe es keinen Grund, den Contract vorzuziehen.
            // Höhere Stufen zahlen je Item mehr, weil sie Farmen und Zusammenarbeit verlangen.
            perItem = entry.get().base() * DtpConfig.listValue(DtpConfig.REWARD_MULTIPLIER.get(), tier - 1, 2.0);
        } else {
            Dtp.LOGGER.warn("Contract template for {} has no reward_per_item and no trade list entry", template.item());
            return Optional.empty();
        }
        long reward = Math.max(1, Math.round(perItem * count));
        return Optional.of(Contract.offer(template.item(), template.key(), match, count, reward, tier, day));
    }

    /**
     * Trank-Contract: Menge aus dem Wertziel der Stufe und dem Wert je Trank. Weil Tränke nicht stapeln, ist die Menge
     * gedeckelt — liegt der Wurf darüber, wird {@code capped_min}–{@code max_count} gewürfelt. Lohn je Trank =
     * Wert × {@code reward_multiplier} + {@code surcharge} für Brauzeit und Platz.
     */
    private static Optional<Contract> createPotion(ContractTemplates.Template template, int tier, RandomSource random, double day,
            HolderLookup.Provider registries) {
        Optional<ContractTemplates.RolledPotion> rolled = template.randomPotion().get().roll(registries, random);
        if (rolled.isEmpty() || rolled.get().value() <= 0) {
            Dtp.LOGGER.warn("Contract template for {}: no potion could be rolled", template.item());
            return Optional.empty();
        }
        ContractTemplates.PotionValues table = ContractTemplates.potionValues();
        double value = rolled.get().value();
        double spread = DtpConfig.TIER_VALUE_SPREAD.get();
        double target = DtpConfig.listValue(DtpConfig.TIER_VALUE.get(), tier - 1, 700.0) * (1.0 + spread * (2.0 * random.nextDouble() - 1.0));
        int count = roundCount(target / value);
        if (count > table.maxCount()) {
            count = cappedCount(table.cappedMin(), table.maxCount(), random);
        }
        long reward = Math.max(1, Math.round(table.reward(value) * count));
        return Optional.of(Contract.offer(rolled.get().item(), Optional.empty(), rolled.get().match(), count, reward, tier, day));
    }

    /** Gleichverteilt zwischen {@code min} und {@code max}, in 16er-Schritten ab {@code min} (unter 64: 4er). */
    public static int cappedCount(int min, int max, RandomSource random) {
        int step = max < 64 ? 4 : 16;
        int steps = Math.max(0, (max - min) / step);
        return Math.min(max, min + step * random.nextInt(steps + 1));
    }

    /** Zieht eine Vorlage nach ihrem Gewicht ({@code weight}). */
    public static ContractTemplates.Template pickWeighted(List<ContractTemplates.Template> candidates, RandomSource random) {
        int total = candidates.stream().mapToInt(ContractTemplates.Template::weight).sum();
        int roll = random.nextInt(total);
        for (ContractTemplates.Template t : candidates) {
            roll -= t.weight();
            if (roll < 0) {
                return t;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /** Glatte Mengen: unter 64 auf 4 gerundet, darüber auf 16; mindestens 1. */
    public static int roundCount(double raw) {
        if (raw < 4) {
            return Math.max(1, (int) Math.round(raw));
        }
        int step = raw < 64 ? 4 : 16;
        return Math.max(step, (int) Math.round(raw / step) * step);
    }
}
