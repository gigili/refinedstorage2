package com.refinedmods.refinedstorage2.core.network.node;

import com.refinedmods.refinedstorage2.core.Rs2World;
import com.refinedmods.refinedstorage2.core.network.Network;
import com.refinedmods.refinedstorage2.core.network.component.EnergyNetworkComponent;
import com.refinedmods.refinedstorage2.core.util.Action;
import com.refinedmods.refinedstorage2.core.util.Position;

public abstract class NetworkNodeImpl implements NetworkNode {
    protected final Rs2World world;
    protected final Position position;
    protected Network network;
    protected RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private boolean wasActive;

    public NetworkNodeImpl(Rs2World world, Position position) {
        this.world = world;
        this.position = position;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setNetwork(Network network) {
        this.network = network;
    }

    public boolean isActive() {
        return redstoneMode.isActive(world.isPowered(position)) && network.getComponent(EnergyNetworkComponent.class).getEnergyStorage().getStored() > 0;
    }

    protected void onActiveChanged(boolean active) {
    }

    @Override
    public void update() {
        network.getComponent(EnergyNetworkComponent.class).getEnergyStorage().extract(getEnergyUsage(), Action.EXECUTE);

        if (wasActive != isActive()) {
            wasActive = isActive();

            onActiveChanged(wasActive);
        }
    }

    protected abstract long getEnergyUsage();

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }
}
