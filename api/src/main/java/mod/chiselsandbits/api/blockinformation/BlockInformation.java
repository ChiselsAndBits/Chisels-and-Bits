package mod.chiselsandbits.api.blockinformation;

import java.util.Comparator;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.BlockStateSerializationUtils;
import mod.chiselsandbits.api.util.ComparatorUtils;
import mod.chiselsandbits.api.util.ISnapshotable;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariant;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Immutable description of a micro-block held in the Chisels &amp; Bits world: a base {@link net.minecraft.world.level.block.state.BlockState}
 * paired with an optional {@link mod.chiselsandbits.api.variant.state.IStateVariant} that preserves per-instance overrides.
 *
 * <p>
 * Instances are (de)serialised through the {@code CODEC}, {@code MAP_CODEC}, and {@code STREAM_CODEC} fields, snapshotted via
 * {@link #createSnapshot()}, ordered by block-state id then variant in {@link #compareTo(BlockInformation)}, and used to build block entities at a position.
 * The {@link #AIR} constant describes empty space.
 *
 * @param blockState base Minecraft block state represented by this information
 * @param variant optional per-instance state variant
 */
public record BlockInformation(
		BlockState blockState,
		Optional<IStateVariant> variant) implements Serializable.Registry<BlockInformation>, ISnapshotable<BlockInformation>, Comparable<BlockInformation> {
	public static final Codec<BlockInformation> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockStateSerializationUtils.LEGACY_BLOCK_STATE_CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
			IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
	).apply(instance, BlockInformation::new));

	public static final Codec<BlockInformation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockState.CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
			IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
	).apply(instance, BlockInformation::new));

	public static final MapCodec<BlockInformation> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockState.CODEC.fieldOf(NbtConstants.STATE).forGetter(BlockInformation::blockState),
			IStateVariant.CODEC.optionalFieldOf(NbtConstants.VARIANT).forGetter(BlockInformation::variant)
	).apply(instance, BlockInformation::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlockInformation> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
			BlockInformation::blockState,
			ByteBufCodecs.optional(IStateVariant.STREAM_CODEC),
			BlockInformation::variant,
			BlockInformation::new
	);
	public static final BlockInformation AIR = new BlockInformation(Blocks.AIR.defaultBlockState(), Optional.empty());

	private static final Comparator<BlockState> STATE_COMPARATOR = Comparator.comparing(
			IBlockStateIdManager.getInstance()::getIdFrom
	);
	private static final Comparator<IStateVariant> VARIANT_COMPARATOR = Comparator.<IStateVariant, String>comparing(
			variant -> variant.getClass().getName()
	).thenComparing(Comparator.naturalOrder());
	private static final Comparator<Optional<IStateVariant>> OPTIONAL_VARIANT_COMPARATOR = ComparatorUtils.createOptionalComparator(
			VARIANT_COMPARATOR
	);
	private static final Comparator<BlockInformation> COMPARATOR = Comparator.comparing(BlockInformation::blockState, STATE_COMPARATOR)
			.thenComparing(BlockInformation::variant, OPTIONAL_VARIANT_COMPARATOR);

	/**
	 * Reports whether the stored block state currently holds a source (still) fluid such as water or lava.
	 *
	 * @return {@code true} if this position occupies a still/source fluid; {@code false} for solids, air, and flowing fluids.
	 */
	public boolean isFluid() {
		return blockState().getFluidState().isSource();
	}

	/**
	 * Reports whether the stored block state is air, i.e. represents empty space in the micro-block world.
	 *
	 * @return {@code true} if the block state is air; otherwise {@code false}.
	 */
	public boolean isAir() {
		return blockState().isAir();
	}

	@Override
	public BlockInformation createSnapshot() {
		return variant.map(stateVariant -> new BlockInformation(blockState, Optional.ofNullable(stateVariant.createSnapshot())))
				.orElseGet(() -> new BlockInformation(blockState, Optional.empty()));
	}

	@Override
	public int compareTo(final BlockInformation blockInformation) {
		return COMPARATOR.compare(this, blockInformation);
	}

	@Override
	public Codec<BlockInformation> codec() {
		return CODEC;
	}

	@Override
	public MapCodec<BlockInformation> mapCodec() {
		return MAP_CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, BlockInformation> streamCodec() {
		return STREAM_CODEC;
	}

	/**
	 * Creates a fresh block entity for this block state at origin ({@link net.minecraft.core.BlockPos#ZERO}).
	 *
	 * @return the created block entity, or {@code null} if no entity could be constructed. The value may be {@code null}.
	 */
	@Nullable
	public BlockEntity newBlockEntityAtZero() {
		return newBlockEntity(BlockPos.ZERO);
	}

	/**
	 * Creates a fresh block entity for this block state at the given position, then applies this information's variant to it when present.
	 *
	 * @param pos the world position at which to create the block entity.
	 * @return the newly created block entity, or {@code null} if the block is not an entity block or no entity was constructed. The value may be {@code null}.
	 */
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos) {
		if (!(blockState.getBlock() instanceof EntityBlock entityBlock)) {
			return null;
		}

		final BlockEntity blockEntity = entityBlock.newBlockEntity(pos, blockState());

		if (blockEntity == null) {
			return null;
		}

		variant().ifPresent(variant -> variant.updateBlockEntity(blockEntity));

		return blockEntity;
	}
}
