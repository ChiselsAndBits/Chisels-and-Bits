package mod.chiselsandbits.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

public class PatternScannerBlock extends HorizontalDirectionalBlock
{
    private static final MapCodec<PatternScannerBlock> CODEC = simpleCodec(PatternScannerBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
      .put(Direction.NORTH, Stream.of(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(14, 5, 0, 16, 7, 2),
        Block.box(15, 8, 0, 16, 9, 1),
        Block.box(15, 8, 15, 16, 9, 16),
        Block.box(0, 8, 15, 1, 9, 16),
        Block.box(0, 8, 0, 1, 9, 1),
        Block.box(14, 7, 0, 16, 8, 16),
        Block.box(0, 7, 0, 2, 8, 16),
        Block.box(0, 9, 0, 2, 10, 16),
        Block.box(14, 9, 0, 16, 10, 16),
        Block.box(14, 5, 14, 16, 7, 16),
        Block.box(0, 5, 14, 2, 7, 16),
        Block.box(0, 5, 0, 2, 7, 2),
        Block.box(3, 5, 3, 13, 6, 13),
        Block.box(0, 8, 11, 2, 9, 12),
        Block.box(14, 8, 11, 16, 9, 12)
      ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get())
      .put(Direction.EAST, Stream.of(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(14, 5, 14, 16, 7, 16),
        Block.box(15, 8, 15, 16, 9, 16),
        Block.box(0, 8, 15, 1, 9, 16),
        Block.box(0, 8, 0, 1, 9, 1),
        Block.box(15, 8, 0, 16, 9, 1),
        Block.box(0, 7, 14, 16, 8, 16),
        Block.box(0, 7, 0, 16, 8, 2),
        Block.box(0, 9, 0, 16, 10, 2),
        Block.box(0, 9, 14, 16, 10, 16),
        Block.box(0, 5, 14, 2, 7, 16),
        Block.box(0, 5, 0, 2, 7, 2),
        Block.box(14, 5, 0, 16, 7, 2),
        Block.box(3, 5, 3, 13, 6, 13),
        Block.box(4, 8, 0, 5, 9, 2),
        Block.box(4, 8, 14, 5, 9, 16)
      ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get())
      .put(Direction.SOUTH, Stream.of(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(0, 5, 14, 2, 7, 16),
        Block.box(0, 8, 15, 1, 9, 16),
        Block.box(0, 8, 0, 1, 9, 1),
        Block.box(15, 8, 0, 16, 9, 1),
        Block.box(15, 8, 15, 16, 9, 16),
        Block.box(0, 7, 0, 2, 8, 16),
        Block.box(14, 7, 0, 16, 8, 16),
        Block.box(14, 9, 0, 16, 10, 16),
        Block.box(0, 9, 0, 2, 10, 16),
        Block.box(0, 5, 0, 2, 7, 2),
        Block.box(14, 5, 0, 16, 7, 2),
        Block.box(14, 5, 14, 16, 7, 16),
        Block.box(3, 5, 3, 13, 6, 13),
        Block.box(14, 8, 4, 16, 9, 5),
        Block.box(0, 8, 4, 2, 9, 5)
      ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get())
      .put(Direction.WEST, Stream.of(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(0, 5, 0, 2, 7, 2),
        Block.box(0, 8, 0, 1, 9, 1),
        Block.box(15, 8, 0, 16, 9, 1),
        Block.box(15, 8, 15, 16, 9, 16),
        Block.box(0, 8, 15, 1, 9, 16),
        Block.box(0, 7, 0, 16, 8, 2),
        Block.box(0, 7, 14, 16, 8, 16),
        Block.box(0, 9, 14, 16, 10, 16),
        Block.box(0, 9, 0, 16, 10, 2),
        Block.box(14, 5, 0, 16, 7, 2),
        Block.box(14, 5, 14, 16, 7, 16),
        Block.box(0, 5, 14, 2, 7, 16),
        Block.box(3, 5, 3, 13, 6, 13),
        Block.box(11, 8, 14, 12, 9, 16),
        Block.box(11, 8, 0, 12, 9, 2)
      ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get())
      .build();

    public PatternScannerBlock(final Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    public @NotNull InteractionResult use(
      @NotNull BlockState state,
      Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull InteractionHand hand,
      @NotNull BlockHitResult hitResult)
    {
        if (true)
            return InteractionResult.PASS;

        if (level.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }
        else
        {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
            return InteractionResult.CONSUME;
        }
    }

    public MenuProvider getMenuProvider(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos post)
    {
        return new SimpleMenuProvider((screenIndex, inventory, player) -> new CraftingMenu(screenIndex, inventory, ContainerLevelAccess.create(level, post)), Component.literal(""));
    }

    @Override
    public @NotNull VoxelShape getShape(
      final @NotNull BlockState state, final @NotNull BlockGetter blockGetter, final @NotNull BlockPos pos, final @NotNull CollisionContext context)
    {
        return SHAPES.get(state.getValue(FACING));
    }
}
