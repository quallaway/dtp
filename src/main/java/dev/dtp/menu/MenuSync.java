package dev.dtp.menu;

import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.dtp.Dtp;

/**
 * Anzeigedaten für die Oberflächen. Der Server ist die einzige Wahrheit: Der Client schickt nur
 * Knopfdrücke (Vanilla {@code clickMenuButton}), der Server antwortet mit einem Datenstand.
 * Gesendet wird nach jeder Aktion und sonst einmal pro Sekunde, solange die Oberfläche offen ist.
 */
public final class MenuSync {
    private static final int INTERVAL = 20;

    public interface Holder {
        MenuSync sync();
    }

    public record Payload(int containerId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "menu_data"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::containerId,
                ByteBufCodecs.TRUSTED_COMPOUND_TAG, Payload::data,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            AbstractContainerMenu menu = context.player().containerMenu;
            if (menu.containerId == payload.containerId() && menu instanceof Holder holder) {
                holder.sync().accept(payload.data());
            }
        }
    }

    private final AbstractContainerMenu menu;
    private final Player player;
    private int countdown;
    private CompoundTag data = new CompoundTag();

    public MenuSync(AbstractContainerMenu menu, Player player) {
        this.menu = menu;
        this.player = player;
    }

    /** Serverseitig aus {@code broadcastChanges} aufrufen. */
    public void tick(Supplier<CompoundTag> builder) {
        if (player instanceof ServerPlayer serverPlayer && countdown-- <= 0) {
            countdown = INTERVAL;
            PacketDistributor.sendToPlayer(serverPlayer, new Payload(menu.containerId, builder.get()));
        }
    }

    /** Nach einer Aktion: Der nächste {@link #tick} sendet sofort. */
    public void markChanged() {
        countdown = 0;
    }

    void accept(CompoundTag data) {
        this.data = data;
    }

    /** Clientseitig: letzter empfangener Datenstand. */
    public CompoundTag data() {
        return data;
    }

    public static void putComponent(CompoundTag tag, String key, Component component) {
        if (component != null) {
            ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, component).ifSuccess(t -> tag.put(key, t));
        }
    }

    /** Ein Item samt Zusatzdaten, etwa mit Verzauberungen — braucht die Register, weil Verzauberungen dynamisch sind. */
    public static void putStack(CompoundTag tag, String key, ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).ifSuccess(t -> tag.put(key, t));
        }
    }

    public static ItemStack getStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        Tag raw = tag.get(key);
        if (raw == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), raw).result().orElse(ItemStack.EMPTY);
    }

    public static Component getComponent(CompoundTag tag, String key) {
        Tag raw = tag.get(key);
        if (raw == null) {
            return null;
        }
        return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, raw).result().orElse(null);
    }
}
