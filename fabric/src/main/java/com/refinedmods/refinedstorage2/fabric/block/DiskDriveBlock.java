package com.refinedmods.refinedstorage2.fabric.block;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProvider;
import com.refinedmods.refinedstorage2.fabric.block.entity.diskdrive.DiskDriveBlockEntity;
import com.refinedmods.refinedstorage2.fabric.util.BiDirection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DiskDriveBlock extends NetworkNodeBlock implements AttributeProvider {
    public static final EnumProperty<BiDirection> DIRECTION = EnumProperty.of("direction", BiDirection.class);

    public DiskDriveBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(DIRECTION, BiDirection.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);

        builder.add(DIRECTION);
    }

    private BiDirection getDirection(Direction playerFacing, float playerPitch) {
        if (playerPitch > 65) {
            return BiDirection.forUp(playerFacing);
        } else if (playerPitch < -65) {
            return BiDirection.forDown(playerFacing.getOpposite());
        } else {
            return BiDirection.forHorizontal(playerFacing.getOpposite());
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(DIRECTION, getDirection(ctx.getPlayerFacing(), ctx.getPlayer() != null ? ctx.getPlayer().pitch : 0));
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockView world) {
        return new DiskDriveBlockEntity();
    }

    @Override
    public void addAllAttributes(World world, BlockPos blockPos, BlockState blockState, AttributeList<?> attributeList) {
        attributeList.offer(((DiskDriveBlockEntity) world.getBlockEntity(blockPos)).getDiskInventory());
    }
}