package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Peony Bath Block
 */
@SuppressWarnings("deprecation")
public class PeonyBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public PeonyBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    @SuppressWarnings("removal")
    public static final ResourceLocation ATTACK_SPEED_MODIFIER_NAME =
        new ResourceLocation(HotBath.MOD_ID, "peony_bath_attack_speed_modifier");

    @SuppressWarnings("removal")
    public static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_NAME =
        new ResourceLocation(HotBath.MOD_ID, "peony_bath_knockback_resistance_modifier");

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.bathe("peony_bath_fluid", entity, this);
    }

    public static void applyAttributeModifier(
            ServerPlayer player,
            Attribute attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        applyAttributeModifier((net.minecraft.world.entity.LivingEntity) player, attribute, value, modifierName, add, operation);
    }

    public static void applyAttributeModifier(
            net.minecraft.world.entity.LivingEntity player,
            Attribute attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        UUID uuid = UUID.nameUUIDFromBytes(modifierName.toString().getBytes());

        if (attributeInstance != null) {
            if (add) {
                AttributeModifier modifier = new AttributeModifier(uuid, modifierName.toString(), value, operation);
                AttributeModifier previous = attributeInstance.getModifier(uuid);
                if (previous == null || previous.getAmount() != value || previous.getOperation() != operation) {
                    attributeInstance.removeModifier(uuid);
                    attributeInstance.addTransientModifier(modifier);
                }
            } else {
                attributeInstance.removeModifier(uuid);
            }
        }
    }
}
