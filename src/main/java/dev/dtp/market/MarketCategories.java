package dev.dtp.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

import dev.dtp.Dtp;
import dev.dtp.world.DimensionAccess;

/**
 * Filtergruppen der Preisübersicht aus {@code data/<ns>/dtp/market_category/<id>.json}.
 *
 * Welche Items zu einer Gruppe gehören, bestimmt der Item-Tag {@code <ns>:market_category/<id>}. Ein Item darf
 * in mehreren Gruppen stehen. Die Tags sind auch auf dem Client bekannt — gefiltert wird dort.
 *
 * Eine Gruppe kann eine Freischaltung verlangen ({@code unlock}). Bis dahin fehlt sie am Marktstand, und kein
 * Marktereignis wählt sie aus. Die Bedingungen gelten serverweit.
 */
public class MarketCategories extends SimpleJsonResourceReloadListener<MarketCategories.Definition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "market_categories");

    /**
     * Freischaltbedingung; alle gesetzten Teile müssen erfüllt sein.
     *
     * @param dimension    die Dimension ist zugänglich — für den Nether: Server-Upgrade gekauft oder Sperre aus
     * @param dragonKilled der Enderdrache wurde auf diesem Server mindestens einmal besiegt
     */
    public record Unlock(Optional<ResourceKey<Level>> dimension, boolean dragonKilled) {
        public static final Unlock NONE = new Unlock(Optional.empty(), false);
        public static final Codec<Unlock> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(Unlock::dimension),
                Codec.BOOL.optionalFieldOf("dragon_killed", false).forGetter(Unlock::dragonKilled)
        ).apply(i, Unlock::new));

        public boolean test(MinecraftServer server) {
            if (dimension.isPresent() && !dimensionOpen(server, dimension.get())) {
                return false;
            }
            return !dragonKilled || MarketCategories.dragonKilled(server);
        }
    }

    public record Definition(Identifier icon, int order, Unlock unlock) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("icon").forGetter(Definition::icon),
                Codec.INT.optionalFieldOf("order", 100).forGetter(Definition::order),
                Unlock.CODEC.optionalFieldOf("unlock", Unlock.NONE).forGetter(Definition::unlock)
        ).apply(i, Definition::new));
    }

    /** Eine geladene Gruppe; {@code tag} ist der Item-Tag, {@code id} der Dateiname für die Übersetzung. */
    public record Category(Identifier id, Identifier tag, Identifier icon, int order, Unlock unlock) {
        public boolean unlocked(MinecraftServer server) {
            return unlock.test(server);
        }
    }

    private static List<Category> categories = List.of();

    public MarketCategories() {
        super(Definition.CODEC, FileToIdConverter.json("dtp/market_category"));
    }

    @Override
    protected void apply(Map<Identifier, Definition> loaded, ResourceManager manager, ProfilerFiller profiler) {
        List<Category> result = new ArrayList<>();
        loaded.forEach((id, def) -> result.add(new Category(id,
                Identifier.fromNamespaceAndPath(id.getNamespace(), "market_category/" + id.getPath()), def.icon(), def.order(),
                def.unlock())));
        result.sort(Comparator.comparingInt(Category::order).thenComparing(c -> c.id().toString()));
        categories = List.copyOf(result);
        Dtp.LOGGER.info("Loaded {} market categories", result.size());
    }

    public static List<Category> all() {
        return categories;
    }

    /** Die freigeschalteten Gruppen in Anzeigereihenfolge. */
    public static List<Category> unlocked(MinecraftServer server) {
        return categories.stream().filter(c -> c.unlocked(server)).toList();
    }

    /**
     * Die freigeschalteten Gruppen für eine Oberfläche: je Gruppe {@code id}, {@code tag} und {@code icon}. Gefiltert
     * wird auf dem Client (siehe {@code CategoryBar}) — Marktstand und Statistik-Tafel zeigen dieselben Gruppen.
     */
    public static ListTag snapshot(MinecraftServer server) {
        ListTag list = new ListTag();
        for (Category c : unlocked(server)) {
            CompoundTag e = new CompoundTag();
            e.putString("id", c.id().toString());
            e.putString("tag", c.tag().toString());
            e.putString("icon", c.icon().toString());
            list.add(e);
        }
        return list;
    }

    /** Gruppe mit dieser ID, falls geladen. */
    public static Optional<Category> byId(String id) {
        return categories.stream().filter(c -> c.id().toString().equals(id)).findFirst();
    }

    /**
     * Ob die Preisübersicht das Item zeigt: nicht, wenn es nur in gesperrten Gruppen steht (die Chorusfrucht vor
     * dem Drachensieg). Items ohne Gruppe bleiben sichtbar. Verkaufen lässt sich das Item trotzdem.
     */
    public static boolean visible(MinecraftServer server, Item item) {
        Holder<Item> holder = item.builtInRegistryHolder();
        boolean grouped = false;
        for (Category c : categories) {
            if (holder.is(TagKey.create(Registries.ITEM, c.tag()))) {
                if (c.unlocked(server)) {
                    return true;
                }
                grouped = true;
            }
        }
        return !grouped;
    }

    /** Der Nether hängt an der Zugangssperre der Mod; andere Dimensionen gelten als offen, sobald es sie gibt. */
    private static boolean dimensionOpen(MinecraftServer server, ResourceKey<Level> dimension) {
        if (server.getLevel(dimension) == null) {
            return false;
        }
        return dimension != Level.NETHER || !DimensionAccess.netherLocked(server);
    }

    /** Vanilla merkt sich im Drachenkampf des End, ob der Drache je besiegt wurde. Ohne End: nie. */
    public static boolean dragonKilled(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        EnderDragonFight fight = end == null ? null : end.getDragonFight();
        return fight != null && fight.hasPreviouslyKilledDragon();
    }
}
