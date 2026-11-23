package dev.dtp.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Eigene Armhaltungen, eingehängt über {@code META-INF/enumextensions.json}.
 *
 * <p>{@code GUN_AIM}: Der Arm mit der Gun zeigt gestreckt nach vorn und folgt der Blickrichtung, wie der
 * Zugarm beim Bogen, aber einhändig. Die Haltung der Waffe in diesem Arm steht in
 * {@code models/item/gun.json} ({@code thirdperson_*}).
 */
public final class DtpArmPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> GUN_AIM = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, false, (IArmPoseTransformer) DtpArmPoses::aim);

    private DtpArmPoses() {
    }

    private static void aim(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = (float) (-Math.PI / 2) + model.head.xRot;
        part.yRot = model.head.yRot + (arm == HumanoidArm.RIGHT ? -0.1F : 0.1F);
        part.zRot = 0.0F;
    }
}
