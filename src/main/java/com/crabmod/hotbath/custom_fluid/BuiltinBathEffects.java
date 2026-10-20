package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.events.enter_fluid_events.PeonyBathEvents;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import com.crabmod.hotbath.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** Executes the existing bath mechanics with values supplied by a data pack. */
public final class BuiltinBathEffects {
    private static final Random RANDOM=new Random();
    private BuiltinBathEffects() {}

    public static void bathe(String id,Entity entity) {
        if(entity.level().isClientSide()||!entity.isAlive())return;
        var state=com.crabmod.hotbath.bathing.BathImmersion.contact(entity,"hotbath:"+id,10);
        if(!state.shouldProcess())return;
        com.crabmod.hotbath.api.HotBathApi.applyLiquidHazards(entity,BuiltinBathDefinitions.get(id).isHot());
        var profile=BuiltinBathDefinitions.get(id).bath();
        if(!(entity instanceof ServerPlayer)) {
            var damage=profile.undeadDamage();
            if(damage!=null && entity.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD) && entity.tickCount%damage.intervalTicks()==0)
                entity.hurt(entity.level().damageSources().magic(),damage.amount());
        }
        if(!(entity instanceof LivingEntity bather)||!bather.isAlive())return;
        ServerPlayer player=bather instanceof ServerPlayer p?p:null;
        if(player==null&&!com.crabmod.hotbath.entity.FriendlyBathBenefits.isEligible(bather))return;
        if(profile.healing()!=null) {
            if((state.stayedTicks()+1)%profile.healing().intervalTicks()==0)bather.heal(profile.healing().amount());
        }
        if(player!=null&&profile.hunger()!=null&&(state.stayedTicks()+1)%profile.hunger().intervalTicks()==0)player.getFoodData().eat((int)profile.hunger().amount(),0);
        apply(profile,bather,state.stayedTicks(),state.totalEnterCount());
        if(player!=null&&profile.advancement()!=null&&state.isFirstEnter()&&state.totalEnterCount()>=profile.advancement().entries())
            AdvancementHelper.tryAwardAdvancement(player,profile.advancement().id().toString(),"code_triggered");
        var peony=profile.peony();
        if(peony!=null&&state.stayedTicks()>=peony.afterTicks()) {
            PeonyBathBlock.applyAttributeModifier(bather,Attributes.KNOCKBACK_RESISTANCE,peony.knockbackResistance(),
                    PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,true,AttributeModifier.Operation.ADD_VALUE);
            PeonyBathBlock.applyAttributeModifier(bather,Attributes.ATTACK_SPEED,peony.attackSpeed(),
                    PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,true,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if(id.equals("peony_bath_fluid")) {
            if(player!=null)PeonyBathEvents.resetExitTimer(player.getUUID());
            else com.crabmod.hotbath.entity.FriendlyBathBenefits.refreshPeony(bather);
        }
    }

    public static void use(String id,LivingEntity entity,boolean splash) {
        if(entity.level().isClientSide())return;
        var definition=BuiltinBathDefinitions.get(id);
        apply(splash?definition.splash():definition.drink(),entity,0,0);
    }

    public static void pipe(String id,LivingEntity entity) {
        if(!entity.level().isClientSide())apply(BuiltinBathDefinitions.get(id).pipe(),entity,0,0);
    }

    private static void apply(BuiltinBathDefinition.Profile profile,LivingEntity entity,int ticks,int entries) {
        if(profile.extinguish()&&entity.isOnFire())entity.clearFire();
        if(profile.instantHealing()>0)entity.heal(profile.instantHealing());
        // Cleanse before adding positive effects, matching the original bath implementations.
        var cleansing=profile.cleansing();
        if(cleansing!=null && ticks>=cleansing.afterTicks()) {
            var matches=new ArrayList<MobEffectInstance>();
            for(var instance:entity.getActiveEffects()) {
                var effect=instance.getEffect().value();
                var id=BuiltInRegistries.MOB_EFFECT.getKey(effect);
                boolean category=cleansing.nonBeneficial()?!effect.isBeneficial():effect.getCategory()==MobEffectCategory.HARMFUL;
                if((category||(id!=null&&cleansing.include().contains(id)))&&!(id!=null&&cleansing.exclude().contains(id)))matches.add(instance);
            }
            if(cleansing.count()>=matches.size())matches.forEach(effect->entity.removeEffect(effect.getEffect()));
            else for(int i=0;i<cleansing.count();i++)entity.removeEffect(matches.remove(cleansing.random()?RANDOM.nextInt(matches.size()):0).getEffect());
        }
        for(var effect:profile.effects())
            if(ticks>=effect.afterTicks() && entries>=effect.minimumEntries())entity.addEffect(effect.create());
    }
}
