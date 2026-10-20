package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Peony Bath Block
 */
public class PeonyBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public PeonyBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    public static final ResourceLocation ATTACK_SPEED_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "peony_bath_attack_speed_modifier");

    public static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "peony_bath_knockback_resistance_modifier");

    @Override
    protected void applyBathEffects(Level level, BlockPos pos, Entity entity) {
        com.crabmod.hotbath.custom_fluid.BuiltinBathEffects.bathe("peony_bath_fluid", entity);
    }

    public static void applyAttributeModifier(
            ServerPlayer player,
            Holder<Attribute> attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        applyAttributeModifier((net.minecraft.world.entity.LivingEntity) player, attribute, value, modifierName, add, operation);
    }

    public static void applyAttributeModifier(
            net.minecraft.world.entity.LivingEntity player,
            Holder<Attribute> attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);

        if (attributeInstance != null) {
            if (add) {
                AttributeModifier modifier = new AttributeModifier(modifierName, value, operation);
                AttributeModifier previous = attributeInstance.getModifier(modifierName);
                if (previous == null || previous.amount() != value || previous.operation() != operation) {
                    attributeInstance.removeModifier(modifierName);
                    attributeInstance.addTransientModifier(modifier);
                }
            } else {
                attributeInstance.removeModifier(modifierName);
            }
        }
    }
}
