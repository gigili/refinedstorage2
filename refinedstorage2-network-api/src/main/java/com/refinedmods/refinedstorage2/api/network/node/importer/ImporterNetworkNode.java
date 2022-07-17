package com.refinedmods.refinedstorage2.api.network.node.importer;

import com.refinedmods.refinedstorage2.api.core.filter.Filter;
import com.refinedmods.refinedstorage2.api.core.filter.FilterMode;
import com.refinedmods.refinedstorage2.api.network.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage2.api.network.node.NetworkNodeActor;
import com.refinedmods.refinedstorage2.api.storage.Actor;

import java.util.Set;
import javax.annotation.Nullable;

public class ImporterNetworkNode extends AbstractNetworkNode {
    private final long energyUsage;
    private final Filter filter = new Filter();
    private final Actor actor = new NetworkNodeActor(this);

    @Nullable
    private ImporterTransferStrategy transferStrategy;

    public ImporterNetworkNode(final long energyUsage) {
        this.energyUsage = energyUsage;
    }

    public <T> void setTransferStrategy(@Nullable final ImporterTransferStrategy transferStrategy) {
        this.transferStrategy = transferStrategy;
    }

    @Override
    public void update() {
        super.update();
        if (transferStrategy != null && isActive()) {
            transferStrategy.transfer(filter, actor);
        }
    }

    public FilterMode getFilterMode() {
        return filter.getMode();
    }

    public void setFilterMode(final FilterMode mode) {
        filter.setMode(mode);
    }

    public void setFilterTemplates(final Set<Object> templates) {
        filter.setTemplates(templates);
    }

    @Override
    public long getEnergyUsage() {
        return energyUsage;
    }
}