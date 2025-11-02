package mod.chiselsandbits.utils;

import com.communi.suggestu.saecularia.caudices.core.block.IBlockWithWorldlyProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"deprecation", "NullableProblems"})
public class ReflectionHelperBlock extends Block implements IBlockWithWorldlyProperties
{
    private final ThreadLocal<String> lastInvokedThreadLocalMethodName = ThreadLocal.withInitial(() -> "unknown");

    private void markMethod()
    {
        setLastInvokedThreadLocalMethodName(
                StackWalker.getInstance().walk(stream -> stream.filter(frame -> !frame.toString().contains("idea.debugger")).skip(1).findFirst().map(StackWalker.StackFrame::getMethodName).orElse("unknown"))
        );
    }

    public ReflectionHelperBlock()
    {
        super( Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "reflection_helper_block"))) );
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState $$0)
    {
        markMethod();
        return Shapes.empty();
    }

    @Nullable
    @Override
    public VoxelShape getBlockSupportShape(@Nullable final BlockState state, @Nullable final BlockGetter reader, @Nullable final BlockPos pos)
    {
        markMethod();
        return Shapes.empty();
    }

    @Nullable
    @Override
    public VoxelShape getShape(@Nullable final BlockState state, @Nullable final BlockGetter worldIn, @Nullable final BlockPos pos, @Nullable final CollisionContext context)
    {
        markMethod();
        return Shapes.empty();
    }

    @Nullable
    @Override
    public VoxelShape getCollisionShape(@Nullable final BlockState state, @Nullable final BlockGetter worldIn, @Nullable final BlockPos pos, @Nullable final CollisionContext context)
    {
        markMethod();
        return Shapes.empty();
    }

    @Override
    public float getDestroyProgress(@Nullable final BlockState state, @Nullable final Player player, @Nullable final BlockGetter worldIn, @Nullable final BlockPos pos)
    {
        markMethod();
        return 0;
    }

    @Override
    public float getExplosionResistance()
    {
        markMethod();
        return 0;
    }

    @Override
    public float getFriction(BlockState state, LevelReader levelReader, BlockPos pos, @Nullable Entity entity) {
        markMethod();
        return 0f;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player) {
        markMethod();
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader blockGetter, BlockPos pos, Player player) {
        markMethod();
        return ItemStack.EMPTY;
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor levelAccessor, BlockPos pos, Rotation rotation) {
        markMethod();
        return state;
    }

    @Override
    public BlockState mirror(BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos, Mirror mirror) {
        markMethod();
        return blockState;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState blockState, SignalGetter signalGetter, BlockPos blockPos, Direction direction) {
        markMethod();
        return false;
    }

    @Override
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter blockAndTintGetter, BlockPos pos, FluidState fluidState) {
        markMethod();
        return false;
    }

    @Override
    public Integer getBeaconColorMultiplier(BlockState state, LevelReader levelReader, BlockPos pos, BlockPos beaconPos) {
        markMethod();
        return 0;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader levelReader, BlockPos pos, @Nullable Entity entity) {
        markMethod();
        return SoundType.AMETHYST;
    }

    @Override
    public float getExplosionResistance(final BlockState state, final BlockGetter world, final BlockPos pos, final Explosion explosion)
    {
        markMethod();
        return 0f;
    }


    @Override
    public List<ItemStack> getDrops(BlockState p_287732_, LootParams.Builder p_287596_) {
        markMethod();
        return Lists.newArrayList();
    }

    public String getLastInvokedThreadLocalMethodName() {
        return lastInvokedThreadLocalMethodName.get();
    }

    public void setLastInvokedThreadLocalMethodName(String lastInvokedThreadLocalMethodName) {
        this.lastInvokedThreadLocalMethodName.set(lastInvokedThreadLocalMethodName);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState $$0) {
        markMethod();
        return false;
    }
}
