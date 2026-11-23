package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.dtp.contract.ContractBoard;
import dev.dtp.registry.ModBlocks;

/**
 * Merkt sich, welche Rotation des Aushangs dieses Board zuletzt gezeigt hat. Hängt seitdem Neues aus, steht die Flagge
 * ({@link BulletinBoardBlock#FLAG}); wer das Board öffnet, holt sie ein. Bewusst ohne Ton und Meldung.
 */
public class BulletinBoardBlockEntity extends BlockEntity {
    /** Abstand der Prüfung in Ticks; die Rotation läuft in Spieltagen, eine Sekunde Verzug fällt nicht auf. */
    private static final int INTERVAL = 20;

    /** Zuletzt gezeigte Rotation, -1 = noch keine (frisch gesetzt oder aus einem älteren Spielstand). */
    private int seenRotation = -1;

    public BulletinBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BULLETIN_BOARD_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BulletinBoardBlockEntity entity) {
        if ((level.getGameTime() + pos.hashCode()) % INTERVAL != 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        entity.update(serverLevel, state);
    }

    /** Prüft den Aushang und hebt oder senkt die Flagge passend. Der Aushang rotiert sonst erst beim Öffnen. */
    public void update(ServerLevel level, BlockState state) {
        MinecraftServer server = level.getServer();
        ContractBoard board = ContractBoard.get(server);
        board.rotateIfDue(server);
        if (seenRotation < 0) {
            // Ein neues Board zeigt nichts Neues an — erst die nächste Rotation hebt die Flagge.
            seenRotation = board.rotation();
            setChanged();
        }
        setFlag(level, state, board.rotation() != seenRotation && !board.offers().isEmpty());
    }

    /** Das Board wurde geöffnet: alles Aushängende ist gesehen, die Flagge geht herunter. */
    public void lower() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Erst rotieren, falls fällig: Das Menü zeigt gleich den neuen Aushang, die Flagge darf danach nicht wieder steigen.
        ContractBoard board = ContractBoard.get(serverLevel.getServer());
        board.rotateIfDue(serverLevel.getServer());
        seenRotation = board.rotation();
        setChanged();
        setFlag(serverLevel, getBlockState(), false);
    }

    private void setFlag(ServerLevel level, BlockState state, boolean raised) {
        if (state.hasProperty(BulletinBoardBlock.FLAG) && state.getValue(BulletinBoardBlock.FLAG) != raised) {
            level.setBlock(worldPosition, state.setValue(BulletinBoardBlock.FLAG, raised), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        seenRotation = input.getIntOr("seen_rotation", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("seen_rotation", seenRotation);
    }
}
