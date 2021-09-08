package com.refinedmods.refinedstorage2.api.storage.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class StorageTracker<T, I> {
    private final Function<T, I> idFactory;
    private final Supplier<Long> clock;
    private final Map<I, Entry> entries = new HashMap<>();

    public StorageTracker(Function<T, I> idFactory, Supplier<Long> clock) {
        this.idFactory = idFactory;
        this.clock = clock;
    }

    public void onChanged(T stack, String name) {
        entries.put(idFactory.apply(stack), new Entry(clock.get(), name));
    }

    public Optional<Entry> getEntry(T stack) {
        return Optional.ofNullable(entries.get(idFactory.apply(stack)));
    }

    public record Entry(long time, String name) {
    }
}