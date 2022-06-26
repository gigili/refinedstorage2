package com.refinedmods.refinedstorage2.platform.fabric.integration.jei;

import com.refinedmods.refinedstorage2.platform.api.resource.filter.FilteredResource;
import com.refinedmods.refinedstorage2.platform.api.resource.filter.ResourceType;
import com.refinedmods.refinedstorage2.platform.common.Platform;
import com.refinedmods.refinedstorage2.platform.common.containermenu.AbstractResourceFilterContainerMenu;
import com.refinedmods.refinedstorage2.platform.common.containermenu.slot.ResourceFilterSlot;
import com.refinedmods.refinedstorage2.platform.common.screen.AbstractBaseScreen;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class GhostIngredientHandler implements IGhostIngredientHandler<AbstractBaseScreen> {
    @Override
    public <I> @NotNull List<Target<I>> getTargets(final AbstractBaseScreen gui,
                                                   final I ingredient,
                                                   final boolean doStart) {
        if (gui.getMenu() instanceof AbstractResourceFilterContainerMenu menu) {
            return getTargets(gui, ingredient, menu);
        }
        return Collections.emptyList();
    }

    private <I> List<Target<I>> getTargets(final AbstractBaseScreen gui,
                                           final I ingredient,
                                           final AbstractResourceFilterContainerMenu menu) {
        final ResourceType resourceType = menu.getCurrentResourceType();
        if (resourceType == null) {
            return Collections.emptyList();
        }
        return menu.slots.stream()
            .filter(ResourceFilterSlot.class::isInstance)
            .map(ResourceFilterSlot.class::cast)
            .flatMap(slot -> resourceType.translate(ingredient).map(filteredResource -> {
                final Rect2i bounds = getBounds(gui, slot);
                return new TargetImpl<I>(bounds, slot.index, filteredResource);
            }).stream())
            .collect(Collectors.toList());
    }

    private Rect2i getBounds(final AbstractBaseScreen gui, final ResourceFilterSlot slot) {
        return new Rect2i(gui.getLeftPos() + slot.x, gui.getTopPos() + slot.y, 17, 17);
    }

    @Override
    public void onComplete() {
        // no op
    }

    private static class TargetImpl<I> implements Target<I> {
        private final Rect2i area;
        private final int slotIndex;
        private final FilteredResource filteredResource;

        TargetImpl(final Rect2i area, final int slotIndex, final FilteredResource filteredResource) {
            this.area = area;
            this.slotIndex = slotIndex;
            this.filteredResource = filteredResource;
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(final I ingredient) {
            Platform.INSTANCE.getClientToServerCommunications()
                .sendResourceFilterSlotChange(slotIndex, filteredResource);
        }
    }
}
