package com.refinedmods.refinedstorage2.platform.fabric.integration.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;

public class ReiClientPluginImpl implements REIClientPlugin {
    @Override
    public void registerScreens(final ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new DraggableStackVisitorImpl());
    }
}
