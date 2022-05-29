package com.refinedmods.refinedstorage2.api.storage;

import com.refinedmods.refinedstorage2.api.core.Action;
import com.refinedmods.refinedstorage2.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage2.api.storage.limited.LimitedStorageImpl;
import com.refinedmods.refinedstorage2.test.Rs2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Rs2Test
class LimitedStorageImplTest {
    private SourceCapturingStorage<String> backed;
    private LimitedStorageImpl<String> sut;
    private final Source customSource = () -> "Custom";

    @BeforeEach
    void setUp() {
        backed = new SourceCapturingStorage<>(new InMemoryStorageImpl<>());
        sut = new LimitedStorageImpl<>(backed, 100);
    }

    @Test
    void Test_negative_capacity() {
        // Arrange
        Storage<String> backed = new InMemoryStorageImpl<>();

        // Act & assert
        assertThrows(IllegalArgumentException.class, () -> new LimitedStorageImpl<>(backed, -1));
    }

    @Test
    void Test_zero_capacity() {
        // Arrange
        SourceCapturingStorage<String> backed = new SourceCapturingStorage<>(new InMemoryStorageImpl<>());
        Storage<String> sut = new LimitedStorageImpl<>(backed, 0);

        // Act
        long inserted = sut.insert("A", 1, Action.EXECUTE, EmptySource.INSTANCE);

        // Assert
        assertThat(inserted).isZero();
        assertThat(backed.getSourcesUsed()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void Test_adding_a_resource(Action action) {
        // Act
        long inserted = sut.insert("A", 100, action, customSource);

        // Assert
        assertThat(inserted).isEqualTo(100);

        if (action == Action.EXECUTE) {
            assertThat(sut.getAll()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
                    new ResourceAmount<>("A", 100)
            );
            assertThat(sut.getStored()).isEqualTo(100);
        } else {
            assertThat(sut.getAll()).isEmpty();
            assertThat(sut.getStored()).isZero();
        }

        assertThat(backed.getSourcesUsed()).containsExactly(customSource);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void Test_adding_a_resource_and_exceeding_capacity(Action action) {
        // Act
        long inserted1 = sut.insert("A", 60, Action.EXECUTE, customSource);
        long inserted2 = sut.insert("B", 45, action, customSource);

        // Assert
        assertThat(inserted1).isEqualTo(60);
        assertThat(inserted2).isEqualTo(40);

        if (action == Action.EXECUTE) {
            assertThat(sut.getAll()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
                    new ResourceAmount<>("A", 60),
                    new ResourceAmount<>("B", 40)
            );
            assertThat(sut.getStored()).isEqualTo(100);
        } else {
            assertThat(sut.getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(
                    new ResourceAmount<>("A", 60)
            );
            assertThat(sut.getStored()).isEqualTo(60);
        }

        assertThat(backed.getSourcesUsed()).containsExactly(customSource, customSource);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void Test_adding_resource_to_an_already_full_storage_and_exceeding_capacity(Action action) {
        // Act
        long inserted1 = sut.insert("A", 100, Action.EXECUTE, customSource);
        long inserted2 = sut.insert("A", 101, action, customSource);

        // Assert
        assertThat(inserted1).isEqualTo(100);
        assertThat(inserted2).isZero();

        assertThat(sut.getStored()).isEqualTo(100);

        assertThat(backed.getSourcesUsed()).containsExactly(customSource);
    }

    @Test
    void Test_adding_invalid_resource() {
        // Act
        Executable action1 = () -> sut.insert("A", 0, Action.EXECUTE, EmptySource.INSTANCE);
        Executable action2 = () -> sut.insert("A", -1, Action.EXECUTE, EmptySource.INSTANCE);
        Executable action3 = () -> sut.insert(null, 1, Action.EXECUTE, EmptySource.INSTANCE);

        // Assert
        assertThrows(IllegalArgumentException.class, action1);
        assertThrows(IllegalArgumentException.class, action2);
        assertThrows(NullPointerException.class, action3);
    }

    @Test
    void Test_extracting_resource() {
        // Arrange
        sut.insert("A", 100, Action.EXECUTE, customSource);

        // Act
        long extracted = sut.extract("A", 101, Action.EXECUTE, customSource);

        // Assert
        assertThat(extracted).isEqualTo(100);
        assertThat(backed.getSourcesUsed()).containsExactly(customSource, customSource);
    }
}