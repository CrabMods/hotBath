package com.crabmod.hotbath.util;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.HerbalBathBlock;
import com.crabmod.hotbath.fluid_blocks.HoneyBathBlock;
import com.crabmod.hotbath.fluid_blocks.HotWaterBlock;
import com.crabmod.hotbath.fluid_blocks.MilkBathBlock;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import com.crabmod.hotbath.fluid_blocks.RoseBathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CustomFluidHandler {

    public static boolean isPlayerHeadInHotBath(Player player) {
        BlockPos playerEyePos = BlockPos.containing(player.getEyePosition());
        BlockState stateAtPlayerPos = player.level().getBlockState(playerEyePos);
        return stateAtPlayerPos.getBlock() instanceof AbstractHotbathBlock;
    }

    public static boolean isPlayerInHotBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof AbstractHotbathBlock;
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
        return block instanceof AbstractHotbathBlock;
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
                return customBe.getFluidDefinition()
                        .map(CustomFluidDefinition::temperature)
                        .orElse(0f);
            }
            return 0;
        }
        
        // For built-in bath types, return default hot temperature
        if (block instanceof AbstractHotbathBlock) {
            return com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions
                    .forFluid(stateAtPlayerPos.getFluidState().getType(), player.level().isClientSide())
                    .map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::temperature).orElse(37.0f);
        }
        
        return 0;
    }

    public static boolean isPlayerInHerbalBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HerbalBathBlock;
    }

    public static boolean isPlayerInHoneyBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HoneyBathBlock;
    }

    public static boolean isPlayerInHotWaterBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof HotWaterBlock;
    }

    public static boolean isPlayerInPeonyBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof PeonyBathBlock;
    }

    public static boolean isPlayerInMilkBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        Block currentBlock = stateAtPlayerPos.getBlock();
        return stateAtPlayerPos.getBlock() instanceof MilkBathBlock;
    }

    public static boolean isPlayerInRoseBathBlock(Player player) {
        BlockPos playerPos = player.blockPosition();
        BlockState stateAtPlayerPos = player.level().getBlockState(playerPos);
        return stateAtPlayerPos.getBlock() instanceof RoseBathBlock;
    }

    public static boolean isEntityInHerbalBathBlock(Entity entity) {
        BlockPos entityPos = entity.blockPosition();
        BlockState stateAtEntityPos = entity.level().getBlockState(entityPos);
        return stateAtEntityPos.getBlock() instanceof HerbalBathBlock;
    }
}










