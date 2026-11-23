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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import dev.dtp.block.MarketScaleBlockEntity;

/** Lässt das Filter-Item der Marktwaage langsam drehend über der Waagschale schweben. */
public class MarketScaleRenderer implements BlockEntityRenderer<MarketScaleBlockEntity, MarketScaleRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public MarketScaleRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MarketScaleBlockEntity scale, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(scale, state, partialTicks, cameraPosition, breakProgress);
        itemModelResolver.updateForTopItem(state.item, scale.filter(), ItemDisplayContext.GROUND, scale.getLevel(), null,
                (int) scale.getBlockPos().asLong());
        state.time = scale.getLevel() == null ? 0.0f : scale.getLevel().getGameTime() + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Über der Waagschale (Oberkante bei 5/16), leicht auf und ab.
        poseStack.translate(0.5f, 0.55f + Mth.sin(state.time / 20.0f) * 0.04f, 0.5f);
        poseStack.rotateDegrees(Axis.YP, state.time * 2.0f % 360.0f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
