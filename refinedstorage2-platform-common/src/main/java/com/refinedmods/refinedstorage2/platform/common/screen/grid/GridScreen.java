package com.refinedmods.refinedstorage2.platform.common.screen.grid;

import com.refinedmods.refinedstorage2.platform.common.containermenu.grid.GridContainerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.refinedmods.refinedstorage2.platform.common.util.IdentifierUtil.createIdentifier;

public class GridScreen extends AbstractGridScreen<GridContainerMenu> {
    private static final ResourceLocation TEXTURE = createIdentifier("textures/gui/grid.png");

    public GridScreen(final GridContainerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, 99);
        this.inventoryLabelY = 75;
        this.imageWidth = 227;
        this.imageHeight = 176;
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
