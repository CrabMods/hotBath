package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.fluid_details.FluidsColor;
import com.crabmod.hotbath.fluid_details.FluidsTexture;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A FluidType that supports dynamic per-block coloring based on BlockEntity data.
 * This allows each placed fluid block to have its own color based on the 
 * CustomFluidDefinition stored in its BlockEntity.
 * 
 * Both source and flowing fluid blocks have BlockEntities that store the fluid ID,
 * as the DynamicCustomFluid.spreadTo() method propagates this data when fluid spreads.
 * 
 * Uses grayscale textures that are tinted with the fluid's color for proper coloring.
 */
public class DynamicFluidType extends FluidType {
    
    // Use grayscale textures for proper tinting with custom colors
    private static final ResourceLocation CUSTOM_FLUID_STILL_RL = FluidsTexture.CUSTOM_FLUID_STILL_TEXTURE;
    private static final ResourceLocation CUSTOM_FLUID_FLOWING_RL = FluidsTexture.CUSTOM_FLUID_FLOWING_TEXTURE;
    private static final ResourceLocation WATER_OVERLAY_RL = new ResourceLocation("block/water_overlay");
    
    private static final int DEFAULT_TINT_COLOR = 0xFF45E1E9; // Default cyan with full alpha
    private static final Vector3f DEFAULT_FOG_COLOR = FluidsColor.DEFAULT_FOG_COLOR;
    
    // Default values (used when no BlockEntity data is found)
    private static final int DEFAULT_LIGHT_LEVEL = 2;
    private static final int DEFAULT_DENSITY = 1000;
    private static final int DEFAULT_VISCOSITY = 1000;
    private static final int DEFAULT_TEMPERATURE = 300; // Room temperature in Kelvin

    public DynamicFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public Component getDescription(FluidStack stack) {
        var definition = CustomFluidAPI.getCustomFluidDefinition(stack);
        if (definition.isEmpty()) {
            return Component.translatable("item.hotbath.custom_fluid.unknown");
        }
        var fluid = definition.get();
        String key = fluid.getTranslationKey();
        // Dedicated servers cannot query the client's language manager.
        String name = FMLEnvironment.dist.isClient()
                ? fluid.getTranslatedName()
                : fluid.translations().getOrDefault("en_us", key);
        return Component.translatableWithFallback(key, name);
    }

    @Override
    public boolean canConvertToSource(net.minecraft.world.level.material.FluidState state,
                                      net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return com.crabmod.hotbath.HotBathConfig.areInfiniteBathSourcesEnabled()
                && super.canConvertToSource(state, level, pos);
    }

    @Override
    public boolean canConvertToSource(net.minecraftforge.fluids.FluidStack stack) {
        return com.crabmod.hotbath.HotBathConfig.areInfiniteBathSourcesEnabled()
                && super.canConvertToSource(stack);
    }

    /**
     * Override to allow water-breathing entities (like tropical fish) to survive in dynamic custom fluids.
     * Entities that can breathe underwater won't drown in our fluid.
     */
    @Override
    public boolean canDrownIn(LivingEntity entity) {
        if (entity.canBreatheUnderwater()) {
            return false;
        }
        return super.canDrownIn(entity);
    }

    /**
     * Gets the CustomFluidDefinition from the BlockEntity at the given position.
     */
    private static Optional<CustomFluidDefinition> getDefinitionFromBlockEntity(BlockAndTintGetter getter, BlockPos pos) {
        BlockEntity be = getter.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidDefinition();
        }
        return Optional.empty();
    }
    
    /**
     * Gets the CustomFluidDefinition for the fluid at the given position.
     */
    private static Optional<CustomFluidDefinition> getDefinition(BlockAndTintGetter getter, BlockPos pos) {
        return getDefinitionFromBlockEntity(getter, pos);
    }
    
    // ==================== Dynamic Property Overrides ====================
    
    /**
     * Returns the light level based on the fluid definition at the position.
     */
    @Override
    public int getLightLevel(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return getDefinition(getter, pos)
                .map(CustomFluidDefinition::luminosity)
                .orElse(DEFAULT_LIGHT_LEVEL);
    }
    
    /**
     * Returns the density based on the fluid definition at the position.
     */
    @Override
    public int getDensity(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return getDefinition(getter, pos)
                .map(CustomFluidDefinition::density)
                .orElse(DEFAULT_DENSITY);
    }
    
    /**
     * Returns the viscosity based on the fluid definition at the position.
     */
    @Override
    public int getViscosity(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return getDefinition(getter, pos)
                .map(CustomFluidDefinition::viscosity)
                .orElse(DEFAULT_VISCOSITY);
    }
    
    /**
     * Returns the temperature based on the fluid definition at the position.
     * Converts Celsius to Kelvin (Kelvin = Celsius + 273).
     */
    @Override
    public int getTemperature(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return getDefinition(getter, pos)
                .map(def -> (int)(def.temperature() + 273)) // Convert Celsius to Kelvin
                .orElse(DEFAULT_TEMPERATURE);
    }
    
    /**
     * Gets the color from the BlockEntity at the given position.
     * Both source and flowing fluid blocks should have BlockEntities with color data.
     * Applies the opacity from the fluid definition to the alpha channel.
     */
    private static int getColorFromBlockEntity(BlockAndTintGetter getter, BlockPos pos) {
        BlockEntity be = getter.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            ResourceLocation fluidId = customBe.getFluidId();
            if (fluidId != null) {
                // Get color directly from registry to avoid potential issues
                return CustomFluidRegistry.getRuntimeData(fluidId)
                        .map(data -> data.getColorARGB())
                        .orElse(-1);
            }
        }
        return -1; // No color found
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return CUSTOM_FLUID_STILL_RL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return CUSTOM_FLUID_FLOWING_RL;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY_RL;
            }

            @Override
            public int getTintColor() {
                return DEFAULT_TINT_COLOR;
            }

            // Machines carry the custom fluid ID on the stack, without a world position.
            @Override
            public int getTintColor(FluidStack stack) {
                ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(stack);
                return fluidId == null ? DEFAULT_TINT_COLOR
                        : CustomFluidRegistry.getRuntimeData(fluidId)
                                .map(CustomFluidRegistry.RuntimeFluidData::getColorARGB)
                                .orElse(DEFAULT_TINT_COLOR);
            }

            /**
             * Gets the tint color based on the BlockEntity at the given position.
             * Both source and flowing fluid blocks have BlockEntities with color data,
             * as DynamicCustomFluid.spreadTo() propagates the data when fluid spreads.
             */
            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                // Try to get color from BlockEntity (for DynamicCustomFluidBlock)
                int color = getColorFromBlockEntity(getter, pos);
                if (color != -1) {
                    return color;
                }
                
                // Fallback to default
                return DEFAULT_TINT_COLOR;
            }

            @Override
            public @NotNull Vector3f modifyFogColor(
                    Camera camera,
                    float partialTick,
                    ClientLevel level,
                    int renderDistance,
                    float darkenWorldAmount,
                    Vector3f fluidFogColor) {
                // TODO: Could potentially get fog color from BlockEntity too
                return DEFAULT_FOG_COLOR;
            }
        });
    }
}
