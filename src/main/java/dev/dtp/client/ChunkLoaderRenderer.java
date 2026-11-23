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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import dev.dtp.block.ChunkLoaderBlockEntity;

/**
 * Die Enderperle im Glaskasten des Chunkloaders: Mit Restlaufzeit schwebt sie mittig, wippt und dreht sich;
 * ohne liegt sie still auf dem Podest.
 */
public class ChunkLoaderRenderer implements BlockEntityRenderer<ChunkLoaderBlockEntity, ChunkLoaderRenderer.State> {
    /** Lazy: Beim Ressourcen-Reload vor dem Titelbildschirm sind die Item-Komponenten noch nicht gebunden. */
    private ItemStack pearl;

    private final ItemModelResolver itemModelResolver;

    public ChunkLoaderRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
        boolean active;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChunkLoaderBlockEntity loader, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(loader, state, partialTicks, cameraPosition, breakProgress);
        itemModelResolver.updateForTopItem(state.item, pearl(), ItemDisplayContext.GROUND, loader.getLevel(), null,
                (int) loader.getBlockPos().asLong());
        state.time = loader.getLevel() == null ? 0.0f : loader.getLevel().getGameTime() + partialTicks;
        state.active = loader.active();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        if (state.active) {
            // Mittig im Glaskasten, leicht auf und ab.
            poseStack.translate(0.5f, 0.4f + Mth.sin(state.time / 16.0f) * 0.05f, 0.5f);
            poseStack.rotateDegrees(Axis.YP, state.time * 3.0f % 360.0f);
        } else {
            // Ruht auf dem Podestring (Oberkante bei 3/16).
            poseStack.translate(0.5f, 0.3f, 0.5f);
        }
        poseStack.scale(0.8f, 0.8f, 0.8f);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private ItemStack pearl() {
        if (pearl == null) {
            pearl = new ItemStack(Items.ENDER_PEARL);
        }
        return pearl;
    }
}
