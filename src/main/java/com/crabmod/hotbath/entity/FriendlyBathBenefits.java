package com.crabmod.hotbath.entity;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.custom_fluid.BuiltinBathDefinitions;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

@Mod.EventBusSubscriber(modid=HotBath.MOD_ID)
public final class FriendlyBathBenefits {
    public static final TagKey<EntityType<?>> INCLUDED = tag("bath_beneficiaries");
    public static final TagKey<EntityType<?>> EXCLUDED = tag("excluded_bath_beneficiaries");
    private static final Map<LivingEntity,Integer> PEONY_EXIT_TIMERS = new WeakHashMap<>();
    private FriendlyBathBenefits() {}
    private static TagKey<EntityType<?>> tag(String path){return TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("hotbath:"+path));}

    /** Non-hostile mobs are automatic; packs can describe exceptional NPC types. */
    public static boolean isEligible(LivingEntity entity) {
        if(!entity.isAlive()||HotBathConfig.isCompatibilityModeEnabled()||entity instanceof Player||entity.getType().is(EXCLUDED))return false;
        if(entity.getType().is(INCLUDED))return true;
        return entity instanceof Mob mob && !(entity instanceof Enemy)
                && entity.getType().getCategory()!=MobCategory.MONSTER && !(mob.getTarget() instanceof Player);
    }

    public static void refreshPeony(LivingEntity entity){PEONY_EXIT_TIMERS.put(entity,0);}

    @SubscribeEvent public static void onEntityTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        Entity subject=event.getEntity();
        if(!(subject instanceof LivingEntity entity)||entity instanceof Player||entity.level().isClientSide())return;
        Integer previous=PEONY_EXIT_TIMERS.get(entity);
        if(previous!=null) {
            int elapsed=previous+1;var bonuses=BuiltinBathDefinitions.get("peony_bath_fluid").bath().peony();
            int attack=bonuses==null?0:bonuses.attackSpeedLingerTicks(),knockback=bonuses==null?0:bonuses.knockbackLingerTicks();
            if(elapsed>=attack)PeonyBathBlock.applyAttributeModifier(entity,Attributes.ATTACK_SPEED,0,PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,false,AttributeModifier.Operation.MULTIPLY_TOTAL);
            if(elapsed>=knockback)PeonyBathBlock.applyAttributeModifier(entity,Attributes.KNOCKBACK_RESISTANCE,0,PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,false,AttributeModifier.Operation.ADDITION);
            if(elapsed>=Math.max(attack,knockback))PEONY_EXIT_TIMERS.remove(entity);else PEONY_EXIT_TIMERS.put(entity,elapsed);
        }
    }

    @SubscribeEvent public static void onLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        Entity subject=event.getEntity();
        if(!(subject instanceof LivingEntity entity)||entity instanceof Player||entity.level().isClientSide())return;
        com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker.MOB_AREA_STATE_CACHE.remove(entity);
        if(PEONY_EXIT_TIMERS.remove(entity)!=null) {
            PeonyBathBlock.applyAttributeModifier(entity,Attributes.ATTACK_SPEED,0,PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,false,AttributeModifier.Operation.MULTIPLY_TOTAL);
            PeonyBathBlock.applyAttributeModifier(entity,Attributes.KNOCKBACK_RESISTANCE,0,PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,false,AttributeModifier.Operation.ADDITION);
        }
    }
}
