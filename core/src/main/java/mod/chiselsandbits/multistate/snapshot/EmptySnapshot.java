package mod.chiselsandbits.multistate.snapshot;

import com.google.common.collect.ImmutableMap;
import mod.chiselsandbits.api.axissize.CollisionType;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.exceptions.SpaceOccupiedException;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.multistate.IStatistics;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.accessor.sortable.IPositionMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.multistate.statistics.IMultiStateObjectStatistics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptySnapshot implements IMultiStateSnapshot
{
    public static final EmptySnapshot INSTANCE = new EmptySnapshot();

    private static final IMultiStateObjectStatistics EMPTY_STATISTICS = new IMultiStateObjectStatistics() {
        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {

        }

        @Override
        public IBlockInformation getPrimaryState()
        {
            return BlockInformation.AIR;
        }

        @Override
        public boolean isEmpty()
        {
            return true;
        }

        @Override
        public Map<IBlockInformation, Integer> getStateCounts()
        {
            return ImmutableMap.<IBlockInformation, Integer>builder().build();
        }

        @Override
        public boolean shouldCheckWeakPower()
        {
            return false;
        }

        @Override
        public float getFullnessFactor()
        {
            return 0;
        }

        @Override
        public float getSlipperiness()
        {
            return 0;
        }

        @Override
        public float getLightEmissionFactor()
        {
            return 0;
        }

        @Override
        public float getLightBlockingFactor()
        {
            return 0;
        }

        @Override
        public float getRelativeBlockHardness(final Player player)
        {
            return 0;
        }

        @Override
        public boolean canPropagateSkylight()
        {
            return true;
        }

        @Override
        public boolean canSustainGrassBelow()
        {
            return false;
        }

        @Override
        public BitSet getCollideableEntries(final CollisionType collisionType) { return BitSet.valueOf(new long[0]); }
    };

    @Override
    public IAreaShapeIdentifier createNewShapeIdentifier()
    {
        return Identifier.INSTANCE;
    }

    @Override
    public Stream<IStateEntryInfo> stream()
    {
        return Stream.empty();
    }

    @Override
    public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget)
    {
        return Optional.empty();
    }

    @Override
    public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        return Optional.empty();
    }

    @Override
    public boolean isInside(final Vec3 inAreaTarget)
    {
        return false;
    }

    @Override
    public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {
        return false;
    }

    @Override
    public IMultiStateSnapshot createSnapshot()
    {
        return this;
    }

    @Override
    public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator)
    {
        return Stream.empty();
    }

    @Override
    public void forEachWithPositionMutator(
      final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer)
    {
        //Noop
    }

    @Override
    public Stream<IMutableStateEntryInfo> mutableStream()
    {
        return Stream.empty();
    }

    @Override
    public void setInAreaTarget(
      final IBlockInformation blockState,
      final Vec3 inAreaTarget) throws SpaceOccupiedException
    {

    }

    @Override
    public void setInBlockTarget(final IBlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException
    {

    }

    @Override
    public void clearInAreaTarget(final Vec3 inAreaTarget)
    {

    }

    @Override
    public void clearInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
    {

    }

    @Override
    public IMultiStateItemStack toItemStack()
    {
        return Stack.INSTANCE;
    }

    @Override
    public IMultiStateObjectStatistics getStatics()
    {
        return EMPTY_STATISTICS;
    }

    @Override
    public IMultiStateSnapshot clone()
    {
        return this;
    }

    @Override
    public void rotate(final Direction.Axis axis, final int rotationCount)
    {
        //Noop
    }

    @Override
    public void mirror(final Direction.Axis axis)
    {
        //Noop
    }

    @Override
    public @NotNull AABB getBoundingBox()
    {
        return new AABB(0,0,0,1,1,1);
    }

    private static final class Identifier implements IAreaShapeIdentifier {
        public static final Identifier INSTANCE = new Identifier();
    }

    public static final class Stack implements IMultiStateItemStack {

        public static final Stack INSTANCE = new Stack();

        @Override
        public IStatistics getStatistics()
        {
            return new IStatistics() {
                @Override
                public IBlockInformation getPrimaryState()
                {
                    return BlockInformation.AIR;
                }

                @Override
                public boolean isEmpty()
                {
                    return true;
                }

                @Override
                public CompoundTag serializeNBT()
                {
                    return new CompoundTag();
                }

                @Override
                public void deserializeNBT(final CompoundTag nbt)
                {
                }

                @Override
                public Set<IBlockInformation> getContainedStates() {
                    return Collections.emptySet();
                }
            };
        }

        @Override
        public ItemStack toBlockStack()
        {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack toPatternStack()
        {
            return ItemStack.EMPTY;
        }

        @Override
        public IMultiStateItemStack copy()
        {
            return this;
        }

        @Override
        public IAreaShapeIdentifier createNewShapeIdentifier()
        {
            return Identifier.INSTANCE;
        }

        @Override
        public Stream<IStateEntryInfo> stream()
        {
            return Stream.empty();
        }

        @Override
        public Optional<IStateEntryInfo> getInAreaTarget(final Vec3 inAreaTarget)
        {
            return Optional.empty();
        }

        @Override
        public Optional<IStateEntryInfo> getInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
        {
            return Optional.empty();
        }

        @Override
        public boolean isInside(final Vec3 inAreaTarget)
        {
            return false;
        }

        @Override
        public boolean isInside(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
        {
            return false;
        }

        @Override
        public IMultiStateSnapshot createSnapshot()
        {
            return EmptySnapshot.INSTANCE;
        }

        @Override
        public Stream<IStateEntryInfo> streamWithPositionMutator(final IPositionMutator positionMutator)
        {
            return Stream.empty();
        }

        @Override
        public void forEachWithPositionMutator(
          final IPositionMutator positionMutator, final Consumer<IStateEntryInfo> consumer)
        {
            //Noop
        }

        @Override
        public Stream<IMutableStateEntryInfo> mutableStream()
        {
            return Stream.empty();
        }

        @Override
        public void setInAreaTarget(
          final IBlockInformation blockInformation,
          final Vec3 inAreaTarget) throws SpaceOccupiedException
        {

        }

        @Override
        public void setInBlockTarget(final IBlockInformation blockInformation, final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget) throws SpaceOccupiedException
        {

        }

        @Override
        public void clearInAreaTarget(final Vec3 inAreaTarget)
        {

        }

        @Override
        public void clearInBlockTarget(final BlockPos inAreaBlockPosOffset, final Vec3 inBlockTarget)
        {

        }

        @Override
        public void serializeInto(@NotNull final FriendlyByteBuf packetBuffer)
        {

        }

        @Override
        public void deserializeFrom(@NotNull final FriendlyByteBuf packetBuffer)
        {

        }

        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {

        }

        @Override
        public void rotate(final Direction.Axis axis, final int rotationCount)
        {

        }

        @Override
        public void mirror(final Direction.Axis axis)
        {

        }

        @Override
        public @NotNull AABB getBoundingBox()
        {
            return new AABB(0,0,0,1,1,1);
        }
    }
}
