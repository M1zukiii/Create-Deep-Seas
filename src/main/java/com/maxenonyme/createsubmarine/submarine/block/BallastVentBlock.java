package com.maxenonyme.createsubmarine.submarine.block;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import com.maxenonyme.createsubmarine.submarine.block.entity.BallastVentBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
public class BallastVentBlock extends HorizontalKineticBlock implements EntityBlock {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public BallastVentBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(UP, false).setValue(DOWN, false)
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BallastVentBlockEntity(pos, state);
    }
    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CreateSubmarine.BALLAST_VENT_BE.get() ? (l, p, s, be) -> ((BallastVentBlockEntity) be).tick() : null;
    }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        if (base == null) base = this.defaultBlockState();
        return computeConnections(base, context.getLevel(), context.getClickedPos());
    }
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (!isPipeConnectionFace(updated, direction)) {
            return updated.setValue(propertyForDirection(direction), false);
        }
        boolean connected = canConnectTo(level, neighborPos, direction);
        return updated.setValue(propertyForDirection(direction), connected);
    }
    public static BlockState computeConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BooleanProperty prop = propertyForDirection(dir);
            if (!isPipeConnectionFace(state, dir)) {
                state = state.setValue(prop, false);
                continue;
            }
            state = state.setValue(prop, canConnectTo(level, pos.relative(dir), dir));
        }
        return state;
    }
    public static boolean isPipeConnectionFace(BlockState state, Direction face) {
        Direction shaftFace = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        return face != shaftFace;
    }
    private static boolean canConnectTo(BlockGetter level, BlockPos neighborPos, Direction faceTowardsNeighbor) {
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be == null) return false;
        if (be.getLevel() == null) return false;
        IFluidHandler handler = be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, faceTowardsNeighbor.getOpposite());
        return handler != null;
    }
    public static BooleanProperty propertyForDirection(Direction dir) {
        return switch (dir) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
        };
    }
}