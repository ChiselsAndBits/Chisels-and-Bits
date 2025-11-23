package mod.chiselsandbits.block;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.ModificationTableContainer;
import mod.chiselsandbits.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

public class ModificationTableBlock extends Block
{
    private static final Component      CONTAINER_NAME = Component.translatable("block." + Constants.MOD_ID + ".modification_table");
    private static final Property<Direction> FACING         = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape DEFAULT_SHAPE = Stream.of(
            Block.box(0, 11, 0, 16, 15, 16),
            Block.box(12, 0, 1, 15, 11, 4),
            Block.box(4, 8, 1, 12, 11, 15),
            Block.box(6, 9, 0, 10, 10, 1),
            Block.box(5.5, 2, 0.5, 7.5, 4, 8.5),
            Block.box(1, 0, 1, 4, 11, 4),
            Block.box(12, 0, 12, 15, 11, 15),
            Block.box(1, 0, 12, 4, 11, 15),
            Block.box(4, 0, 0, 6, 2, 8),
            Block.box(7, 0, 1, 9, 2, 9)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final Map<Direction, VoxelShape> SHAPES = Maps.newHashMap();
    static {
        SHAPES.put(Direction.NORTH, VoxelShapeUtils.rotateHorizontal(DEFAULT_SHAPE, Direction.SOUTH));
        SHAPES.put(Direction.SOUTH, VoxelShapeUtils.rotateHorizontal(DEFAULT_SHAPE, Direction.NORTH));
        SHAPES.put(Direction.WEST, VoxelShapeUtils.rotateHorizontal(DEFAULT_SHAPE, Direction.EAST));
        SHAPES.put(Direction.EAST, VoxelShapeUtils.rotateHorizontal(DEFAULT_SHAPE, Direction.WEST));
    }


    public ModificationTableBlock(final Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult result) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        } else {
            player.openMenu(state.getMenuProvider(level, pos));
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new ModificationTableContainer(id, inventory, ContainerLevelAccess.create(worldIn, pos)), CONTAINER_NAME);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING).getOpposite());
    }
}
