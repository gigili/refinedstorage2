package com.refinedmods.refinedstorage2.platform.forge.packet.c2s;

import com.refinedmods.refinedstorage2.platform.api.resource.filter.FilteredResource;
import com.refinedmods.refinedstorage2.platform.common.containermenu.AbstractResourceFilterContainerMenu;

import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class ResourceFilterSlotChangePacket {
    private final int slotIndex;
    @Nullable
    private final FilteredResource filteredResource;
    @Nullable
    private final FriendlyByteBuf buf;

    public ResourceFilterSlotChangePacket(final int slotIndex, final FilteredResource filteredResource) {
        this.slotIndex = slotIndex;
        this.filteredResource = filteredResource;
        this.buf = null;
    }

    public ResourceFilterSlotChangePacket(final int slotIndex, final FriendlyByteBuf buf) {
        this.slotIndex = slotIndex;
        this.filteredResource = null;
        this.buf = buf;
    }

    public static ResourceFilterSlotChangePacket decode(final FriendlyByteBuf buf) {
        final int slotIndex = buf.readInt();
        return new ResourceFilterSlotChangePacket(slotIndex, buf);
    }

    public static void encode(final ResourceFilterSlotChangePacket packet, final FriendlyByteBuf buf) {
        buf.writeInt(packet.slotIndex);
        Objects.requireNonNull(packet.filteredResource).writeToPacket(buf);
    }

    public static void handle(final ResourceFilterSlotChangePacket packet, final Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            handle(packet, player);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void handle(final ResourceFilterSlotChangePacket packet, final ServerPlayer player) {
        if (player.containerMenu instanceof AbstractResourceFilterContainerMenu menu) {
            menu.setFilteredResource(packet.slotIndex, Objects.requireNonNull(packet.buf));
        }
    }
}
