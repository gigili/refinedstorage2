package com.refinedmods.refinedstorage2.api.network.impl.node.grid;

import com.refinedmods.refinedstorage2.api.core.CoreValidations;
import com.refinedmods.refinedstorage2.api.grid.GridWatcher;
import com.refinedmods.refinedstorage2.api.grid.service.GridService;
import com.refinedmods.refinedstorage2.api.grid.service.GridServiceFactory;
import com.refinedmods.refinedstorage2.api.grid.service.GridServiceImpl;
import com.refinedmods.refinedstorage2.api.network.component.StorageNetworkComponent;
import com.refinedmods.refinedstorage2.api.network.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage2.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage2.api.resource.list.listenable.ResourceListListener;
import com.refinedmods.refinedstorage2.api.storage.Actor;
import com.refinedmods.refinedstorage2.api.storage.channel.StorageChannel;
import com.refinedmods.refinedstorage2.api.storage.channel.StorageChannelType;
import com.refinedmods.refinedstorage2.api.storage.tracked.TrackedResource;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
import javax.annotation.Nullable;

public class GridNetworkNode extends AbstractNetworkNode implements GridServiceFactory {
    private final Collection<? extends StorageChannelType<?>> storageChannelTypes;
    private final Set<GridWatcher> watchers = new HashSet<>();
    private final Map<GridWatcher, Map<StorageChannelType<?>, ResourceListListener<?>>> storageChannelListeners =
        new HashMap<>();
    private final long energyUsage;

    public GridNetworkNode(final long energyUsage,
                           final Collection<? extends StorageChannelType<?>> storageChannelTypes) {
        this.energyUsage = energyUsage;
        this.storageChannelTypes = storageChannelTypes;
    }

    private <T> StorageChannel<T> getStorageChannel(final StorageChannelType<T> type) {
        if (network == null) {
            throw new IllegalStateException("Network must be present to retrieve storage channel");
        }
        return network.getComponent(StorageNetworkComponent.class).getStorageChannel(type);
    }

    public <T> List<GridResource<T>> getResources(final StorageChannelType<T> type,
                                                  final Class<? extends Actor> actorType) {
        final StorageChannel<T> storageChannel = getStorageChannel(type);
        return storageChannel.getAll().stream().map(resourceAmount -> new GridResource<>(
            resourceAmount,
            storageChannel.findTrackedResourceByActorType(resourceAmount.getResource(), actorType).orElse(null)
        )).toList();
    }

    @Override
    public long getEnergyUsage() {
        return energyUsage;
    }

    public void addWatcher(final GridWatcher watcher, final Class<? extends Actor> actorType) {
        CoreValidations.validateNotContains(watchers, watcher, "Watcher is already registered");
        storageChannelTypes.forEach(storageChannelType -> attachWatcherToStorageChannel(
            watcher,
            actorType,
            storageChannelType
        ));
        watchers.add(watcher);
    }

    private <T> void attachWatcherToStorageChannel(final GridWatcher watcher,
                                                   final Class<? extends Actor> actorType,
                                                   final StorageChannelType<T> storageChannelType) {
        final StorageChannel<T> storageChannel = getStorageChannel(storageChannelType);
        final ResourceListListener<T> listener = change -> watcher.onChanged(
            storageChannelType,
            change,
            storageChannel.findTrackedResourceByActorType(
                change.resourceAmount().getResource(),
                actorType
            ).orElse(null)
        );
        storageChannel.addListener(listener);
        storageChannelListeners.computeIfAbsent(watcher, k -> new HashMap<>()).put(storageChannelType, listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void removeWatcher(final GridWatcher watcher) {
        CoreValidations.validateContains(watchers, watcher, "Watcher is not registered");
        storageChannelListeners.get(watcher).forEach((type, listener) -> removeAttachedWatcherFromStorageChannel(
            (StorageChannelType) type,
            (ResourceListListener) listener
        ));
        storageChannelListeners.remove(watcher);
        watchers.remove(watcher);
    }

    private <T> void removeAttachedWatcherFromStorageChannel(final StorageChannelType<T> type,
                                                             final ResourceListListener<T> listener) {
        final StorageChannel<T> storageChannel = getStorageChannel(type);
        storageChannel.removeListener(listener);
    }

    @Override
    protected void onActiveChanged(final boolean newActive) {
        super.onActiveChanged(newActive);
        watchers.forEach(watcher -> watcher.onActiveChanged(newActive));
    }

    @Override
    public <T> GridService<T> create(
        final StorageChannelType<T> storageChannelType,
        final Actor actor,
        final ToLongFunction<T> maxAmountProvider,
        final long singleAmount
    ) {
        final StorageChannel<T> storageChannel = getStorageChannel(storageChannelType);
        return new GridServiceImpl<>(storageChannel, actor, maxAmountProvider, singleAmount);
    }

    public record GridResource<T>(
        ResourceAmount<T> resourceAmount,
        @Nullable TrackedResource trackedResource
    ) {
    }
}
