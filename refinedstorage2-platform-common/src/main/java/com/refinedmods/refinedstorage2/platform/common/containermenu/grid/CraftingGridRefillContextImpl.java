package com.refinedmods.refinedstorage2.platform.common.containermenu.grid;

import com.refinedmods.refinedstorage2.platform.api.resource.ItemResource;
import com.refinedmods.refinedstorage2.platform.common.block.entity.grid.CraftingGridBlockEntity;

import net.minecraft.world.entity.player.Player;

public class CraftingGridRefillContextImpl implements CraftingGridRefillContext {
    private final CraftingGridBlockEntity blockEntity;

    public CraftingGridRefillContextImpl(final CraftingGridBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public boolean extract(final ItemResource resource, final Player player) {
        return blockEntity.extract(resource, 1, player) == 1;
    }

    @Override
    public void close() {
        // no op
    }
}
