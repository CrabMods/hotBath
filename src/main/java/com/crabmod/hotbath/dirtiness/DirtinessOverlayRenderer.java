package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.compat.EmfDirtinessCompat;
import com.crabmod.hotbath.HotBathConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Dirt overlay renderer that renders dirt directly on the player model.
 * 
 * Uses the player model itself to render dirt as a texture overlay,
 * which automatically adapts to any custom player model/skin.
 * This avoids Z-fighting and works with all model types.
 * 
 * Dirt Distribution (controlled by per-part intensity):
 * - Legs = most dirty (contact with ground)
 * - Body = moderate
 * - Arms = moderate  
 * - Head = least dirty
 * 
 * Pattern Selection:
 * - Each "dirtiness cycle" uses the dirt seed to select a pattern
 * - When player is cleaned and gets dirty again, a new seed = new pattern
 */
public class DirtinessOverlayRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    
    // Number of different dirt patterns available
    private static final int NUM_PATTERNS = 10;
    
    // Dirt overlay textures - uses player skin UV layout
    private static final ResourceLocation[] DIRT_TEXTURES = new ResourceLocation[NUM_PATTERNS];
    
    static {
        for (int i = 0; i < NUM_PATTERNS; i++) {
            DIRT_TEXTURES[i] = new ResourceLocation(HotBath.MOD_ID, "textures/entity/player/dirt_overlay_" + i + ".png");
        }
    }
    
    private static final float MIN_DIRTINESS = 0.01f;
    
    public DirtinessOverlayRenderer(PlayerRenderer renderer) {
        super(renderer);
    }
    
    /**
     * Get pattern index based on the current dirt seed.
     * This means each dirtiness cycle will have a consistent pattern,
     * but cleaning and getting dirty again will result in a new pattern.
     */
    private static int getPatternFromSeed(long seed) {
        return DirtinessPattern.index(seed, NUM_PATTERNS);
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // Check if dirtiness system is enabled
        if (!HotBathConfig.isDirtinessEnabled()) return;
        
        float dirtiness = DirtinessClientData.getDirtiness(player.getUUID());
        if (dirtiness < MIN_DIRTINESS) return;
        
        // Get the dirt seed for this player's current dirtiness cycle
        long seed = DirtinessClientData.getDirtSeed(player.getUUID());
        // Fallback to UUID-based seed if no seed is set
        if (seed == 0) {
            seed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
        }
        
        // Get the dirt texture pattern based on seed
        int patternIndex = getPatternFromSeed(seed);
        ResourceLocation dirtTexture = DIRT_TEXTURES[patternIndex];
        
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        
        AbstractClientPlayer previous = EmfDirtinessCompat.beginOverlay(player);
        try {
            // Render dirt on each body part with different intensities
            // Legs are dirtiest, head is cleanest
            // Using different thresholds: legs show dirt first, head shows dirt last

            // Legs - show dirt even at low dirtiness (threshold 0.0)
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.leftLeg, dirtiness, 1.0f, 0.0f, dirtTexture);
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.rightLeg, dirtiness, 1.0f, 0.0f, dirtTexture);

            // Body - moderate threshold (0.15)
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.body, dirtiness, 0.8f, 0.15f, dirtTexture);

            // Arms - moderate threshold (0.2)
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.leftArm, dirtiness, 0.7f, 0.2f, dirtTexture);
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.rightArm, dirtiness, 0.7f, 0.2f, dirtTexture);

            // Head - high threshold, only shows when very dirty (0.5)
            renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.head, dirtiness, 0.4f, 0.5f, dirtTexture);

            // Also render on outer layer parts if they exist and are visible
            if (model.leftPants.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.leftPants, dirtiness, 1.0f, 0.0f, dirtTexture);
            }
            if (model.rightPants.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.rightPants, dirtiness, 1.0f, 0.0f, dirtTexture);
            }
            if (model.jacket.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.jacket, dirtiness, 0.8f, 0.15f, dirtTexture);
            }
            if (model.leftSleeve.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.leftSleeve, dirtiness, 0.7f, 0.2f, dirtTexture);
            }
            if (model.rightSleeve.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.rightSleeve, dirtiness, 0.7f, 0.2f, dirtTexture);
            }
            if (model.hat.visible) {
                renderBodyPartWithDirt(poseStack, bufferSource, packedLight, model.hat, dirtiness, 0.4f, 0.5f, dirtTexture);
            }
        } finally {
            EmfDirtinessCompat.endOverlay(previous);
        }
    }
    
    /**
     * Renders dirt overlay on a specific body part.
     * 
     * @param poseStack The pose stack
     * @param bufferSource The buffer source
     * @param packedLight The packed light value
     * @param part The model part to render
     * @param dirtiness The overall dirtiness level (0-1)
     * @param intensityMultiplier How dirty this part should be relative to others (0-1)
     * @param threshold Minimum dirtiness level before this part shows dirt
     * @param dirtTexture The dirt texture to use
     */
    private void renderBodyPartWithDirt(PoseStack poseStack, MultiBufferSource bufferSource, 
                                         int packedLight, ModelPart part, 
                                         float dirtiness, float intensityMultiplier, float threshold,
                                         ResourceLocation dirtTexture) {
        if (!part.visible) return;
        if (dirtiness < threshold) return;
        
        // Calculate effective dirtiness for this part (remapped from threshold to 1.0)
        float effectiveDirtiness = (dirtiness - threshold) / (1.0f - threshold);
        effectiveDirtiness *= intensityMultiplier;
        if (effectiveDirtiness < MIN_DIRTINESS) return;
        
        // Calculate alpha directly from effective dirtiness for clear gradient
        // Linear mapping: alpha increases proportionally with dirtiness
        float alpha = Math.min(0.95f, effectiveDirtiness * 0.95f);
        
        // Use white color (1,1,1) to let the texture's own color show through
        // The texture itself contains the brown/dirt coloring
        // Only alpha controls the visibility/intensity
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        
        // Get vertex consumer for this part's render
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(dirtTexture));
        
        // Render the part's cubes with dirt overlay including color and alpha
        // In 1.20.1 Forge, ModelPart.render() accepts RGBA float parameters
        part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
    }
    
    // Keep methods for compatibility with other code (no longer needed but kept for API stability)
    public static void clearCache(UUID playerId) {
        // No cache needed - pattern is derived from seed each frame
    }
    
    public static void clearAllCaches() {
        // No cache needed - pattern is derived from seed each frame
    }
}
