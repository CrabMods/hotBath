package com.crabmod.hotbath.util;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.HerbalBathBlock;
import com.crabmod.hotbath.fluid_blocks.HoneyBathBlock;
import com.crabmod.hotbath.fluid_blocks.HotWaterBlock;
import com.crabmod.hotbath.fluid_blocks.MilkBathBlock;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import com.crabmod.hotbath.fluid_blocks.RoseBathBlock;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public class CustomFluidHandler {

    public static boolean isPlayerHeadInHotBath(Player player) {
        BlockPos playerEyePos = BlockPos.containing(player.getEyePosition());
        BlockState stateAtPlayerPos = player.level().getBlockState(playerEyePos);
        return stateAtPlayerPos.getBlock() instanceof AbstractHotbathBlock
                || getStoredBathFluid(player.level(), playerEyePos) != null;
    }

    public static boolean isPlayerInHotBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof AbstractHotbathBlock
                || getStoredBathFluid(player.level(), playerPos) != null;
    }
    
    /**
     * Checks if the player is in a **hot** bath block.
     * For built-in bath types (HotWaterBlock, HerbalBathBlock, etc.), they are always considered hot.
     * For custom fluids (DynamicCustomFluidBlock), checks the fluid's temperature >= HOT_TEMPERATURE_THRESHOLD.
     * 
     * @param player The player to check
     * @return true if the player is in a hot bath that should provide warmth effects
     */
    public static boolean isPlayerInHotBath(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        Block block = stateAtPlayerPos.getBlock();
        
        // For DynamicCustomFluidBlock, check if the fluid is hot
        if (block instanceof DynamicCustomFluidBlock) {
            BlockEntity be = player.level().getBlockEntity(playerPos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                return customBe.isHot();
            }
            return false; // No BlockEntity, can't determine temperature
        }
        
        // For built-in bath types, they are always considered hot
        if (block instanceof AbstractHotbathBlock bath) {
            return bath.isHotBath(player.level(), playerPos);
        }

        Fluid storedFluid = getStoredBathFluid(player.level(), playerPos);
        if (storedFluid == null) {
            return false;
        }
        if (storedFluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()) {
            return getStoredCustomDefinition(player.level(), playerPos)
                    .map(CustomFluidDefinition::isHot)
                    .orElse(false);
        }

        return com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions
                .forFluid(storedFluid, player.level().isClientSide())
                .map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::isHot).orElse(true);
    }
    
    /**
     * Gets the temperature of the bath fluid at the player's position.
     * For built-in bath types, returns default hot temperature (40鎺矯).
     * For custom fluids, returns the configured temperature.
     * 
     * @param player The player to check
     * @return The temperature in Celsius, or 0 if not in a bath
     */
    public static float getBathTemperature(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        Block block = stateAtPlayerPos.getBlock();
        
        // For DynamicCustomFluidBlock, get temperature from BlockEntity
        if (block instanceof DynamicCustomFluidBlock) {
            BlockEntity be = player.level().getBlockEntity(playerPos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                Optional<CustomFluidDefinition> def = customBe.getFluidDefinition();
                if (def.isPresent()) {
                    return def.get().temperature();
                }
            }
            return 0;
        }
        
        // For built-in bath types, return default hot temperature
        if (block instanceof AbstractHotbathBlock) {
            return com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions
                    .forFluid(stateAtPlayerPos.getFluidState().getType(), player.level().isClientSide())
                    .map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::temperature).orElse(37.0f);
        }

        Fluid storedFluid = getStoredBathFluid(player.level(), playerPos);
        if (storedFluid == null) {
            return 0;
        }
        if (storedFluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()) {
            return getStoredCustomDefinition(player.level(), playerPos)
                    .map(CustomFluidDefinition::temperature)
                    .orElse(0.0f);
        }
        return com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions
                .forFluid(storedFluid, player.level().isClientSide())
                .map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::temperature).orElse(37.0f);
    }

    private static Fluid getStoredBathFluid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                || !state.getValue(BlockStateProperties.WATERLOGGED)) {
            return null;
        }

        Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
        if (!HotbathFluidHelper.isHotbathFluid(storedFluid)) {
            return null;
        }

        return HotbathFluidHelper.getSourceFluid(storedFluid);
    }

    private static Optional<CustomFluidDefinition> getStoredCustomDefinition(Level level, BlockPos pos) {
        return Optional.ofNullable(HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos))
                .flatMap(CustomFluidAPI::getFluidDefinition);
    }

    private static boolean isStoredBathFluid(Player player, Fluid sourceFluid) {
        return getStoredBathFluid(player.level(), player.blockPosition()) == sourceFluid;
    }

    public static boolean isPlayerInHerbalBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HerbalBathBlock
                || isStoredBathFluid(player, FluidsRegister.HERBAL_BATH_FLUID.get());
    }

    public static boolean isPlayerInHoneyBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HoneyBathBlock
                || isStoredBathFluid(player, FluidsRegister.HONEY_BATH_FLUID.get());
    }

    public static boolean isPlayerInHotWaterBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HotWaterBlock
                || isStoredBathFluid(player, FluidsRegister.HOT_WATER_FLUID.get());
    }

    public static boolean isPlayerInPeonyBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof PeonyBathBlock
                || isStoredBathFluid(player, FluidsRegister.PEONY_BATH_FLUID.get());
    }

    public static boolean isPlayerInMilkBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof MilkBathBlock
                || isStoredBathFluid(player, FluidsRegister.MILK_BATH_FLUID.get());
    }

    public static boolean isPlayerInRoseBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof RoseBathBlock
                || isStoredBathFluid(player, FluidsRegister.ROSE_BATH_FLUID.get());
    }

    public static boolean isEntityInHerbalBathBlock(Entity entity) {
        BlockPos entityPos = entity.blockPosition();
        BlockState stateAtEntityPos = entity.level().getBlockState(entityPos);
        Fluid storedFluid = getStoredBathFluid(entity.level(), entityPos);
        return stateAtEntityPos.getBlock() instanceof HerbalBathBlock
                || storedFluid == FluidsRegister.HERBAL_BATH_FLUID.get();
    }
}
