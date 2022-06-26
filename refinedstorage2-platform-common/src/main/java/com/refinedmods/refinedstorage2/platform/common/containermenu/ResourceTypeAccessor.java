package com.refinedmods.refinedstorage2.platform.common.containermenu;

import com.refinedmods.refinedstorage2.platform.api.resource.filter.ResourceType;

import javax.annotation.Nullable;

public interface ResourceTypeAccessor {
    @Nullable
    ResourceType getCurrentResourceType();

    ResourceType toggleResourceType();
}
