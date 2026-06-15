package mod.chiselsandbits.api.serialization;

import java.io.*;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function7;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public interface CBStreamCodecs {
	StreamCodec<FriendlyByteBuf, long[]> LONG_ARRAY = new StreamCodec<>() {
		public long @NotNull [] decode(FriendlyByteBuf buffer) {
			return buffer.readLongArray();
		}

		public void encode(FriendlyByteBuf buffer, long @NotNull [] payload) {
			buffer.writeLongArray(payload);
		}
	};

	StreamCodec<FriendlyByteBuf, BitSet> BIT_SET = LONG_ARRAY.map(
			BitSet::valueOf,
			BitSet::toLongArray
	);

	StreamCodec<ByteBuf, Vec3> VEC_3 = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(3))
			.apply(codec -> codec.map(
					doubles -> {
						if (doubles.size() != 3) {
							throw new IllegalArgumentException("Expected 3 doubles, got " + doubles.size());
						}

						return new Vec3(doubles.get(0), doubles.get(1), doubles.get(2));
					},
					vec3 -> List.of(vec3.x(), vec3.y(), vec3.z())
			));

	static <R, C, V, B extends ByteBuf> StreamCodec<? super B, Table<R, C, V>> table(
			IntFunction<Map<R, Map<C,V>>> mapFactory,
			IntFunction<Map<C, V>> innerMapFactory,
			Supplier<Map<C, V>> innerMapSupplier,
			StreamCodec<? super B, R> rowCodec,
			StreamCodec<? super B, C> columnCodec,
			StreamCodec<? super B, V> valueCodec
	) {
		final StreamCodec<? super B, Map<C, V>> columnMapCodec = ByteBufCodecs.map(
				innerMapFactory,
				columnCodec,
				valueCodec
		);

		final StreamCodec<? super B, Map<R, Map<C, V>>> tableMapCodec = ByteBufCodecs.map(
				mapFactory,
				rowCodec,
				columnMapCodec
		);

		return tableMapCodec.map(
				rMapMap -> {
					final Table<R, C, V> table = HashBasedTable.create();
					rMapMap.forEach((row, columnMap) -> {
						columnMap.forEach((column, value) -> table.put(row, column, value));
					});
					return table;
				},
				Table::rowMap
		);
	}

	static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5, final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6, final StreamCodec<? super B, T7> codec7, final Function<C, T7> getter7, final Function7<T1, T2, T3, T4, T5, T6, T7, C> p_331335_) {
		return new StreamCodec<>() {
			public @NotNull C decode(@NotNull B buffer) {
				T1 t1 = codec1.decode(buffer);
				T2 t2 = codec2.decode(buffer);
				T3 t3 = codec3.decode(buffer);
				T4 t4 = codec4.decode(buffer);
				T5 t5 = codec5.decode(buffer);
				T6 t6 = codec6.decode(buffer);
				T7 t7 = codec7.decode(buffer);
				return p_331335_.apply(t1, t2, t3, t4, t5, t6, t7);
			}

			public void encode(@NotNull B buffer, @NotNull C instance) {
				codec1.encode(buffer, getter1.apply(instance));
				codec2.encode(buffer, getter2.apply(instance));
				codec3.encode(buffer, getter3.apply(instance));
				codec4.encode(buffer, getter4.apply(instance));
				codec5.encode(buffer, getter5.apply(instance));
				codec6.encode(buffer, getter6.apply(instance));
				codec7.encode(buffer, getter7.apply(instance));
			}
		};
	}

	public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1,
																										final Function<C, T1> getter1,
																										final StreamCodec<? super B, T2> codec2,
																										final Function<C, T2> getter2,
																										final StreamCodec<? super B, T3> codec3,
																										final Function<C, T3> getter3,
																										final StreamCodec<? super B, T4> codec4,
																										final Function<C, T4> getter4,
																										final StreamCodec<? super B, T5> codec5,
																										final Function<C, T5> getter5,
																										final StreamCodec<? super B, T6> codec6,
																										final Function<C, T6> getter6,
																										final StreamCodec<? super B, T7> codec7,
																										final Function<C, T7> getter7,
																										final StreamCodec<? super B, T8> codec8,
																										final Function<C, T8> getter8,
																										final StreamCodec<? super B, T9> codec9,
																										final Function<C, T9> getter9,
																										final StreamCodec<? super B, T10> codec10,
																										final Function<C, T10> getter10,
																										final StreamCodec<? super B, T11> codec11,
																										final Function<C, T11> getter11,
																										final StreamCodec<? super B, T12> codec12,
																										final Function<C, T12> getter12,
																										final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> p_331335_) {
		return new StreamCodec<>() {
			public @NotNull C decode(@NotNull B buffer) {
				T1 t1 = codec1.decode(buffer);
				T2 t2 = codec2.decode(buffer);
				T3 t3 = codec3.decode(buffer);
				T4 t4 = codec4.decode(buffer);
				T5 t5 = codec5.decode(buffer);
				T6 t6 = codec6.decode(buffer);
				T7 t7 = codec7.decode(buffer);
				T8 t8 = codec8.decode(buffer);
				T9 t9 = codec9.decode(buffer);
				T10 t10 = codec10.decode(buffer);
				T11 t11 = codec11.decode(buffer);
				T12 t12 = codec12.decode(buffer);
				return p_331335_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
			}

			public void encode(@NotNull B buffer, @NotNull C instance) {
				codec1.encode(buffer, getter1.apply(instance));
				codec2.encode(buffer, getter2.apply(instance));
				codec3.encode(buffer, getter3.apply(instance));
				codec4.encode(buffer, getter4.apply(instance));
				codec5.encode(buffer, getter5.apply(instance));
				codec6.encode(buffer, getter6.apply(instance));
				codec7.encode(buffer, getter7.apply(instance));
				codec8.encode(buffer, getter8.apply(instance));
				codec9.encode(buffer, getter9.apply(instance));
				codec10.encode(buffer, getter10.apply(instance));
				codec11.encode(buffer, getter11.apply(instance));
				codec12.encode(buffer, getter12.apply(instance));
			}
		};
	}

	static <T> StreamCodec<RegistryFriendlyByteBuf, T> compressed(StreamCodec<RegistryFriendlyByteBuf, T> codec) {
		return new StreamCodec<>() {
			@Override
			public @NotNull T decode(@NotNull RegistryFriendlyByteBuf buffer) {
				final byte[] compressedData = buffer.readByteArray();
				final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);

				try {
					final LZ4FrameInputStream lz4FrameInputStream = new LZ4FrameInputStream(byteArrayInputStream);
					final ByteBuf buf = Unpooled.wrappedBuffer(lz4FrameInputStream.readAllBytes());
					final RegistryFriendlyByteBuf innerBuffer = new RegistryFriendlyByteBuf(buf, buffer.registryAccess());

					final T decoded = codec.decode(innerBuffer);

					innerBuffer.release();
					buf.release();
					lz4FrameInputStream.close();
					byteArrayInputStream.close();
					return decoded;
				} catch (IOException e) {
					throw new RuntimeException("Failed to decompress data.", e);
				}
			}

			@Override
			public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull T value) {
				try {
					final ByteBuf uncompressedBuffer = buffer.alloc().buffer();
					final RegistryFriendlyByteBuf innerBuffer = new RegistryFriendlyByteBuf(uncompressedBuffer, buffer.registryAccess());
					codec.encode(innerBuffer, value);
					final byte[] uncompressedData = new byte[uncompressedBuffer.readableBytes()];
					uncompressedBuffer.readBytes(uncompressedData);

					innerBuffer.release();
					uncompressedBuffer.release();

					final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					final OutputStream lz4Stream = new LZ4FrameOutputStream(outputStream);
					lz4Stream.write(uncompressedData);

					final byte[] compressedData = outputStream.toByteArray();

					lz4Stream.close();
					outputStream.close();

					buffer.writeByteArray(compressedData);
				} catch (IOException e) {
					throw new RuntimeException("Failed to compress data.", e);
				}
			}
		};
	}

	static <B, R, D> StreamCodec<B, R> dispatch(
			StreamCodec<? super B, D> dispatcherCodec,
			Function<D, StreamCodec<B, ? extends R>> decoderFactory,
			Function<R, D> dispatcherSelector
	) {
		return new StreamCodec<>() {
			@Override
			public @NotNull R decode(@NotNull B buffer) {
				D dispatcher = dispatcherCodec.decode(buffer);
				StreamCodec<B, ? extends R> codec = decoderFactory.apply(dispatcher);
				return codec.decode(buffer);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void encode(@NotNull B buffer, @NotNull R value) {
				D dispatcher = dispatcherSelector.apply(value);
				dispatcherCodec.encode(buffer, dispatcher);
				StreamCodec<B, R> codec = (StreamCodec<B, R>) decoderFactory.apply(dispatcher);
				codec.encode(buffer, value);
			}
		};
	}

	static <B, T> StreamCodec<B, T> lazyInitialized(Supplier<StreamCodec<B, T>> supplier) {
		return new StreamCodec<B, T>() {
			private StreamCodec<B, T> codec;

			@Override
			public @NotNull T decode(@NotNull B buffer) {
				if (codec == null) {
					codec = supplier.get();
				}

				return codec.decode(buffer);
			}

			@Override
			public void encode(@NotNull B buffer, @NotNull T value) {
				if (codec == null) {
					codec = supplier.get();
				}

				codec.encode(buffer, value);
			}
		};
	}

	static <B extends FriendlyByteBuf, S> StreamCodec.CodecOperation<B, S, S> nullable() {
		return new StreamCodec.CodecOperation<>() {
			@Override
			public @NotNull StreamCodec<B, S> apply(@NotNull StreamCodec<B, S> codec) {
				return new StreamCodec<>() {
					@SuppressWarnings("NullableProblems")
					@Override
					@Nullable
					public S decode(B buffer) {
						return buffer.readBoolean() ? codec.decode(buffer) : null;
					}

					@Override
					public void encode(@NotNull B buffer, @Nullable S value) {
						buffer.writeBoolean(value != null);

						if (value != null) {
							codec.encode(buffer, value);
						}
					}
				};
			}
		};
	}
}
