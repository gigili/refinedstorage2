package com.refinedmods.refinedstorage2.platform.common.screen.widget;

import com.refinedmods.refinedstorage2.platform.api.resource.filter.ResourceType;
import com.refinedmods.refinedstorage2.platform.common.containermenu.ResourceTypeAccessor;

import javax.annotation.Nullable;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ResourceFilterButtonWidget extends Button {
    public static final int WIDTH = 50;

    public ResourceFilterButtonWidget(final int x, final int y, final ResourceTypeAccessor resourceTypeAccessor) {
        super(
            x,
            y,
            WIDTH,
            15,
            getName(resourceTypeAccessor),
            createPressAction(resourceTypeAccessor)
        );
    }

    private static Component getName(@Nullable final ResourceTypeAccessor resourceTypeAccessor) {
        final ResourceType resourceType = resourceTypeAccessor.getCurrentResourceType();
        if (resourceType == null) {
            return Component.empty();
        }
        return resourceType.getName();
    }

    private static OnPress createPressAction(final ResourceTypeAccessor resourceTypeAccessor) {
        return btn -> {
            final ResourceType toggled = resourceTypeAccessor.toggleResourceType();
            btn.setMessage(toggled.getName());
        };
    }
}
