package com.refinedmods.refinedstorage2.core.network;

import com.refinedmods.refinedstorage2.core.network.component.NetworkComponent;
import com.refinedmods.refinedstorage2.core.network.component.NetworkComponentRegistry;
import com.refinedmods.refinedstorage2.core.network.host.NetworkNodeHost;

import java.util.Map;

public class NetworkImpl implements Network {
    private final Map<Class<? extends NetworkComponent>, NetworkComponent> components;

    public NetworkImpl(NetworkComponentRegistry networkComponentRegistry) {
        components = networkComponentRegistry.buildComponentMap(this);
    }

    @Override
    public <T extends NetworkComponent> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    @Override
    public void addHost(NetworkNodeHost<?> host) {
        components.values().forEach(c -> c.onHostAdded(host));
    }

    @Override
    public void removeHost(NetworkNodeHost<?> host) {
        components.values().forEach(c -> c.onHostRemoved(host));
    }
}
