package dev.dtp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import dev.dtp.block.SlotMachineBlock;
import dev.dtp.block.SlotMachineBlockEntity;
import dev.dtp.casino.SlotMachine;

/**
 * Die drei Walzen im Fenster des Spielautomaten. Die Symbole sind Items (wie im Rahmen), die von oben durchs Fenster
 * laufen; der 1 px tiefe Rahmen des Modells verdeckt, was über oder unter dem Fenster liegt. Jede Walze hält zu ihrem
 * Stopp-Tick ({@link SlotMachine#STOPS}) genau auf dem gewürfelten Symbol an. Gerechnet wird nur aus Spielzeit und
 * Einwurf-Tick — kein eigenes Netzwerkpaket.
 */
public class SlotMachineRenderer implements BlockEntityRenderer<SlotMachineBlockEntity, SlotMachineRenderer.State> {
    /** Walzenmitten in Modellpixeln; Walze 1 steht vom Spieler aus links (Osten im Nord-Modell). */
    private static final float[] REEL_X = {13.0f, 8.0f, 3.0f};
    /** Fenstermitte (Zeilen 5–9 der Fronttextur) und Abstand zweier Symbole auf der Walze, in Modellpixeln. */
    private static final float WINDOW_Y = 8.5f;
    private static final float SYMBOL_PITCH = 5.0f;
    /** Knapp vor der Rückwand der Fenstervertiefung (1 px tief). */
    private static final float DEPTH = 0.75f;
    /** Ticks je Symbol, während sich eine Walze dreht. */
    private static final float PERIOD = 2.0f;
    private static final float SCALE = 0.25f;

    private final ItemModelResolver itemModelResolver;
    /**
     * Erst beim ersten Rendern gefüllt: Der Renderer entsteht beim Ressourcen-Reload vor dem Titelbildschirm,
     * und da sind die Item-Komponenten noch nicht gebunden — ein ItemStack dort bricht den Reload ab.
     */
    private ItemStack[] symbols;

    public SlotMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    private ItemStack[] symbols() {
        if (symbols == null) {
            ItemStack[] built = new ItemStack[SlotMachine.SYMBOL_COUNT];
            for (int i = 0; i < built.length; i++) {
                built[i] = SlotMachine.symbol(i);
            }
            symbols = built;
        }
        return symbols;
    }

    public static class State extends BlockEntityRenderState {
        /** Je Walze das Symbol im Fenster und das, das gerade nach unten hinausläuft. */
        final ItemStackRenderState[] current = new ItemStackRenderState[3];
        final ItemStackRenderState[] leaving = new ItemStackRenderState[3];
        /** Je Walze die Verschiebung in Modellpixeln nach oben (0 = mittig). */
        final float[] offset = new float[3];
        final boolean[] moving = new boolean[3];
        float yRot;

        State() {
            for (int i = 0; i < 3; i++) {
                current[i] = new ItemStackRenderState();
                leaving[i] = new ItemStackRenderState();
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SlotMachineBlockEntity machine, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTicks, cameraPosition, breakProgress);
        Level level = machine.getLevel();
        Direction front = machine.getBlockState().getValue(SlotMachineBlock.FACING);
        state.yRot = 180.0f - front.toYRot();
        if (level != null) {
            // Der Block selbst ist dunkel; die Walzen sieht man von vorn, also zählt das Licht vor dem Fenster.
            state.lightCoords = LightCoordsUtil.getLightCoords(level, machine.getBlockPos().relative(front));
        }
        float elapsed = machine.spinning() && level != null
                ? Math.max(0.0f, level.getGameTime() - machine.spinStart() + partialTicks) : Float.MAX_VALUE;
        int seed = (int) machine.getBlockPos().asLong();
        for (int reel = 0; reel < 3; reel++) {
            int target = machine.symbols()[reel];
            ItemStack[] symbols = symbols();
            float remaining = (SlotMachine.STOPS[reel] - elapsed) / PERIOD;
            if (remaining > 0.0f) {
                // n Symbole bis zum Ziel; f = wie weit das aktuelle Symbol noch über der Mitte steht (0..1).
                int n = (int) Math.floor(remaining);
                float f = remaining - n;
                resolve(state.current[reel], symbols[Math.floorMod(target + n, symbols.length)], machine, seed + reel);
                resolve(state.leaving[reel], symbols[Math.floorMod(target + n + 1, symbols.length)], machine, seed + reel + 3);
                state.offset[reel] = f * SYMBOL_PITCH;
                state.moving[reel] = true;
            } else {
                resolve(state.current[reel], symbols[Math.floorMod(target, symbols.length)], machine, seed + reel);
                state.leaving[reel].clear();
                state.offset[reel] = 0.0f;
                state.moving[reel] = false;
            }
        }
    }

    private void resolve(ItemStackRenderState target, ItemStack stack, SlotMachineBlockEntity machine, int seed) {
        itemModelResolver.updateForTopItem(target, stack, ItemDisplayContext.FIXED, machine.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // Ins Nord-Modell drehen: Die Front liegt dann bei z = 0 und zeigt nach -z, wie bei einem Rahmen.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.rotateDegrees(Axis.YP, state.yRot);
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        for (int reel = 0; reel < 3; reel++) {
            symbol(state, state.current[reel], REEL_X[reel], WINDOW_Y + state.offset[reel], poseStack, collector);
            if (state.moving[reel]) {
                symbol(state, state.leaving[reel], REEL_X[reel], WINDOW_Y + state.offset[reel] - SYMBOL_PITCH, poseStack, collector);
            }
        }
        poseStack.popPose();
    }

    private static void symbol(State state, ItemStackRenderState item, float x, float y, PoseStack poseStack, SubmitNodeCollector collector) {
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x / 16.0f, y / 16.0f, DEPTH / 16.0f);
        poseStack.scale(SCALE, SCALE, SCALE);
        item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
