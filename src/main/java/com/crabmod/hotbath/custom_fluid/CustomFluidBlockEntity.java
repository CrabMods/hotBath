package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.registers.BlockEntityRegister;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * BlockEntity for storing custom fluid data in placed fluid blocks.
 * This allows each fluid block to have its own fluid definition from data packs.
 */
public class CustomFluidBlockEntity extends BlockEntity {
    
    private static final String TAG_FLUID_ID = "FluidId";
    
    @Nullable
    private ResourceLocation fluidId;
    
    public CustomFluidBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.CUSTOM_FLUID_BLOCK_ENTITY.get(), pos, state);
    }
    
    /**
     * Sets the fluid ID for this block.
     * Also updates all connected flowing fluid blocks and schedules tick updates
     * for neighboring flowing fluids with different IDs so they can recalculate.
     */
    public void setFluidId(@Nullable ResourceLocation fluidId) {
        ResourceLocation oldFluidId = this.fluidId;
        this.fluidId = fluidId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Update all connected flowing fluid blocks
            updateConnectedFluidBlocks(fluidId);
            
            // If the fluid ID changed, schedule tick updates for neighboring flowing fluids
            // that have a different ID, so they can recalculate and disappear
            boolean idChanged = (oldFluidId == null && fluidId != null) 
                    || (oldFluidId != null && !oldFluidId.equals(fluidId));
            if (idChanged) {
                scheduleNeighborFluidUpdates(level, worldPosition);
            }
        }
    }
    
    /**
     * Schedule fluid tick updates for all neighboring blocks that contain flowing custom fluid
     * with a different customFluidId. This ensures that when a source block's ID is changed,
     * any adjacent flowing fluids with a different (or null) customFluidId will recalculate
     * and potentially disappear.
     */
    private void scheduleNeighborFluidUpdates(Level level, BlockPos pos) {
        // Check all 6 directions
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            
            // Check if neighbor is a dynamic custom fluid that is flowing (not source)
            if (!neighborFluid.isEmpty() 
                    && neighborFluid.getType() instanceof DynamicCustomFluid 
                    && !neighborFluid.isSource()) {
                // Schedule a tick for this flowing fluid to recalculate its state
                level.scheduleTick(neighborPos, neighborFluid.getType(), 1);
            }
        }
    }
    
    /**
     * Updates all connected custom fluid blocks with the new fluid ID.
     * Uses BFS to find all adjacent fluid blocks.
     * Only updates blocks that have no fluidId (flowing from this source) or the same fluidId.
     * Does NOT overwrite blocks that already have a different fluidId (different source).
     */
    private void updateConnectedFluidBlocks(@Nullable ResourceLocation newFluidId) {
        if (level == null || level.isClientSide) return;
        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        
        // Start from current position
        visited.add(worldPosition);
        
        // Add adjacent positions to queue
        for (Direction dir : Direction.values()) {
            queue.add(worldPosition.relative(dir));
        }
        
        int maxBlocks = 256; // Limit to prevent lag
        int processed = 0;
        
        while (!queue.isEmpty() && processed < maxBlocks) {
            BlockPos pos = queue.poll();
            if (visited.contains(pos)) continue;
            visited.add(pos);
            
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity fluidBe) {
                ResourceLocation existingId = fluidBe.fluidId;
                
                // Only update if:
                // 1. The block has no fluidId (null) - it's a flowing block from this source
                // 2. The block has the same fluidId - it's from the same source
                // Do NOT update if it has a different fluidId - it's from a different source
                boolean shouldUpdate = existingId == null || existingId.equals(newFluidId);
                
                if (shouldUpdate) {
                    fluidBe.fluidId = newFluidId;
                    fluidBe.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                    processed++;
                    
                    // Add adjacent positions to continue searching
                    for (Direction dir : Direction.values()) {
                        BlockPos adjacent = pos.relative(dir);
                        if (!visited.contains(adjacent)) {
                            queue.add(adjacent);
                        }
                    }
                }
                // If the block has a different fluidId, we don't update it and don't continue BFS from it
            }
        }
    }
    
    /**
     * Gets the fluid ID stored in this block.
     * If fluidId is null, attempts to recover it from waterlogging storage.
     */
    @Nullable
    public ResourceLocation getFluidId() {
        if (fluidId != null) {
            return fluidId;
        }
        // Fallback: check waterlogging storage for custom fluid ID
        // This prevents brief flash of default color when waterlogged block is broken
        if (level != null) {
            ResourceLocation storedId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, worldPosition);
            if (storedId != null) {
                this.fluidId = storedId;
            }
        }
        return fluidId;
    }
    
    /**
     * Gets the fluid definition for the stored fluid ID.
     * Uses getFluidId() which attempts to recover the ID from waterlogging storage if needed.
     */
    public Optional<CustomFluidDefinition> getFluidDefinition() {
        ResourceLocation id = getFluidId();
        if (id == null) {
            return Optional.empty();
        }
        return CustomFluidRegistry.getDefinition(id);
    }
    
    /**
     * Checks if the stored fluid is hot (temperature >= threshold).
     */
    public boolean isHot() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::isHot)
                .orElse(false);
    }
    
    /**
     * Checks if steam particles should be shown for this fluid.
     * Steam is shown when temperature >= HOT_TEMPERATURE_THRESHOLD (35°C) AND show_steam is true.
     */
    public boolean shouldShowSteam() {
        return getFluidDefinition()
                .map(def -> def.isHot() && def.showSteam())
                .orElse(false);
    }
    
    /**
     * Checks if general particles (splash, drip, etc.) should be shown for this fluid.
     * Controlled by the show_particles setting in the fluid definition.
     */
    public boolean shouldShowParticles() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::showParticles)
                .orElse(true);
    }
    
    /**
     * Checks if bubble particles should be shown for this fluid.
     * Controlled by the show_bubbles setting in the fluid definition.
     */
    public boolean shouldShowBubbles() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::showBubbles)
                .orElse(true);
    }
    
    /**
     * Gets the color of the stored fluid.
     */
    public int getFluidColor() {
        return getFluidDefinition()
                .map(CustomFluidDefinition::color)
                .orElse(0x45E1E9); // Default cyan color
    }
    
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (fluidId != null) {
            tag.putString(TAG_FLUID_ID, fluidId.toString());
        }
    }
    
    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_FLUID_ID)) {
            fluidId = ResourceLocation.tryParse(tag.getString(TAG_FLUID_ID));
        } else {
            fluidId = null;
        }
    }
    
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (fluidId != null) {
            tag.putString(TAG_FLUID_ID, fluidId.toString());
        }
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    /**
     * Called on client when receiving update packet from server (triggered by sendBlockUpdated).
     * This is the primary method for syncing BlockEntity data changes.
     */
    @Override
    public void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket packet, 
                             HolderLookup.@NotNull Provider lookupProvider) {
        ResourceLocation oldFluidId = this.fluidId;
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, lookupProvider);
        }
        
        // If fluid ID changed, trigger re-render and light update
        if (level != null && level.isClientSide) {
            boolean fluidChanged = (oldFluidId == null && fluidId != null) 
                    || (oldFluidId != null && !oldFluidId.equals(fluidId));
            if (fluidChanged) {
                // Force chunk re-render for fluid color update
                level.setBlocksDirty(worldPosition, Blocks.AIR.defaultBlockState(), getBlockState());
                // Also update light level
                scheduleLightUpdate();
            }
        }
    }
    
    /**
     * Called on client when chunk is loaded and BlockEntity data is received.
     * handleUpdateTag is for initial chunk load, onDataPacket is for updates.
     * This fixes the issue where custom fluids with luminosity > 0 would lose their light
     * after rejoining a server.
     */
    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadAdditional(tag, registries);
        // After loading the tag, schedule a light update
        scheduleLightUpdate();
    }
    
    /**
     * Schedules a light update for this block position.
     * Called after fluid data is loaded/synced to ensure proper light emission.
     */
    private void scheduleLightUpdate() {
        if (level != null && fluidId != null) {
            Optional<CustomFluidDefinition> defOpt = getFluidDefinition();
            if (defOpt.isPresent()) {
                // Trigger light engine to recompute light at this position
                level.getLightEngine().checkBlock(worldPosition);
            }
        }
    }
    
    /**
     * Called when the BlockEntity is loaded (both on initial placement and world reload).
     * This is the appropriate place to trigger light updates since the fluidId has been loaded.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        scheduleLightUpdate();
        // Also mark the block for update on server side
        if (level != null && !level.isClientSide && fluidId != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
