package mod.chiselsandbits.api.axissize;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.util.BlockStatePredicates;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum CollisionType
{
	NONE_AIR(BlockStatePredicates.NOT_AIR, Blocks.STONE.defaultBlockState(), false),
			COLLIDEABLE_ONLY(BlockStatePredicates.COLLIDEABLE_ONLY, Blocks.STONE.defaultBlockState(), true),
			ALL(BlockStatePredicates.ALL, Blocks.AIR.defaultBlockState(), false);

	public static final Codec<CollisionType> CODEC = Codec.STRING.xmap(CollisionType::valueOf, CollisionType::name);
	public static final StreamCodec<ByteBuf, CollisionType> STREAM_CODEC = ByteBufCodecs.INT.map(i -> values()[i], Enum::ordinal);
	private final Predicate<BlockState> isValidFor;
	private final BlockState exampleState;
	private final boolean canBeEmptyWithJustFluids;

	CollisionType(final Predicate<BlockState> isValidFor, final BlockState exampleState, boolean canBeEmptyWithJustFluids) {this.isValidFor = isValidFor;
		this.exampleState = exampleState;
		this.canBeEmptyWithJustFluids = canBeEmptyWithJustFluids;
	}

	public boolean isValidFor(final IStateEntryInfo info) {return isValidFor.test(info.getBlockInformation().blockState()); }

	public boolean isValidFor(final BlockState blockState) {return isValidFor.test(blockState); }

	public boolean canBeEmptyWithJustFluids() {
		return canBeEmptyWithJustFluids;
	}

	public BlockState getExampleState() {
		return exampleState;
	}
}
