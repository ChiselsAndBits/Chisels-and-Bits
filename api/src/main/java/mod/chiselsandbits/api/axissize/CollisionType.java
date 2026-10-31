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

/**
 * Defines which block states count as occupied for collision and empty-space checks.
 */
public enum CollisionType {
	/**
	 * Only non-air block states count as occupying; a column of fluids still occupies this type.
	 */
	NONE_AIR(BlockStatePredicates.NOT_AIR, Blocks.STONE.defaultBlockState(), false),
	/**
	 * Only fully collidable block states are considered occupied; fluids do not occupy, so the region may be empty when filled only with fluids.
	 */
	COLLIDEABLE_ONLY(BlockStatePredicates.COLLIDEABLE_ONLY, Blocks.STONE.defaultBlockState(), true),
	/**
	 * Every block state counts as occupying, including air; the region is never considered empty even if it contains only fluids.
	 */
	ALL(BlockStatePredicates.ALL, Blocks.AIR.defaultBlockState(), false);

	/**
	 * Encodes this type to its {@link #name()} string and decodes back via {@code valueOf}.
	 */
	public static final Codec<CollisionType> CODEC = Codec.STRING.xmap(CollisionType::valueOf, CollisionType::name);
	/**
	 * Encodes this type by its ordinal index and restores it from that index.
	 */
	public static final StreamCodec<ByteBuf, CollisionType> STREAM_CODEC = ByteBufCodecs.INT.map(i -> values()[i], Enum::ordinal);
	private final Predicate<BlockState> isValidFor;
	private final BlockState exampleState;
	private final boolean canBeEmptyWithJustFluids;

	CollisionType(final Predicate<BlockState> isValidFor, final BlockState exampleState, boolean canBeEmptyWithJustFluids) {
		this.isValidFor = isValidFor;
		this.exampleState = exampleState;
		this.canBeEmptyWithJustFluids = canBeEmptyWithJustFluids;
	}

	/**
	 * Returns whether the entry's block state counts as occupying for this collision type.
	 *
	 * @param info entry whose block information is tested
	 * @return {@code true} when its state is valid for this collision type
	 */
	public boolean isValidFor(final IStateEntryInfo info) {
		return isValidFor.test(info.getBlockInformation().blockState());
	}

	/**
	 * Returns whether the given block state counts as occupying for this collision type.
	 *
	 * @param blockState state to test
	 * @return {@code true} when the state is valid for this collision type
	 */
	public boolean isValidFor(final BlockState blockState) {
		return isValidFor.test(blockState);
	}

	/**
	 * Returns whether a region filled only with fluids is considered empty for this collision type.
	 *
	 * @return {@code true} when fluid-only contents do not count as occupied
	 */
	public boolean canBeEmptyWithJustFluids() {
		return canBeEmptyWithJustFluids;
	}

	/**
	 * Returns a representative block state for this collision type.
	 *
	 * @return example state used to represent occupancy
	 */
	public BlockState getExampleState() {
		return exampleState;
	}
}
