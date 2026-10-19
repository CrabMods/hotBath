package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBathClientConfig;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.dirtiness.DirtinessClientData;
import com.crabmod.hotbath.dirtiness.DirtinessOverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EpicFightDirtinessPatchedLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends RenderLayer<E, M>> extends PatchedLayer<E, T, M, R> {

    private static final int NUM_PATTERNS = 10;
    private static final ResourceLocation[] DIRT_TEXTURES = new ResourceLocation[NUM_PATTERNS];
    private static final float MIN_DIRTINESS = 0.01f;

    private static final float DIRT_R = 0.30f;
    private static final float DIRT_G = 0.22f;
    private static final float DIRT_B = 0.15f;

    private final boolean firstPerson;

    static {
        for (int i = 0; i < NUM_PATTERNS; i++) {
            DIRT_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "textures/entity/player/dirt_overlay_" + i + ".png");
        }
    }

    public EpicFightDirtinessPatchedLayer() {
        this(false);
    }

    EpicFightDirtinessPatchedLayer(boolean firstPerson) {
        this.firstPerson = firstPerson;
    }

    @Override
    protected void renderLayer(T entitypatch, E entity, @Nullable R vanillaLayer,
                               PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                               OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
        if (!HotBathConfig.isDirtinessEnabled() || !HotBathClientConfig.SHOW_DIRT.get()) return;
        if (!(entity instanceof Player player)) return;

        float dirtiness = DirtinessClientData.getDirtiness(player.getUUID());
        if (dirtiness < MIN_DIRTINESS) return;

        long seed = DirtinessClientData.getDirtSeed(player.getUUID());
        if (seed == 0) {
            seed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
        }
        int patternIndex = com.crabmod.hotbath.dirtiness.DirtinessPattern.index(seed, NUM_PATTERNS);
        ResourceLocation dirtTexture = DIRT_TEXTURES[patternIndex];
        RenderType renderType = RenderType.entityTranslucent(dirtTexture);

        HumanoidMesh mesh = getMesh(entity);
        if (mesh == null) return;

        Armature armature = entitypatch.getArmature();

        Map<SkinnedMeshPart, Boolean> savedVisibility = saveVisibility(mesh);
        try {
            if (firstPerson) {
                hideAll(mesh);
                renderPartGroup(mesh, poseStack, buffer, renderType, packedLight, poses, armature,
                        dirtiness, 1.0f, 0.0f,
                        mesh.leftArm, mesh.rightArm, mesh.leftSleeve, mesh.rightSleeve);
                return;
            }

            hideAll(mesh);
            renderPartGroup(mesh, poseStack, buffer, renderType, packedLight, poses, armature,
                    dirtiness, 1.0f, 0.0f,
                    mesh.leftLeg, mesh.rightLeg, mesh.leftPants, mesh.rightPants);

            hideAll(mesh);
            renderPartGroup(mesh, poseStack, buffer, renderType, packedLight, poses, armature,
                    dirtiness, 0.8f, 0.15f,
                    mesh.torso, mesh.jacket);

            hideAll(mesh);
            renderPartGroup(mesh, poseStack, buffer, renderType, packedLight, poses, armature,
                    dirtiness, 0.7f, 0.2f,
                    mesh.leftArm, mesh.rightArm, mesh.leftSleeve, mesh.rightSleeve);

            hideAll(mesh);
            renderPartGroup(mesh, poseStack, buffer, renderType, packedLight, poses, armature,
                    dirtiness, 0.4f, 0.5f,
                    mesh.head, mesh.hat);
        } finally {
            restoreVisibility(savedVisibility);
        }
    }

    private void renderPartGroup(HumanoidMesh mesh, PoseStack poseStack, MultiBufferSource buffer,
                                 RenderType renderType, int packedLight, OpenMatrix4f[] poses,
                                 Armature armature, float dirtiness, float intensityMultiplier,
                                 float threshold, SkinnedMeshPart... parts) {
        if (dirtiness < threshold) return;

        float effectiveDirtiness = (dirtiness - threshold) / (1.0f - threshold);
        effectiveDirtiness *= intensityMultiplier;
        if (effectiveDirtiness < MIN_DIRTINESS) return;

        float alpha = calculateAlpha(effectiveDirtiness);

        for (SkinnedMeshPart part : parts) {
            if (part != null) {
                part.setHidden(false);
            }
        }

        mesh.draw(poseStack, buffer, renderType, packedLight,
                DIRT_R, DIRT_G, DIRT_B, alpha,
                OverlayTexture.NO_OVERLAY, armature, poses);
    }

    private static float calculateAlpha(float dirtiness) {
        return Math.min(0.90f, dirtiness * 0.6f + dirtiness * dirtiness * 0.4f);
    }

    private static HumanoidMesh getMesh(LivingEntity entity) {
        if (entity instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
            boolean isSlim = PlayerSkin.Model.SLIM.equals(clientPlayer.getSkin().model());
            return (isSlim ? Meshes.ALEX : Meshes.BIPED).get();
        }
        return Meshes.BIPED.get();
    }

    private static Map<SkinnedMeshPart, Boolean> saveVisibility(HumanoidMesh mesh) {
        Map<SkinnedMeshPart, Boolean> saved = new LinkedHashMap<>();
        for (Map.Entry<String, SkinnedMeshPart> entry : mesh.getPartEntry()) {
            saved.put(entry.getValue(), entry.getValue().isHidden());
        }
        return saved;
    }

    private static void hideAll(HumanoidMesh mesh) {
        for (SkinnedMeshPart part : mesh.getAllParts()) {
            part.setHidden(true);
        }
    }

    private static void restoreVisibility(Map<SkinnedMeshPart, Boolean> saved) {
        saved.forEach(SkinnedMeshPart::setHidden);
    }
}
