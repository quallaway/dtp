package dev.dtp.upgrade;

import java.util.List;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Symbol eines Upgrades im Baum, in der Effekt- bzw. Modulauswahl und im Modul-HUD: ein Item oder ein GUI-Sprite.
 *
 * <p>Im Datapack ist {@code icon} entweder eine Item-ID ({@code "minecraft:blast_furnace"}) oder ein Objekt mit
 * Sprite-ID ({@code {"sprite": "minecraft:mob_effect/speed"}}). Sprites kommen aus dem Vanilla-GUI-Atlas, der
 * {@code textures/gui/sprites/**} und {@code textures/mob_effect/**} (Präfix {@code mob_effect/}) einsammelt; die
 * eigenen liegen unter {@code assets/dtp/textures/gui/sprites/upgrade/}.
 */
public record UpgradeIcon(Identifier id, boolean sprite) {
    public static final UpgradeIcon BARRIER = item(Identifier.withDefaultNamespace("barrier"));

    private static final Codec<Identifier> SPRITE_OBJECT = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("sprite").forGetter(id -> id)
    ).apply(i, id -> id));

    public static final Codec<UpgradeIcon> CODEC = Codec.either(Identifier.CODEC, SPRITE_OBJECT).xmap(
            e -> e.map(UpgradeIcon::item, UpgradeIcon::sprite),
            icon -> icon.sprite ? Either.right(icon.id) : Either.left(icon.id));

    public static UpgradeIcon item(Identifier id) {
        return new UpgradeIcon(id, false);
    }

    public static UpgradeIcon sprite(Identifier id) {
        return new UpgradeIcon(id, true);
    }

    /** Symbol für {@code level} (1-basiert): {@code levelIcons} gelten parallel zu {@code level_positions} ab Stufe 2. */
    public static UpgradeIcon forLevel(UpgradeIcon base, List<UpgradeIcon> levelIcons, int level) {
        int index = level - 2;
        return index >= 0 && index < levelIcons.size() ? levelIcons.get(index) : base;
    }

    /** Für den Client: {@code icon} (Item) oder {@code icon_sprite} (Sprite). */
    public void write(CompoundTag tag) {
        tag.putString(sprite ? "icon_sprite" : "icon", id.toString());
    }
}
