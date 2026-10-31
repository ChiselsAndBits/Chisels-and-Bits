package mod.chiselsandbits.api.block.storage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.api.serialization.Serializable;
import mod.chiselsandbits.api.util.constants.NbtConstants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Maps block information to compact indices used by multistate storage.
 *
 * @param paletteEntries entries in palette-index order
 * @param paletteMap lookup from block information to its palette entry
 */

public record StateEntryPalette(
		List<Entry> paletteEntries,
		Map<BlockInformation, Entry> paletteMap) {
	/**
	 * Codec for serializing the palette's ordered entries.
	 */
	public static final Codec<StateEntryPalette> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Entry.CODEC.listOf().fieldOf(NbtConstants.ENTRIES).forGetter(StateEntryPalette::paletteEntries)
			).apply(instance, StateEntryPalette::new));

	/**
	 * Codec for reading palettes written with the legacy entry format.
	 */
	public static final Codec<StateEntryPalette> LEGACY_CODEC =
			Entry.LEGACY_CODEC.listOf()
				.xmap(StateEntryPalette::new, StateEntryPalette::paletteEntries);

	/**
	 * Map codec for embedding this palette in another serialized object.
	 */
	public static final MapCodec<StateEntryPalette> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Entry.CODEC.listOf().fieldOf(NbtConstants.ENTRIES).forGetter(StateEntryPalette::paletteEntries)
			).apply(instance, StateEntryPalette::new));

	/**
	 * Network codec for the ordered palette entries.
	 */
	public static final StreamCodec<RegistryFriendlyByteBuf, StateEntryPalette> STREAM_CODEC = StreamCodec.composite(
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
			StateEntryPalette::paletteEntries,
			StateEntryPalette::new
	);

	/**
	 * Creates an empty palette containing the required air entry.
	 */
	public StateEntryPalette() {
		this(List.of(), Map.of());
	}

	/**
	 * Copies the palette entries and lookup map from another palette.
	 *
	 * @param palette palette whose entries and lookup map are copied
	 */
	public StateEntryPalette(final StateEntryPalette palette) {
		this(palette.paletteEntries(), palette.paletteMap());
	}

	/**
	 * Creates a palette and lookup map from the supplied entries.
	 *
	 * @param paletteEntries entries to store in palette-index order
	 */
	public StateEntryPalette(final List<Entry> paletteEntries) {
		this(paletteEntries, paletteEntries.stream().collect(Collectors.toMap(Entry::blockInformation, Function.identity())));
	}

	/**
	 * Creates a palette from its ordered entries and state lookup map, adding air if absent.
	 */
	public StateEntryPalette(List<Entry> paletteEntries, Map<BlockInformation, Entry> paletteMap) {
		this.paletteEntries = Lists.newCopyOnWriteArrayList();
		this.paletteMap = Maps.newConcurrentMap();

		this.paletteEntries.addAll(paletteEntries);
		this.paletteMap.putAll(paletteMap);

		if (!paletteMap.containsKey(BlockInformation.AIR)) {
			this.paletteMap.put(BlockInformation.AIR, new Entry(BlockInformation.AIR));
			this.paletteEntries.add(this.paletteMap.get(BlockInformation.AIR));
		}
	}

	/**
	 * Returns the palette entries in index order as an unmodifiable list.
	 *
	 * @return an unmodifiable view of the entries in palette-index order
	 */
	public List<Entry> paletteEntries() {
		return Collections.unmodifiableList(paletteEntries);
	}

	/**
	 * Returns the block-information lookup as an unmodifiable map.
	 *
	 * @return an unmodifiable view mapping each block information value to its entry
	 */
	public Map<BlockInformation, Entry> paletteMap() {
		return Collections.unmodifiableMap(paletteMap);
	}

	/**
	 * Returns the number of entries currently stored in the palette.
	 *
	 * @return the number of palette entries, including the required air entry
	 */
	public long size() {
		return this.paletteEntries.size();
	}

	/**
	 * Represents the size information of the palette.
	 *
	 * <p>
	 * The size is only accurate if it has changed.
	 *
	 * @param hasChanged Whether the size has changed.
	 * @param size The size of the palette, 0 if not changed.
	 */
	public record SizeInformation(
			boolean hasChanged,
			int size) {
		/**
		 * Creates a result indicating that the palette size did not change.
		 *
		 * @return size information with {@code hasChanged} false and size zero
		 */
		public static SizeInformation notChanged() {
			return new SizeInformation(false, 0);
		}

		/**
		 * Creates a result indicating that the palette size changed to the supplied value.
		 *
		 * @param size new palette size
		 * @return size information containing the supplied size and a true change flag
		 */
		public static SizeInformation changed(int size) {
			return new SizeInformation(true, size);
		}
	}

	/**
	 * Represents the result of a palette operation.
	 *
	 * @param value value produced by the palette operation
	 * @param size palette-size change reported by the operation
	 * @param <T> The type of the value.
	 */
	public record Result<T>(
			T value,
			SizeInformation size) {
		/**
		 * Wraps a result value when the operation left palette size unchanged.
		 *
		 * @param value value produced by the operation
		 * @param <T> type of the wrapped value
		 * @return a result marked as having no palette-size change
		 */
		public static <T> Result<T> notChanged(T value) {
			return new Result<>(value, SizeInformation.notChanged());
		}

		/**
		 * Wraps a result value and the new palette size after a change.
		 *
		 * @param value value produced by the operation
		 * @param size new palette size
		 * @param <T> type of the wrapped value
		 * @return a result marked with the supplied new palette size
		 */
		public static <T> Result<T> changed(T value, int size) {
			return new Result<>(value, SizeInformation.changed(size));
		}

		/**
		 * Invokes the consumer only when the operation changed palette size.
		 *
		 * @param consumer callback that receives the new palette size when it changed
		 */
		public void whenSizeChanged(Consumer<Integer> consumer) {
			if (size.hasChanged()) {
				consumer.accept(size.size());
			}
		}
	}

	/**
	 * Returns the existing index for a state, adding it to the palette when absent.
	 *
	 * @param state block information to look up or add
	 * @return the palette index and whether adding the state changed the palette size
	 */
	public Result<Integer> getIndex(final BlockInformation state) {
		if (this.paletteMap().containsKey(state)) {
			final Entry entry = this.paletteMap().get(state);
			return Result.notChanged(this.paletteEntries().indexOf(entry));
		}

		final Entry newEntry = new Entry(state);
		this.paletteMap.put(state, newEntry);

		this.paletteEntries.add(newEntry);

		return Result.changed(this.paletteEntries().size() - 1, this.paletteEntries().size());
	}

	/**
	 * Resolves a palette index to its block information, using air for invalid indices.
	 *
	 * @param blockStateId palette index to resolve
	 * @return the indexed block information, or air when the index is invalid
	 */
	public BlockInformation getBlockState(final int blockStateId) {
		if (this.paletteEntries().isEmpty()) {
			return BlockInformation.AIR;
		}

		if (blockStateId < 0 || blockStateId >= this.paletteEntries.size()) {
			return getBlockState(0);
		}

		return this.paletteEntries.get(blockStateId).blockInformation();
	}

	/**
	 * Clears all entries and restores the required air entry.
	 *
	 * @return the palette index of the restored air entry and the resulting size change
	 */
	public Result<Integer> clear() {
		this.paletteEntries.clear();
		this.paletteMap.clear();
		return this.getIndex(BlockInformation.AIR);
	}

	/**
	 * Returns the block information values currently represented by this palette.
	 *
	 * @return the block information keys currently present in the palette lookup
	 */
	public List<BlockInformation> states() {
		return this.paletteMap.keySet().stream().toList();
	}

	/**
	 * Palette entry pairing a packed palette index with its block information.
	 *
	 * @param blockInformation block information represented by this entry
	 */
	public record Entry(BlockInformation blockInformation) implements Serializable.Registry<Entry> {
		/**
		 * Codec for entries in the legacy block-information format.
		 */
		public static final Codec<Entry> LEGACY_CODEC = BlockInformation.LEGACY_CODEC
				.xmap(Entry::new, Entry::blockInformation);

		/**
		 * Codec for entries in the current serialized format.
		 */
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> {
			return instance.group(
							BlockInformation.CODEC.fieldOf("state").forGetter(Entry::blockInformation)
			).apply(instance, Entry::new);
		});

		/**
		 * Map codec for embedding an entry in another serialized object.
		 */
		public static final MapCodec<Entry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> {
			return instance.group(
							BlockInformation.CODEC.fieldOf("state").forGetter(Entry::blockInformation)
			).apply(instance, Entry::new);
		});

		/**
		 * Network codec for a single palette entry.
		 */
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				BlockInformation.STREAM_CODEC,
				Entry::blockInformation,
				Entry::new
		);

		/**
		 * Returns the current serialized codec for this entry type.
		 */
		@Override
		public Codec<Entry> codec() {
			return CODEC;
		}

		/**
		 * Returns the current map codec for this entry type.
		 */
		@Override
		public MapCodec<Entry> mapCodec() {
			return MAP_CODEC;
		}

		/**
		 * Returns the network codec for this entry type.
		 */
		@Override
		public StreamCodec<RegistryFriendlyByteBuf, Entry> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
