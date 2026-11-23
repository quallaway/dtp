package dev.dtp.block;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import dev.dtp.casino.SlotMachine;
import dev.dtp.casino.SlotMachine.Outcome;
import dev.dtp.currency.Taler;
import dev.dtp.data.Protocol;
import dev.dtp.registry.ModBlocks;

/**
 * Zustand einer Drehung: Beim Einwurf stehen Ausgang und Symbole schon fest; der Client animiert allein aus
 * {@link #spinStart} und der Spielzeit, ohne eigenes Paket — das Block-Update erreicht alle, die den Chunk sehen.
 * Die Drehung wird gespeichert und läuft nach einem Neustart zu Ende.
 */
public class SlotMachineBlockEntity extends BlockEntity {
    /** Abstand des Walzen-Klackerns in Ticks. */
    private static final int RATTLE = 3;

    /** Spielzeit des Einwurfs, -1 = frei. */
    private long spinStart = -1;
    private int stake = -1;
    private Outcome outcome = Outcome.NOTHING;
    private int[] symbols = {1, 2, 3};
    private @Nullable UUID player;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SLOT_MACHINE_BE.get(), pos, state);
    }

    public boolean spinning() {
        return spinStart >= 0;
    }

    public long spinStart() {
        return spinStart;
    }

    public Outcome outcome() {
        return outcome;
    }

    /** Die Symbole der laufenden bzw. letzten Drehung. */
    public int[] symbols() {
        return symbols;
    }

    /**
     * Wirft eine Münze aus dem Stapel ein und startet die Drehung. Kreativspieler zahlen nichts. Gibt false zurück, wenn
     * die Walzen sich noch drehen oder der Stapel kein Taler ist; dann bleibt alles, wie es war.
     */
    public boolean insert(Player who, ItemStack stack) {
        Taler coin = Taler.of(stack);
        if (spinning() || coin == null || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        stack.consume(1, who);
        Outcome rolled = SlotMachine.roll(serverLevel.getRandom());
        start(serverLevel, who, coin, rolled, SlotMachine.symbols(rolled, serverLevel.getRandom()));
        return true;
    }

    /** Startet eine Drehung mit festem Ergebnis — getrennt von {@link #insert}, damit die Tests jeden Ausgang prüfen können. */
    public void start(ServerLevel serverLevel, @Nullable Player who, Taler coin, Outcome rolled, int[] reels) {
        spinStart = serverLevel.getGameTime();
        stake = coin.ordinal();
        outcome = rolled;
        symbols = reels.clone();
        player = who == null ? null : who.getUUID();
        setChanged();
        setSpinning(serverLevel, true);
        serverLevel.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.7f);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SlotMachineBlockEntity machine) {
        if (!machine.spinning() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long elapsed = level.getGameTime() - machine.spinStart;
        if (elapsed >= SlotMachine.DURATION) {
            machine.resolve(serverLevel);
            return;
        }
        for (int stop : SlotMachine.STOPS) {
            if (elapsed == stop) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
                return;
            }
        }
        if (elapsed % RATTLE == 0) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.25f, 1.6f);
        }
    }

    /** Zahlt aus, spielt den Ergebniston und gibt den Automaten wieder frei — eine Meldung gibt es bewusst nicht. */
    public void resolve(ServerLevel serverLevel) {
        if (!spinning()) {
            return;
        }
        Taler coin = Taler.values()[Math.clamp(stake, 0, Taler.values().length - 1)];
        long amount = SlotMachine.payout(outcome, coin);
        Direction front = getBlockState().getValue(SlotMachineBlock.FACING);
        for (ItemStack stack : Taler.split(amount)) {
            eject(serverLevel, front, stack);
        }
        effects(serverLevel, front, amount);
        ServerPlayer who = player == null ? null : serverLevel.getServer().getPlayerList().getPlayer(player);
        Protocol.log(serverLevel.getServer(), who, "slot_machine_" + outcome.id(), coin.id, 1, amount);
        spinStart = -1;
        setChanged();
        setSpinning(serverLevel, false);
    }

    /** Wirft einen Stapel vorn aus dem Automaten, mit etwas Schwung nach vorn — unabhängig von der Gamerule für Blockdrops. */
    private void eject(ServerLevel serverLevel, Direction front, ItemStack stack) {
        Vec3 at = Vec3.atCenterOf(worldPosition).add(front.getStepX() * 0.6, -0.3, front.getStepZ() * 0.6);
        double spread = 0.04;
        ItemEntity item = new ItemEntity(serverLevel, at.x, at.y, at.z, stack,
                front.getStepX() * 0.12 + serverLevel.getRandom().triangle(0, spread), 0.15,
                front.getStepZ() * 0.12 + serverLevel.getRandom().triangle(0, spread));
        item.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(item);
    }

    /**
     * Ton und Partikel zum Ausgang. Der Ton allein soll sagen, was passiert ist: Fanfare (Jackpot, doppelte Hörweite),
     * Amethyst (Gewinn), Erfahrungskugel (Trostpreis), tiefer Bass (nichts). Ein Trostpreis ohne Auszahlung
     * (Kupfertaler) klingt wie eine Niete.
     */
    private void effects(ServerLevel serverLevel, Direction front, long amount) {
        Outcome shown = outcome == Outcome.CONSOLATION && amount == 0 ? Outcome.NOTHING : outcome;
        SoundEvent sound;
        float volume = 1.0f;
        float pitch = 1.0f;
        SimpleParticleType particle;
        int count;
        switch (shown) {
            case JACKPOT -> {
                sound = SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
                volume = 2.0f;
                particle = ParticleTypes.TOTEM_OF_UNDYING;
                count = 40;
            }
            case WIN -> {
                sound = SoundEvents.AMETHYST_BLOCK_CHIME;
                particle = ParticleTypes.HAPPY_VILLAGER;
                count = 12;
            }
            case CONSOLATION -> {
                sound = SoundEvents.EXPERIENCE_ORB_PICKUP;
                particle = ParticleTypes.WAX_ON;
                count = 4;
            }
            default -> {
                sound = SoundEvents.NOTE_BLOCK_BASS.value();
                pitch = 0.5f;
                particle = ParticleTypes.SMOKE;
                count = 6;
            }
        }
        serverLevel.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        Vec3 at = Vec3.atCenterOf(worldPosition).add(front.getStepX() * 0.55, 0.1, front.getStepZ() * 0.55);
        serverLevel.sendParticles(particle, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, outcome == Outcome.JACKPOT ? 0.3 : 0.02);
    }

    private void setSpinning(ServerLevel serverLevel, boolean value) {
        BlockState state = getBlockState();
        if (state.getValue(SlotMachineBlock.SPINNING) != value) {
            serverLevel.setBlock(worldPosition, state.setValue(SlotMachineBlock.SPINNING, value), Block.UPDATE_CLIENTS);
        }
        BlockState now = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spinStart = input.getLongOr("spin_start", -1);
        stake = input.getIntOr("stake", -1);
        outcome = Outcome.byOrdinal(input.getIntOr("outcome", Outcome.NOTHING.ordinal()));
        symbols = input.getIntArray("symbols").filter(a -> a.length == 3).orElse(new int[] {1, 2, 3});
        player = input.read("player", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("spin_start", spinStart);
        output.putInt("stake", stake);
        output.putInt("outcome", outcome.ordinal());
        output.putIntArray("symbols", symbols);
        output.storeNullable("player", UUIDUtil.CODEC, player);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
