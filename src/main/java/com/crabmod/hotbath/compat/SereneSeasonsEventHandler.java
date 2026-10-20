package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.api.HotBathApi;
import com.crabmod.hotbath.util.CustomFluidHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Seasonal liquid benefits share the same measured contact in world fluids and containers. */
public final class SereneSeasonsEventHandler {
    private static final int PLAYER_INTERVAL=40;
    private static final int MOB_INTERVAL=100;
    private static final int DURATION=200;
    private SereneSeasonsEventHandler() {}

    @SubscribeEvent public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer player&&player.tickCount%PLAYER_INTERVAL==0)
            CompatManager.safeEventCall("sereneseasons","onPlayerTick",()->applyWinterBenefits(player));
    }
    @SubscribeEvent public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        var living=event.getEntity();
        if(!(living instanceof Player)&&living.tickCount%MOB_INTERVAL==0)
            CompatManager.safeEventCall("sereneseasons","onLivingTick",()->applyWinterBenefits(living));
    }

    private static void applyWinterBenefits(LivingEntity entity) {
        if(entity.level().isClientSide()||!entity.isAlive()||!HotBathApi.allowsModIntegrations(entity))return;
        var contact=HotBathApi.currentBathContact(entity);
        if(contact.isPresent()&&!contact.get().bath())return;
        if(!CustomFluidHandler.isEntityInHotBath(entity))return;
        var state=SeasonHelper.getSeasonState(entity.level());
        if(state==null||state.getSeason()!=Season.WINTER)return;
        boolean midWinter=state.getSubSeason()==Season.SubSeason.MID_WINTER;
        boolean ambient=!(entity instanceof Player);
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,DURATION,midWinter?1:0,ambient,true,true));
        if(midWinter)entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,DURATION,0,ambient,true,true));
    }
}
