package com.refinedmods.refinedstorage2.platform.fabric.integration.rei;

import com.refinedmods.refinedstorage2.platform.api.resource.filter.FilteredResource;
import com.refinedmods.refinedstorage2.platform.api.resource.filter.ResourceType;
import com.refinedmods.refinedstorage2.platform.common.Platform;
import com.refinedmods.refinedstorage2.platform.common.containermenu.AbstractResourceFilterContainerMenu;
import com.refinedmods.refinedstorage2.platform.common.containermenu.slot.ResourceFilterSlot;
import com.refinedmods.refinedstorage2.platform.common.screen.AbstractBaseScreen;

import java.util.Optional;
import java.util.stream.Stream;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;

public class DraggableStackVisitorImpl
    implements DraggableStackVisitor<AbstractBaseScreen<? extends AbstractResourceFilterContainerMenu>> {
    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
        final DraggingContext<AbstractBaseScreen<? extends AbstractResourceFilterContainerMenu>> context,
        final DraggableStack stack
    ) {
        final var screen = context.getScreen();
        final var menu = screen.getMenu();
        final ResourceType resourceType = menu.getCurrentResourceType();
        if (resourceType == null) {
            return Stream.empty();
        }
        final var value = stack.getStack().getValue();
        return menu.slots.stream()
            .filter(ResourceFilterSlot.class::isInstance)
            .map(ResourceFilterSlot.class::cast)
            .flatMap(slot -> resourceType.translate(value).map(
                filteredResource -> BoundsProvider.ofRectangle(toRectangle(screen, slot))
            ).stream());
    }

    private static Rectangle toRectangle(final AbstractBaseScreen<? extends AbstractResourceFilterContainerMenu> screen,
                                         final ResourceFilterSlot slot) {
        return new Rectangle(
            screen.getLeftPos() + slot.x,
            screen.getTopPos() + slot.y,
            18,
            18
        );
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(
        final DraggingContext<AbstractBaseScreen<? extends AbstractResourceFilterContainerMenu>> context,
        final DraggableStack stack
    ) {
        final var screen = context.getScreen();
        final var menu = screen.getMenu();
        final ResourceType resourceType = menu.getCurrentResourceType();
        if (resourceType == null) {
            return DraggedAcceptorResult.PASS;
        }
        final Object value = stack.getStack().getValue();
        final Optional<FilteredResource> mapped = resourceType.translate(value);
        if (mapped.isEmpty()) {
            return DraggedAcceptorResult.PASS;
        }
        for (final Slot slot : menu.slots) {
            if (!(slot instanceof ResourceFilterSlot resourceFilterSlot)) {
                continue;
            }
            final Rectangle slotRect = toRectangle(screen, resourceFilterSlot);
            if (slotRect.contains(context.getCurrentPosition())) {
                Platform.INSTANCE.getClientToServerCommunications().sendResourceFilterSlotChange(
                    slot.index,
                    mapped.get()
                );
                return DraggedAcceptorResult.ACCEPTED;
            }
        }
        return DraggedAcceptorResult.PASS;
    }

    @Override
    public <R extends Screen> boolean isHandingScreen(final R screen) {
        return screen instanceof AbstractBaseScreen<?>
            && ((AbstractBaseScreen<?>) screen).getMenu() instanceof AbstractResourceFilterContainerMenu;
    }
}
