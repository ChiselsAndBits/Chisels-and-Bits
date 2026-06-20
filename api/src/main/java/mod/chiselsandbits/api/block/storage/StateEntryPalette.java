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

public record StateEntryPalette(List<Entry> paletteEntries, Map<BlockInformation, Entry> paletteMap) {
	public static final Codec<StateEntryPalette> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Entry.CODEC.listOf().fieldOf(NbtConstants.ENTRIES).forGetter(StateEntryPalette::paletteEntries)
					).apply(instance, StateEntryPalette::new));

	public static final Codec<StateEntryPalette> LEGACY_CODEC = 
	Entry.LEGACY_CODEC.listOf()
		.xmap(StateEntryPalette::new, StateEntryPalette::paletteEntries);

	public static final MapCodec<StateEntryPalette> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Entry.CODEC.listOf().fieldOf(NbtConstants.ENTRIES).forGetter(StateEntryPalette::paletteEntries)
					).apply(instance, StateEntryPalette::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, StateEntryPalette> STREAM_CODEC = StreamCodec.composite(
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
			StateEntryPalette::paletteEntries,
			StateEntryPalette::new
	);

	public StateEntryPalette() {
		this(List.of(), Map.of());
	}

	public StateEntryPalette(final StateEntryPalette palette) {
		this(palette.paletteEntries(), palette.paletteMap());
	}

	public StateEntryPalette(final List<Entry> paletteEntries) {
		this(paletteEntries, paletteEntries.stream().collect(Collectors.toMap(Entry::blockInformation, Function.identity())));
	}

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

	public List<Entry> paletteEntries() {
		return Collections.unmodifiableList(paletteEntries);
	}

	public Map<BlockInformation, Entry> paletteMap() {
		return Collections.unmodifiableMap(paletteMap);
	}

	public long size() {
		return this.paletteEntries.size();
	}

	/**
	 * Represents the size information of the palette.
	 * <p>
	 * The size is only accurate if it has changed.
	 *
	 * @param hasChanged Whether the size has changed.
	 * @param size The size of the palette, 0 if not changed.
	 */
	public record SizeInformation(boolean hasChanged, int size) {
		public static SizeInformation notChanged() {
			return new SizeInformation(false, 0);
		}

		public static SizeInformation changed(int size) {
			return new SizeInformation(true, size);
		}
	}

	/**
	 * Represents the result of a palette operation.
	 *
	 * @param value The value of the operation.
	 * @param size The size information of the palette.
	 * @param <T> The type of the value.
	 */
	public record Result<T>(T value, SizeInformation size) {
		public static <T> Result<T> notChanged(T value) {
			return new Result<>(value, SizeInformation.notChanged());
		}

		public static <T> Result<T> changed(T value, int size) {
			return new Result<>(value, SizeInformation.changed(size));
		}

		public void whenSizeChanged(Consumer<Integer> consumer) {
			if (size.hasChanged()) {
				consumer.accept(size.size());
			}
		}
	}

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

	public BlockInformation getBlockState(final int blockStateId) {
		if (this.paletteEntries().isEmpty()) {
			return BlockInformation.AIR;
		}

		if (blockStateId <0 || blockStateId>= this.paletteEntries.size()) {
			return getBlockState(0);
		}

		return this.paletteEntries.get(blockStateId).blockInformation();
	}

	public Result<Integer> clear() {
		this.paletteEntries.clear();
		this.paletteMap.clear();
		return this.getIndex(BlockInformation.AIR);
	}

	public List<BlockInformation> states() {
		return this.paletteMap.keySet().stream().toList();
	}

	public record Entry(BlockInformation blockInformation) implements Serializable.Registry<Entry> {
		public static final Codec<Entry> LEGACY_CODEC = BlockInformation.LEGACY_CODEC
			.xmap(Entry::new, Entry::blockInformation);

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> {
				return instance.group(
						BlockInformation.CODEC.fieldOf("state").forGetter(Entry::blockInformation)
						).apply(instance, Entry::new);
		}

		);

		public static final MapCodec<Entry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> {
				return instance.group(
						BlockInformation.CODEC.fieldOf("state").forGetter(Entry::blockInformation)
						).apply(instance, Entry::new);
		}

		);

		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				BlockInformation.STREAM_CODEC,
				Entry::blockInformation,
				Entry::new
		);

		@Override
		public Codec<Entry> codec() {
			return CODEC;
		}

		@Override
		public MapCodec<Entry> mapCodec() {
			return MAP_CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, Entry> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
