package com.refinedmods.refinedstorage2.fabric.screenhandler.grid;

import java.util.Optional;

import com.refinedmods.refinedstorage2.core.grid.GridEventHandler;
import com.refinedmods.refinedstorage2.core.grid.GridEventHandlerImpl;
import com.refinedmods.refinedstorage2.core.grid.GridExtractMode;
import com.refinedmods.refinedstorage2.core.grid.GridInsertMode;
import com.refinedmods.refinedstorage2.core.grid.GridScrollMode;
import com.refinedmods.refinedstorage2.core.grid.GridSearchBoxMode;
import com.refinedmods.refinedstorage2.core.grid.GridSize;
import com.refinedmods.refinedstorage2.core.grid.GridSortingDirection;
import com.refinedmods.refinedstorage2.core.grid.GridSortingType;
import com.refinedmods.refinedstorage2.core.grid.GridView;
import com.refinedmods.refinedstorage2.core.grid.GridViewImpl;
import com.refinedmods.refinedstorage2.core.list.StackListListener;
import com.refinedmods.refinedstorage2.core.list.StackListResult;
import com.refinedmods.refinedstorage2.core.list.item.ItemStackList;
import com.refinedmods.refinedstorage2.core.network.node.RedstoneMode;
import com.refinedmods.refinedstorage2.core.storage.StorageChannel;
import com.refinedmods.refinedstorage2.core.storage.StorageTracker;
import com.refinedmods.refinedstorage2.core.util.ItemStackIdentifier;
import com.refinedmods.refinedstorage2.fabric.RefinedStorage2Config;
import com.refinedmods.refinedstorage2.fabric.RefinedStorage2Mod;
import com.refinedmods.refinedstorage2.fabric.block.entity.RedstoneModeSettings;
import com.refinedmods.refinedstorage2.fabric.block.entity.grid.GridBlockEntity;
import com.refinedmods.refinedstorage2.fabric.block.entity.grid.GridSettings;
import com.refinedmods.refinedstorage2.fabric.coreimpl.grid.PlayerGridInteractor;
import com.refinedmods.refinedstorage2.fabric.coreimpl.grid.query.FabricGridStackFactory;
import com.refinedmods.refinedstorage2.fabric.packet.s2c.GridActivePacket;
import com.refinedmods.refinedstorage2.fabric.packet.s2c.GridItemUpdatePacket;
import com.refinedmods.refinedstorage2.fabric.screen.grid.GridSearchBox;
import com.refinedmods.refinedstorage2.fabric.screenhandler.BaseScreenHandler;
import com.refinedmods.refinedstorage2.fabric.screenhandler.RedstoneModeAccessor;
import com.refinedmods.refinedstorage2.fabric.screenhandler.property.TwoWaySyncProperty;
import com.refinedmods.refinedstorage2.fabric.util.PacketUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GridScreenHandler extends BaseScreenHandler implements GridEventHandler, StackListListener<ItemStack>, RedstoneModeAccessor {
    private static final Logger LOGGER = LogManager.getLogger(GridScreenHandler.class);

    private static String lastSearchQuery = "";

    private final PlayerInventory playerInventory;
    private final GridView<ItemStack> itemView = new GridViewImpl<>(new FabricGridStackFactory(), ItemStackIdentifier::new, ItemStackList.create());

    private GridBlockEntity grid;

    private StorageChannel<ItemStack> storageChannel; // TODO - Support changing of the channel.
    private GridEventHandler eventHandler;
    private boolean active;

    private final TwoWaySyncProperty<RedstoneMode> redstoneModeProperty;
    private final TwoWaySyncProperty<GridSortingDirection> sortingDirectionProperty;
    private final TwoWaySyncProperty<GridSortingType> sortingTypeProperty;
    private final TwoWaySyncProperty<GridSize> sizeProperty;
    private final TwoWaySyncProperty<GridSearchBoxMode> searchBoxModeProperty;

    private Runnable sizeChangedListener;
    private GridSearchBox searchBox;

    private GridSize size;
    private GridSearchBoxMode searchBoxMode;

    public GridScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(RefinedStorage2Mod.SCREEN_HANDLERS.getGrid(), syncId);

        this.playerInventory = playerInventory;

        addProperty(redstoneModeProperty = TwoWaySyncProperty.forClient(
            0,
            RedstoneModeSettings::getRedstoneMode,
            RedstoneModeSettings::getRedstoneMode,
            RedstoneMode.IGNORE,
            (redstoneMode) -> {
            }
        ));

        addProperty(sortingDirectionProperty = TwoWaySyncProperty.forClient(
            1,
            GridSettings::getSortingDirection,
            GridSettings::getSortingDirection,
            GridSortingDirection.ASCENDING,
            this::onSortingDirectionChanged
        ));

        addProperty(sortingTypeProperty = TwoWaySyncProperty.forClient(
            2,
            GridSettings::getSortingType,
            GridSettings::getSortingType,
            GridSortingType.QUANTITY,
            this::onSortingTypeChanged
        ));

        addProperty(sizeProperty = TwoWaySyncProperty.forClient(
            3,
            GridSettings::getSize,
            GridSettings::getSize,
            GridSize.STRETCH,
            this::onSizeChanged
        ));

        addProperty(searchBoxModeProperty = TwoWaySyncProperty.forClient(
            4,
            searchBoxMode -> RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().getId(searchBoxMode),
            searchBoxMode -> RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().get(searchBoxMode),
            RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().getDefault(),
            this::onSearchBoxModeChanged
        ));

        active = buf.readBoolean();

        itemView.setSortingDirection(GridSettings.getSortingDirection(buf.readInt()));
        itemView.setSortingType(GridSettings.getSortingType(buf.readInt()));
        size = GridSettings.getSize(buf.readInt());
        searchBoxMode = RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().get(buf.readInt());

        addSlots(0);

        int size = buf.readInt();
        for (int i = 0; i < size; ++i) {
            ItemStack stack = buf.readItemStack();
            stack.setCount(buf.readInt());
            StorageTracker.Entry trackerEntry = PacketUtil.readTrackerEntry(buf);
            itemView.loadStack(stack, stack.getCount(), trackerEntry);
        }
        itemView.sort();
    }

    public GridScreenHandler(int syncId, PlayerInventory playerInventory, GridBlockEntity grid) {
        super(RefinedStorage2Mod.SCREEN_HANDLERS.getGrid(), syncId);

        addProperty(redstoneModeProperty = TwoWaySyncProperty.forServer(
            0,
            RedstoneModeSettings::getRedstoneMode,
            RedstoneModeSettings::getRedstoneMode,
            grid::getRedstoneMode,
            grid::setRedstoneMode
        ));

        addProperty(sortingDirectionProperty = TwoWaySyncProperty.forServer(
            1,
            GridSettings::getSortingDirection,
            GridSettings::getSortingDirection,
            grid::getSortingDirection,
            grid::setSortingDirection
        ));

        addProperty(sortingTypeProperty = TwoWaySyncProperty.forServer(
            2,
            GridSettings::getSortingType,
            GridSettings::getSortingType,
            grid::getSortingType,
            grid::setSortingType
        ));

        addProperty(sizeProperty = TwoWaySyncProperty.forServer(
            3,
            GridSettings::getSize,
            GridSettings::getSize,
            grid::getSize,
            grid::setSize
        ));

        addProperty(searchBoxModeProperty = TwoWaySyncProperty.forServer(
            4,
            searchBoxMode -> RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().getId(searchBoxMode),
            searchBoxMode -> RefinedStorage2Mod.API.getGridSearchBoxModeRegistry().get(searchBoxMode),
            grid::getSearchBoxMode,
            grid::setSearchBoxMode
        ));

        this.playerInventory = playerInventory;
        this.storageChannel = grid.getNetwork().getItemStorageChannel();
        this.storageChannel.addListener(this);
        this.eventHandler = new GridEventHandlerImpl(grid.isActive(), storageChannel, new PlayerGridInteractor(playerInventory.player)) {
            @Override
            public void onActiveChanged(boolean active) {
                super.onActiveChanged(active);
                PacketUtil.sendToPlayer((ServerPlayerEntity) playerInventory.player, GridActivePacket.ID, buf -> buf.writeBoolean(active));
            }
        };
        this.grid = grid;
        this.grid.addWatcher(eventHandler);

        addSlots(0);
    }

    public void setSizeChangedListener(Runnable sizeChangedListener) {
        this.sizeChangedListener = sizeChangedListener;
    }

    public void setSortingDirection(GridSortingDirection sortingDirection) {
        sortingDirectionProperty.syncToServer(sortingDirection);
    }

    public GridSortingDirection getSortingDirection() {
        return sortingDirectionProperty.getDeserialized();
    }

    public void setSortingType(GridSortingType sortingType) {
        sortingTypeProperty.syncToServer(sortingType);
    }

    public GridSortingType getSortingType() {
        return sortingTypeProperty.getDeserialized();
    }

    public GridSize getSize() {
        return size;
    }

    public void setSize(GridSize size) {
        sizeProperty.syncToServer(size);
    }

    public GridSearchBoxMode getSearchBoxMode() {
        return searchBoxModeProperty.getDeserialized();
    }

    public void setSearchBoxMode(GridSearchBoxMode searchBoxMode) {
        searchBoxModeProperty.syncToServer(searchBoxMode);
    }

    private void onSortingTypeChanged(GridSortingType sortingType) {
        if (itemView.getSortingType() != sortingType) {
            itemView.setSortingType(sortingType);
            itemView.sort();
        }
    }

    private void onSortingDirectionChanged(GridSortingDirection sortingDirection) {
        if (itemView.getSortingDirection() != sortingDirection) {
            itemView.setSortingDirection(sortingDirection);
            itemView.sort();
        }
    }

    private void onSizeChanged(GridSize size) {
        if (this.size != size) {
            this.size = size;
            if (sizeChangedListener != null) {
                sizeChangedListener.run();
            }
        }
    }

    private void onSearchBoxModeChanged(GridSearchBoxMode searchBoxMode) {
        if (this.searchBoxMode != searchBoxMode) {
            this.searchBoxMode = searchBoxMode;
            this.updateSearchBox();
        }
    }

    public void setSearchBox(GridSearchBox searchBox) {
        this.searchBox = searchBox;
        this.updateSearchBox();
        if (RefinedStorage2Config.get().getGrid().isRememberSearchQuery()) {
            this.searchBox.setText(lastSearchQuery);
        }
    }

    private void updateSearchBox() {
        this.searchBox.setAutoSelected(searchBoxMode.shouldAutoSelect());
        this.searchBox.setListener(text -> {
            if (RefinedStorage2Config.get().getGrid().isRememberSearchQuery()) {
                lastSearchQuery = text;
            }
            searchBoxMode.onTextChanged(itemView, text);
        });
    }

    @Override
    public void close(PlayerEntity playerEntity) {
        super.close(playerEntity);

        if (storageChannel != null) {
            storageChannel.removeListener(this);
        }

        if (grid != null) {
            grid.removeWatcher(eventHandler);
        }
    }

    public void addSlots(int playerInventoryY) {
        slots.clear();

        addPlayerInventory(playerInventory, 8, playerInventoryY);
    }

    @Override
    public void onInsertFromCursor(GridInsertMode mode) {
        eventHandler.onInsertFromCursor(mode);
    }

    @Override
    public void onInsertFromTransfer(Slot slot) {
        eventHandler.onInsertFromTransfer(slot);
    }

    @Override
    public void onExtract(ItemStack stack, GridExtractMode mode) {
        eventHandler.onExtract(stack, mode);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity playerEntity, int slotIndex) {
        if (!playerEntity.world.isClient()) {
            Slot slot = getSlot(slotIndex);
            if (slot.hasStack()) {
                eventHandler.onInsertFromTransfer(slot);
                sendContentUpdates();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onItemUpdate(ItemStack template, int amount, StorageTracker.Entry trackerEntry) {
        LOGGER.info("Item {} got updated with {}", template, amount);

        itemView.onChange(template, amount, trackerEntry);
    }

    @Override
    public void onActiveChanged(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onScroll(ItemStack stack, int slot, GridScrollMode mode) {
        eventHandler.onScroll(stack, slot, mode);
    }

    @Override
    public void onChanged(StackListResult<ItemStack> change) {
        LOGGER.info("Received a change of {} for {}", change.getChange(), change.getStack());

        PacketUtil.sendToPlayer((ServerPlayerEntity) playerInventory.player, GridItemUpdatePacket.ID, buf -> {
            PacketUtil.writeItemStackWithoutCount(buf, change.getStack());
            buf.writeInt(change.getChange());

            Optional<StorageTracker.Entry> entry = storageChannel.getTracker().getEntry(change.getStack());
            PacketUtil.writeTrackerEntry(buf, entry);
        });
    }

    public GridView<ItemStack> getItemView() {
        return itemView;
    }

    @Override
    public RedstoneMode getRedstoneMode() {
        return redstoneModeProperty.getDeserialized();
    }

    @Override
    public void setRedstoneMode(RedstoneMode redstoneMode) {
        redstoneModeProperty.syncToServer(redstoneMode);
    }
}