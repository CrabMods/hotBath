package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.ParticleGenerator;
import com.crabmod.hotbath.util.SoundHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.crabmod.hotbath.fluid_details.BaseFluidType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class AbstractHotbathBlock extends LiquidBlock {
    // Cache for bubble column direction to avoid repeated scans
    private static final Map<Long, CachedBubbleResult> BUBBLE_COLUMN_CACHE = new ConcurrentHashMap<>();
    private static final int BUBBLE_CACHE_TTL_TICKS = 20; // 1 second cache
    private static final int MAX_BUBBLE_SCAN_DEPTH = 64; // Limit scan depth
    private static final int MAX_SURFACE_SEARCH_DEPTH = 64; // Limit surface search
    private static final int CACHE_CLEANUP_INTERVAL = 100; // Check cleanup every 100 cache accesses
    private static final int MAX_CACHE_SIZE = 1000;
    private static final int TIME_BASED_CLEANUP_INTERVAL = 6000; // Clean up every 5 minutes (6000 ticks)
    private static final AtomicInteger cacheAccessCounter = new AtomicInteger(0);
    private static long lastTimeBasedCleanup = 0;

    private record CachedBubbleResult(int direction, long cacheTime) {}
    private static final String HOTBATH_UNDERWATER_STATE = "HotbathUnderwaterState";
    private static final String HOTBATH_ENTER_WATER_STATE = "HotbathEnterWaterState";

    protected AbstractHotbathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier.get(), properties);
    }

    /**
     * Checks if this bath fluid is considered "hot" (temperature >= 35鎺矯).
     * Hot baths cause damage to ice mobs and show steam effects.
     * Subclasses (like CustomFluidBlock) can override this based on their temperature.
     * 
     * @return true if this is a hot bath (default for built-in bath fluids)
     */
    public boolean isHotBath() {
        return true; // Built-in bath fluids are always hot
    }
    
    /**
     * Checks if this bath fluid at the given position is considered "hot".
     * This method can be overridden by subclasses that need position-based checks
     * (like DynamicCustomFluidBlock which uses BlockEntity to store temperature).
     * 
     * @param level The level
     * @param pos The position of the fluid block
     * @return true if this is a hot bath at this position
     */
    public boolean isHotBath(Level level, BlockPos pos) {
        return com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions
                .forFluid(defaultBlockState().getFluidState().getType(), level.isClientSide())
                .map(com.crabmod.hotbath.custom_fluid.BuiltinBathDefinition::isHot)
                .orElseGet(this::isHotBath);
    }


    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        com.crabmod.hotbath.api.HotBathApi.applyLiquidHazards(entity,isHotBath(level,pos));

        applyBathEffects(level, pos, entity);
        applyBubbleColumnPhysics(level, pos, entity);
    }

    public void applyWaterloggedEntityInside(Level level, BlockPos pos, Entity entity) {
        com.crabmod.hotbath.api.HotBathApi.applyLiquidHazards(entity,isHotBath(level,pos));
        applyBathEffects(level, pos, entity);
    }

    protected void applyBathEffects(Level level, BlockPos pos, Entity entity) {
    }



    private void applyBubbleColumnPhysics(Level level, BlockPos pos, Entity entity) {
        // Bubble column physics
        int direction = getBubbleColumnDirection(level, pos);
        if (direction != 0) {
            boolean dragDown = direction < 0;
            BlockState stateAbove = level.getBlockState(pos.above());
            if (stateAbove.isAir()) {
                entity.onAboveBubbleCol(dragDown);
            } else {
                entity.onInsideBubbleColumn(dragDown);
            }
        }
    }

    private int getBubbleColumnDirection(Level level, BlockPos pos) {
        // Use position-based cache key
        long posKey = pos.asLong();
        long currentTime = level.getGameTime();
        
        // Check cache first
        CachedBubbleResult cached = BUBBLE_COLUMN_CACHE.get(posKey);
        if (cached != null && (currentTime - cached.cacheTime()) < BUBBLE_CACHE_TTL_TICKS) {
            return cached.direction();
        }
        
        // Calculate direction
        int direction = calculateBubbleColumnDirection(level, pos);
        
        // Store in cache
        BUBBLE_COLUMN_CACHE.put(posKey, new CachedBubbleResult(direction, currentTime));
        
        // Periodically clean old cache entries
        int accessCount = cacheAccessCounter.incrementAndGet();
        boolean shouldCleanByCount = accessCount % CACHE_CLEANUP_INTERVAL == 0 && BUBBLE_COLUMN_CACHE.size() > MAX_CACHE_SIZE;
        boolean shouldCleanByTime = (currentTime - lastTimeBasedCleanup) > TIME_BASED_CLEANUP_INTERVAL;
        
        if (shouldCleanByCount || shouldCleanByTime) {
            BUBBLE_COLUMN_CACHE.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue().cacheTime()) > BUBBLE_CACHE_TTL_TICKS * 5);
            if (shouldCleanByTime) {
                lastTimeBasedCleanup = currentTime;
            }
        }
        
        return direction;
    }
    
    private int calculateBubbleColumnDirection(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        FluidType currentFluidType = level.getFluidState(pos).getFluidType();

        // Limit scan depth to avoid lag
        for (int i = 0; i < MAX_BUBBLE_SCAN_DEPTH; i++) {
            mutablePos.move(Direction.DOWN);
            BlockState state = level.getBlockState(mutablePos);
            if (state.is(Blocks.SOUL_SAND)) return 1;
            if (state.is(Blocks.MAGMA_BLOCK)) return -1;

            // Stop if we hit a solid block or a different fluid
            if (!state.is(this) && state.getFluidState().getFluidType() != currentFluidType) return 0;
        }
        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(
            @NotNull BlockState stateIn,
            @NotNull Level worldIn,
            @NotNull BlockPos pos,
            @NotNull RandomSource rand) {

        // Generate steam particles only if shouldShowSteam returns true
        // Default is true for hardcoded hot baths, can be overridden by DynamicCustomFluidBlock
        if (shouldShowSteam(worldIn, pos)) {
            generateSteamParticles(worldIn, pos, rand);
        }

        // Bubble column particles - only if shouldShowBubbles returns true
        int direction = getBubbleColumnDirection(worldIn, pos);
        if (direction != 0 && shouldShowBubbles(worldIn, pos)) {
            FluidType fluidType = stateIn.getFluidState().getFluidType();
            ParticleOptions bubbleParticle = null;
            if (fluidType instanceof BaseFluidType baseFluidType) {
                bubbleParticle = baseFluidType.getBubbleParticle();
            }

            if (direction > 0) {
                 if (bubbleParticle == null) bubbleParticle = ParticleTypes.BUBBLE_COLUMN_UP;

                 // Use addAlwaysVisibleParticle so bubbles are visible through the water surface from above
                 worldIn.addAlwaysVisibleParticle(bubbleParticle, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0D, 0.04D, 0.0D);
                 worldIn.addAlwaysVisibleParticle(bubbleParticle, pos.getX() + rand.nextFloat(), pos.getY() + rand.nextFloat(), pos.getZ() + rand.nextFloat(), 0.0D, 0.04D, 0.0D);
                 if (rand.nextInt(200) == 0) {
                     worldIn.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
                 }
            } else {
                 if (bubbleParticle == null) bubbleParticle = ParticleTypes.CURRENT_DOWN;

                 worldIn.addAlwaysVisibleParticle(bubbleParticle, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 0.0D, -0.04D, 0.0D);
                 if (rand.nextInt(200) == 0) {
                     worldIn.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
                 }
            }
        }

        // Optimization: Only process the local client player instead of iterating all players
        // This is client-side code, so we only need to handle the local player's sounds
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        
        // Set the maximum distance in squared units to avoid unnecessary checks
        final double maxDistanceSqr = 3.0 * 3.0;
        
        // Calculate the squared distance between the local player and the block position
        if (localPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxDistanceSqr) {
            return; // Skip if the player is too far from the hot bath block
        }

        // Check if the player's head is underwater, if so, don't play ambient sound
        boolean isPlayerHeadUnderwater = CustomFluidHandler.isPlayerHeadInHotBath(localPlayer);
        if (!isPlayerHeadUnderwater) {
            // Play ambient water sound only if the player is not completely underwater
            SoundHandler.playAmbientWaterSound(worldIn, pos, rand);
        }

        // Only handle underwater and water entry/exit states if the player is within range
        handleClientPlayerUnderwaterState(localPlayer, rand);
        handleClientPlayerEnterWaterState(localPlayer, rand);
    }

    // Handle the logic for when a player enters the water for the first time and exits completely
    private void handleClientPlayerEnterWaterState(Player player, RandomSource rand) {
        CompoundTag playerData = player.getPersistentData();

        // Check if the player's feet are in hot bath fluid (not necessarily the head)
        boolean isPlayerInWater = CustomFluidHandler.isPlayerInHotBathBlock(player);

        if (isPlayerInWater) {
            // If the player is in water and hasn't triggered the entry sound yet
            if (!playerData.getBoolean(HOTBATH_ENTER_WATER_STATE)) {
                SoundHandler.playEnterWaterSound(player, rand); // Play entry sound
                playerData.putBoolean(
                        HOTBATH_ENTER_WATER_STATE, true); // Set the player as having entered water
                spawnSplashParticles(player, player.level(), rand);
            }
        } else {
            // If the player has exited the water, reset the state to allow entry sound to play again
            if (playerData.getBoolean(HOTBATH_ENTER_WATER_STATE)) {
                playerData.putBoolean(HOTBATH_ENTER_WATER_STATE, false); // Reset the entry state
            }
        }
    }

    private void spawnSplashParticles(Player player, Level level, RandomSource rand) {
        BlockPos pos = player.blockPosition();
        // Find the actual surface level with depth limit to avoid performance issues
        BlockPos surfacePos = pos;
        int searchCount = 0;
        while (level.getBlockState(surfacePos.above()).getBlock() instanceof AbstractHotbathBlock 
               && surfacePos.getY() < level.getMaxBuildHeight()
               && searchCount < MAX_SURFACE_SEARCH_DEPTH) {
            surfacePos = surfacePos.above();
            searchCount++;
        }

        BlockState state = level.getBlockState(surfacePos);
        FluidType fluidType = state.getFluidState().getFluidType();

        if (fluidType instanceof BaseFluidType baseFluidType) {
            ParticleOptions bubble = baseFluidType.getBubbleParticle();
            if (bubble != null) {
                float width = player.getBbWidth();
                // Calculate max count based on player width
                int maxCount = (int) (20.0F + width * 10.0F);

                // Scale particle count based on fall distance (entry height)
                // Max count is reached at 3.0 blocks fall distance
                float factor = net.minecraft.util.Mth.clamp(player.fallDistance / 3.0F, 0.0F, 1.0F);
                int count = (int) (maxCount * factor);

                if (count <= 0) return;

                float fluidHeight = state.getFluidState().getHeight(level, surfacePos);
                // Spawn slightly below surface to ensure they are in fluid
                double surfaceY = surfacePos.getY() + fluidHeight - 0.05D;

                for (int i = 0; i < count; i++) {
                    // Random position within player width
                    double r = width * (0.5D + rand.nextDouble() * 0.5D); // 0.5 to 1.0 times width
                    double angle = rand.nextDouble() * 2.0D * Math.PI;

                    double offsetX = Math.cos(angle) * r;
                    double offsetZ = Math.sin(angle) * r;

                    double x = player.getX() + offsetX;
                    double z = player.getZ() + offsetZ;

                    // Velocity:
                    // Outward horizontal velocity to simulate scattering
                    double speed = 0.02D + rand.nextDouble() * 0.08D;
                    double vx = Math.cos(angle) * speed;
                    double vz = Math.sin(angle) * speed;

                    // Downward vertical velocity to simulate air being pushed down
                    // This allows bubbles to travel down then float up
                    double vy = -0.05D - rand.nextDouble() * 0.1D;

                    // Add player's momentum
                    vx += player.getDeltaMovement().x * 0.2D;
                    vz += player.getDeltaMovement().z * 0.2D;

                    // Note: We multiply by 5.0 because HotBathBubbleParticle multiplies by 0.2
                    level.addParticle(bubble, x, surfaceY, z, vx * 5.0D, vy * 5.0D, vz * 5.0D);
                }

                // Add some center turbulence
                int turbulenceCount = (int) (10 * factor);
                for (int i = 0; i < turbulenceCount; i++) {
                     double x = player.getX() + (rand.nextDouble() - 0.5D) * width;
                     double z = player.getZ() + (rand.nextDouble() - 0.5D) * width;
                     double vy = -0.1D - rand.nextDouble() * 0.2D;
                     level.addParticle(bubble, x, surfaceY, z,
                        (rand.nextDouble() - 0.5D) * 0.2D,
                        vy * 5.0D,
                        (rand.nextDouble() - 0.5D) * 0.2D);
                }
            }
        }
    }

    // Handle the underwater state of the player, controlling sound for entering and exiting
    // underwater
    private void handleClientPlayerUnderwaterState(Player player, RandomSource rand) {
        CompoundTag playerData = player.getPersistentData();

        // Check if the player's head is in the hot bath fluid (completely underwater)
        boolean isPlayerHeadUnderwater = CustomFluidHandler.isPlayerHeadInHotBath(player);

        if (isPlayerHeadUnderwater) {
            // If the player just went underwater
            if (!playerData.getBoolean(HOTBATH_UNDERWATER_STATE)) {
                SoundHandler.playUnderwaterEnterSound(player, rand); // Play underwater entry sound
                playerData.putBoolean(HOTBATH_UNDERWATER_STATE, true); // Mark the player as underwater
            } else {
                SoundHandler.playUnderwaterLoopSound(player, rand); // Play underwater loop sound
            }
        } else {
            // If the player has exited from being underwater
            if (playerData.getBoolean(HOTBATH_UNDERWATER_STATE)) {
                SoundHandler.playExitWaterSound(player, rand); // Play exit water sound
                SoundHandler.stopUnderwaterLoopSound(player); // Stop underwater loop sound
                playerData.putBoolean(HOTBATH_UNDERWATER_STATE, false); // Reset the underwater state
            }
        }
    }

    /**
     * Determines if steam particles should be shown for this fluid block.
     * Built-in steam follows the existing reloadable temperature definition.
     * Can be overridden by subclasses (e.g., DynamicCustomFluidBlock) to check temperature.
     */
    @OnlyIn(Dist.CLIENT)
    protected boolean shouldShowSteam(Level level, BlockPos pos) {
        return isHotBath(level, pos);
    }
    
    /**
     * Determines if bubble particles should be shown for this fluid block.
     * Custom fluids use their existing show_bubbles setting.
     * Can be overridden by subclasses (e.g., DynamicCustomFluidBlock) to check showBubbles setting.
     */
    @OnlyIn(Dist.CLIENT)
    protected boolean shouldShowBubbles(Level level, BlockPos pos) {
        return true; // Default: all hardcoded hot baths show bubbles
    }

    // Generate steam particles around the block if adjacent blocks are air
    private void generateSteamParticles(Level worldIn, BlockPos pos, RandomSource rand) {
        BlockPos[] adjacentPositions =
                new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()};

        int airBlockCount = 0;
        BlockPos[] airBlocks = new BlockPos[adjacentPositions.length];

        // Check for air blocks adjacent to the current position
        for (BlockPos adjacentPos : adjacentPositions) {
            if (worldIn.getBlockState(adjacentPos).isAir()) {
                airBlocks[airBlockCount++] = adjacentPos;
            }
        }

        // If there are air blocks, randomly select one and generate steam particles
        if (airBlockCount > 0) {
            BlockPos selectedPos = airBlocks[rand.nextInt(airBlockCount)];
            ParticleGenerator.renderDefaultSteam((ClientLevel) worldIn, selectedPos, rand);
        }
    }
}
