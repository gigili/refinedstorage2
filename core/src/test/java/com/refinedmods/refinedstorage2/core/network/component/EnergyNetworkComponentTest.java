package com.refinedmods.refinedstorage2.core.network.component;

import com.refinedmods.refinedstorage2.core.Rs2Test;
import com.refinedmods.refinedstorage2.core.adapter.FakeRs2World;
import com.refinedmods.refinedstorage2.core.network.host.FakeNetworkNodeHost;
import com.refinedmods.refinedstorage2.core.network.node.controller.ControllerNetworkNode;
import com.refinedmods.refinedstorage2.core.network.node.controller.ControllerType;
import com.refinedmods.refinedstorage2.core.util.Position;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Rs2Test
class EnergyNetworkComponentTest {
    @Test
    void Test_initial_state() {
        // Arrange
        EnergyNetworkComponent sut = new EnergyNetworkComponent();

        // Assert
        assertThat(sut.getEnergyStorage().getStored()).isZero();
        assertThat(sut.getEnergyStorage().getCapacity()).isZero();
    }

    @Test
    void Test_adding_node_should_update_energy_storage() {
        // Arrange
        EnergyNetworkComponent sut = new EnergyNetworkComponent();

        long capacityBefore = sut.getEnergyStorage().getCapacity();
        long storedBefore = sut.getEnergyStorage().getStored();

        // Act
        sut.onHostAdded(new FakeNetworkNodeHost<>(new ControllerNetworkNode(
                new FakeRs2World(),
                Position.ORIGIN,
                100,
                1000,
                ControllerType.NORMAL
        )));

        long capacityAfter = sut.getEnergyStorage().getCapacity();
        long storedAfter = sut.getEnergyStorage().getStored();

        // Assert
        assertThat(capacityBefore).isZero();
        assertThat(storedBefore).isZero();

        assertThat(capacityAfter).isEqualTo(1000);
        assertThat(storedAfter).isEqualTo(100);
    }

    @Test
    void Test_removing_node_should_update_energy_storage() {
        // Arrange
        EnergyNetworkComponent sut = new EnergyNetworkComponent();

        FakeNetworkNodeHost<ControllerNetworkNode> host = new FakeNetworkNodeHost<>(new ControllerNetworkNode(
                new FakeRs2World(),
                Position.ORIGIN,
                100,
                1000,
                ControllerType.NORMAL
        ));

        sut.onHostAdded(host);

        long capacityBefore = sut.getEnergyStorage().getCapacity();
        long storedBefore = sut.getEnergyStorage().getStored();

        // Act
        sut.onHostRemoved(host);

        long capacityAfter = sut.getEnergyStorage().getCapacity();
        long storedAfter = sut.getEnergyStorage().getStored();

        // Assert
        assertThat(capacityBefore).isEqualTo(1000);
        assertThat(storedBefore).isEqualTo(100);

        assertThat(capacityAfter).isZero();
        assertThat(storedAfter).isZero();
    }
}
