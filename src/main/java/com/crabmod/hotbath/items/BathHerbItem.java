package com.crabmod.hotbath.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Item with custom tooltip
 */
public final class BathHerbItem extends net.minecraft.world.item.BlockItem {
    public BathHerbItem(Properties properties) {
        super(com.crabmod.hotbath.registers.BlocksRegister.BATH_HERB_CROP.get(), properties);
    }

    @Override
    public String getDescriptionId() { return "item.hotbath.bath_herb"; }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
