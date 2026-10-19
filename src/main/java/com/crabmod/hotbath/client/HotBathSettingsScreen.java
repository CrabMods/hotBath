package com.crabmod.hotbath.client;

import com.crabmod.hotbath.HotBathClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Display settings opened from the loader's Mods menu. */
public final class HotBathSettingsScreen extends Screen {
    private final Screen parent;

    public HotBathSettingsScreen(Screen parent) {
        super(Component.translatable("hotbath.settings.title"));
        this.parent = parent;
    }

    public static void register(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (mod, parent) -> new HotBathSettingsScreen(parent));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(label("dirt", HotBathClientConfig.SHOW_DIRT.get()), button -> {
            HotBathClientConfig.SHOW_DIRT.set(!HotBathClientConfig.SHOW_DIRT.get());
            HotBathClientConfig.SPEC.save();
            button.setMessage(label("dirt", HotBathClientConfig.SHOW_DIRT.get()));
        }).bounds(width / 2 - 110, height / 2 - 24, 220, 20).build());
        addRenderableWidget(Button.builder(label("flies", HotBathClientConfig.SHOW_FLIES.get()), button -> {
            HotBathClientConfig.SHOW_FLIES.set(!HotBathClientConfig.SHOW_FLIES.get());
            HotBathClientConfig.SPEC.save();
            button.setMessage(label("flies", HotBathClientConfig.SHOW_FLIES.get()));
        }).bounds(width / 2 - 110, height / 2 + 4, 220, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 110, height / 2 + 48, 220, 20).build());
    }

    private static Component label(String option, boolean enabled) {
        return Component.translatable("hotbath.settings." + option,
                enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(font, title, width / 2, height / 2 - 76, 0xFFFFFF);
        gui.drawCenteredString(font, Component.translatable("hotbath.settings.description"),
                width / 2, height / 2 - 52, 0xAAAAAA);
    }
}
