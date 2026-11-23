package dev.dtp.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.dtp.DtpConfig;
import dev.dtp.blackmarket.ScamCurse;
import dev.dtp.market.MerchantGoods;
import dev.dtp.market.TradeList;

/**
 * Ein Lieferauftrag. Die Belohnung wird beim Erstellen fixiert; der Zeitbonus hängt am Erstellungszeitpunkt
 * und läuft auch weiter, wenn der Contract abgebrochen und neu angenommen wird. So lohnt es nicht, Ware für
 * ein Angebot schon vor der Annahme vorzubereiten.
 *
 * {@code match} ist eine Mindestbedingung an das Item im Vanilla-Format ({@code components}, {@code predicates}),
 * etwa „Haltbarkeit II oder höher“. Leer = jedes Exemplar des Items zählt.
 *
 * {@code key} macht aus dem Contract einen Auftrag über eine <b>gemeinsame Ware</b> der Handelsliste, etwa
 * {@code dtp:wood_log}: Dann zählt jedes Item dieser Ware, {@code item} ist nur noch das Symbol am Aushang.
 * Geliefert wird in Kistenreihenfolge, gemischte Arten sind erlaubt.
 */
public record Contract(UUID id, Identifier item, Optional<Identifier> key, DataComponentMatchers match, int count, long reward,
        int tier, double createdDay, int delivered) {
    public static final Codec<Contract> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Contract::id),
            Identifier.CODEC.fieldOf("item").forGetter(Contract::item),
            Identifier.CODEC.optionalFieldOf("key").forGetter(Contract::key),
            DataComponentMatchers.CODEC.forGetter(Contract::match),
            Codec.INT.fieldOf("count").forGetter(Contract::count),
            Codec.LONG.fieldOf("reward").forGetter(Contract::reward),
            Codec.INT.fieldOf("tier").forGetter(Contract::tier),
            Codec.DOUBLE.fieldOf("created_day").forGetter(Contract::createdDay),
            Codec.INT.optionalFieldOf("delivered", 0).forGetter(Contract::delivered)
    ).apply(i, Contract::new));

    /** Ein frisch gewürfeltes Angebot. */
    public static Contract offer(Identifier item, DataComponentMatchers match, int count, long reward, int tier, double createdDay) {
        return offer(item, Optional.empty(), match, count, reward, tier, createdDay);
    }

    /** Ein frisch gewürfeltes Angebot; mit {@code key} über eine gemeinsame Ware der Handelsliste. */
    public static Contract offer(Identifier item, Optional<Identifier> key, DataComponentMatchers match, int count, long reward,
            int tier, double createdDay) {
        return new Contract(UUID.randomUUID(), item, key, match, count, reward, tier, createdDay, 0);
    }

    public Contract withDelivered(int delivered) {
        return new Contract(id, item, key, match, count, reward, tier, createdDay, delivered);
    }

    /** Zurück zum gewöhnlichen Angebot: ohne Lieferfortschritt, aber mit der alten Frist. */
    public Contract asPlainOffer() {
        return new Contract(id, item, key, match, count, reward, tier, createdDay, 0);
    }

    /**
     * Wechselt die Uhr, ohne das Alter zu ändern: Ein Contract, der auf der Uhr mit Stand {@code fromNow} gestempelt
     * ist, wird so auf die Uhr mit Stand {@code toNow} gestempelt. Beim Annehmen von der Aushang- auf die persönliche
     * Uhr, beim Abbrechen zurück — so läuft die Bonusfrist über Abbrechen und erneutes Annehmen weiter.
     */
    public Contract rebased(double fromNow, double toNow) {
        return new Contract(id, item, key, match, count, reward, tier, toNow - (fromNow - createdDay), delivered);
    }

    public Item itemValue() {
        return BuiltInRegistries.ITEM.getValue(item);
    }

    /**
     * Zählt dieses Exemplar für den Contract? Umbenannte Items nie — Schutz für das eigene Lieblingswerkzeug.
     * Items mit Scam- oder Maut-Fluch vom Schwarzmarkt auch nicht, ebenso wenig Händlerware von Dorfbewohnern.
     * Bei einer gemeinsamen Ware zählt jedes handelbare Exemplar, dessen Marktschlüssel passt.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || stack.has(DataComponents.CUSTOM_NAME) || ScamCurse.isLocked(stack)
                || MerchantGoods.isMarked(stack) || !match.test(stack)) {
            return false;
        }
        if (key.isPresent()) {
            return TradeList.lookup(stack).isPresent() && TradeList.marketKey(stack.getItem()).equals(key);
        }
        return stack.is(itemValue());
    }

    /** Name der Anforderung: bei einer gemeinsamen Ware der Gruppenname, sonst der Item-Name. */
    public Component displayName() {
        return key.map(Contract::groupName).orElseGet(() -> withExact(new ItemStack(itemValue())).getHoverName());
    }

    /** Das Item mit den exakt geforderten Komponenten, etwa dem geforderten Trank. */
    private ItemStack withExact(ItemStack stack) {
        if (!match.exact().isEmpty()) {
            stack.applyComponents(match.exact().asPatch());
        }
        return stack;
    }

    /** Der exakt geforderte Trank, falls die Bedingung einen nennt. */
    public static Optional<Holder<Potion>> requiredPotion(DataComponentMatchers match) {
        if (match.exact().isEmpty()) {
            return Optional.empty();
        }
        ItemStack probe = new ItemStack(net.minecraft.world.item.Items.POTION);
        probe.applyComponents(match.exact().asPatch());
        return probe.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
    }

    /** Übersetzungsschlüssel {@code dtp.contract.group.<namespace>.<pfad>}, etwa „Holzstämme (beliebige Art)“. */
    public static Component groupName(Identifier key) {
        return Component.translatable("dtp.contract.group." + key.getNamespace() + "." + key.getPath().replace('/', '.'));
    }

    /** Fordert der Contract Verzauberungen? Dann gilt: diese Stufe oder höher, weitere Verzauberungen erlaubt. */
    public boolean requiresEnchantments() {
        return match.partial().containsKey(DataComponentPredicates.ENCHANTMENTS)
                || match.partial().containsKey(DataComponentPredicates.STORED_ENCHANTMENTS);
    }

    public boolean requiresUndamaged() {
        return match.partial().containsKey(DataComponentPredicates.DAMAGE);
    }

    /**
     * Das Item, wie es am Aushang erscheint: mit den geforderten Verzauberungen in ihrer Mindeststufe, damit der
     * Tooltip sie nennt. Die Verzauberungsbedingungen werden dazu über ihren Codec gelesen — anders kommt man von
     * außen nicht an die Liste.
     */
    public ItemStack displayStack(HolderLookup.Provider registries) {
        ItemStack stack = withExact(new ItemStack(itemValue()));
        if (!requiresEnchantments()) {
            return stack;
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Tag encoded = DataComponentMatchers.CODEC.codec().encodeStart(ops, match).result().orElse(null);
        if (!(encoded instanceof CompoundTag root)) {
            return stack;
        }
        CompoundTag predicates = root.getCompoundOrEmpty("predicates");
        applyMinimum(stack, predicates.get("minecraft:enchantments"), DataComponents.ENCHANTMENTS, ops);
        applyMinimum(stack, predicates.get("minecraft:stored_enchantments"), DataComponents.STORED_ENCHANTMENTS, ops);
        return stack;
    }

    private static void applyMinimum(ItemStack stack, Tag predicates, DataComponentType<ItemEnchantments> type, RegistryOps<Tag> ops) {
        if (predicates == null) {
            return;
        }
        List<EnchantmentPredicate> list = EnchantmentPredicate.CODEC.listOf().parse(ops, predicates).result().orElse(List.of());
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(stack.getOrDefault(type, ItemEnchantments.EMPTY));
        for (EnchantmentPredicate p : list) {
            p.enchantments().filter(set -> set.size() == 1)
                    .ifPresent(set -> enchantments.set(set.get(0), p.level().min().orElse(1)));
        }
        stack.set(type, enchantments.toImmutable());
    }

    /** Item und Bedingung als vergleichbarer Schlüssel: Zwei Contracts mit gleichem Schlüssel fordern dasselbe. */
    public String requirement(HolderLookup.Provider registries) {
        return requirement(item, key, match, registries);
    }

    /**
     * Die Bedingung wird für den Vergleich serialisiert, weil Verzauberungslisten keine Wertgleichheit haben —
     * zweimal aus dem Datapack gelesen, sind sie verschiedene Objekte. Bei einer gemeinsamen Ware zählt der
     * Marktschlüssel statt des Symbol-Items — „Holzstämme“ und „Eichenstämme“ hängen nie gleichzeitig aus.
     */
    public static String requirement(Identifier item, Optional<Identifier> key, DataComponentMatchers match,
            HolderLookup.Provider registries) {
        String base = key.map(k -> "#" + k).orElseGet(item::toString);
        if (match.isEmpty()) {
            return base;
        }
        return base + DataComponentMatchers.CODEC.codec()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), match)
                .mapOrElse(Object::toString, error -> "?");
    }

    public int remaining() {
        return Math.max(0, count - delivered);
    }

    /**
     * Persönliche Bedingungen der Wertformel. Ohne Upgrade gelten die Werte aus der Config; Quick Delivery
     * hebt den Bonus an und lässt den Wert nach der Frist schneller fallen. Die Frist selbst ist für alle gleich.
     */
    public record Terms(double bonusFactor, double decayPerInterval) {
        public static Terms standard() {
            return new Terms(DtpConfig.BONUS_FACTOR.get(), DtpConfig.DECAY_PER_INTERVAL.get());
        }
    }

    /** Frist für den Zeitbonus in Tagen, je Stufe aus der Config. */
    public double bonusDays() {
        return DtpConfig.listValue(DtpConfig.BONUS_DAYS.get(), tier - 1, 3.0);
    }

    /**
     * Belohnung = Grundwert × Faktor. Innerhalb der Bonusfrist gilt bonusFactor; mit dem Ende der Frist fällt der
     * Bonus ganz weg, danach gilt max(minFactor ; 1 − decayPerInterval × volleIntervalle).
     * Mit den Startwerten: innerhalb der Frist +20 %, dann ein Intervall lang 100 %, danach −10 % je Intervall,
     * bis auf die Hälfte. Ein Contract läuft nie ab.
     *
     * @param now Stand der Uhr, auf die der Contract gestempelt ist — im Aushang die serverweite Contract-Uhr,
     *            angenommen die persönliche des Besitzers
     */
    public long payout(double now) {
        return payout(now, Terms.standard());
    }

    public long payout(double now, Terms terms) {
        return Math.round(reward * factor(now, terms));
    }

    /** Aktueller Anteil am Grundwert, z. B. 1,2 innerhalb der Bonusfrist. */
    public double factor(double now) {
        return factor(now, Terms.standard());
    }

    public double factor(double now, Terms terms) {
        double overdue = now - createdDay - bonusDays();
        if (overdue <= 0) {
            return terms.bonusFactor();
        }
        int intervals = (int) Math.floor(overdue / DtpConfig.DECAY_INTERVAL_DAYS.get());
        return Math.max(DtpConfig.MIN_FACTOR.get(), afterBonus(terms) - terms.decayPerInterval() * intervals);
    }

    /** Faktor direkt nach der Bonusfrist: der Grundwert, nie mehr als der Bonusfaktor selbst. */
    private static double afterBonus(Terms terms) {
        return Math.min(1.0, terms.bonusFactor());
    }

    /**
     * Um wie viel der Anteil bei der nächsten Stufe fällt — innerhalb der Frist der ganze Bonus, danach ein Schritt,
     * weniger, wenn die Untergrenze dazwischen liegt.
     */
    public double nextDropStep(double now, Terms terms) {
        double minFactor = DtpConfig.MIN_FACTOR.get();
        if (now - createdDay - bonusDays() <= 0) {
            return Math.max(0.0, terms.bonusFactor() - Math.max(minFactor, afterBonus(terms)));
        }
        return Math.min(terms.decayPerInterval(), factor(now, terms) - minFactor);
    }

    /**
     * Tage, bis der Wert die nächste Stufe tiefer fällt — innerhalb der Bonusfrist also deren Rest.
     * -1, wenn die Untergrenze erreicht ist und nichts mehr fällt.
     */
    public double daysUntilNextDrop(double now) {
        return daysUntilNextDrop(now, Terms.standard());
    }

    public double daysUntilNextDrop(double now, Terms terms) {
        double overdue = now - createdDay - bonusDays();
        if (overdue <= 0) {
            return -overdue;
        }
        double interval = DtpConfig.DECAY_INTERVAL_DAYS.get();
        if (factor(now, terms) <= DtpConfig.MIN_FACTOR.get() + 1e-9 || terms.decayPerInterval() <= 0) {
            return -1;
        }
        double rest = (Math.floor(overdue / interval) + 1) * interval - overdue;
        return rest <= 0 ? interval : rest;
    }
}
