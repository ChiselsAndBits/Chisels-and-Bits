package mod.chiselsandbits.api.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function7;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.jspecify.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * Stream codecs for Chisels &amp; Bits values and common packet transformations.
 */

public interface CBStreamCodecs {
	/**
	 * Network codec for arrays of 64-bit values.
	 */
	StreamCodec<FriendlyByteBuf, long[]> LONG_ARRAY = new StreamCodec<>() {
		public long[] decode(FriendlyByteBuf buffer) {
			return buffer.readLongArray();
		}

		public void encode(FriendlyByteBuf buffer, long[] payload) {
			buffer.writeLongArray(payload);
		}
	};

	/**
	 * Network codec for bit sets represented as arrays of 64-bit words.
	 */
	StreamCodec<FriendlyByteBuf, BitSet> BIT_SET = LONG_ARRAY.map(
			BitSet::valueOf,
			BitSet::toLongArray
	);

	/**
	 * Network codec for a three-component floating-point vector.
	 */
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

	/**
	 * Creates a codec that serializes a table as nested row-to-column maps.
	 *
	 * @param mapFactory factory for the outer row map
	 * @param innerMapFactory factory for each row's column map
	 * @param innerMapSupplier supplier for a new column map
	 * @param rowCodec codec for row keys
	 * @param columnCodec codec for column keys
	 * @param valueCodec codec for cell values
	 * @param <R> row-key type
	 * @param <C> column-key type
	 * @param <V> cell-value type
	 * @param <B> buffer type
	 * @return a codec that encodes and decodes the table's rows, columns, and values
	 */
	static <R, C, V, B extends ByteBuf> StreamCodec<? super B, Table<R, C, V>> table(
			IntFunction<Map<R, Map<C, V>>> mapFactory,
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

	/**
	 * Combines seven field codecs, accessors, and a constructor into a codec for a composite value.
	 *
	 * @param codec1 codec for the first field
	 * @param getter1 accessor for the first field
	 * @param codec2 codec for the second field
	 * @param getter2 accessor for the second field
	 * @param codec3 codec for the third field
	 * @param getter3 accessor for the third field
	 * @param codec4 codec for the fourth field
	 * @param getter4 accessor for the fourth field
	 * @param codec5 codec for the fifth field
	 * @param getter5 accessor for the fifth field
	 * @param codec6 codec for the sixth field
	 * @param getter6 accessor for the sixth field
	 * @param codec7 codec for the seventh field
	 * @param getter7 accessor for the seventh field
	 * @param p_331335_ function that constructs the composite value from the decoded fields
	 * @param <B> buffer type
	 * @param <C> composite value type
	 * @param <T1> first field type
	 * @param <T2> second field type
	 * @param <T3> third field type
	 * @param <T4> fourth field type
	 * @param <T5> fifth field type
	 * @param <T6> sixth field type
	 * @param <T7> seventh field type
	 * @return a codec that reads and writes the seven fields in order
	 */
	static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
			final StreamCodec<? super B, T1> codec1,
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
			final Function7<T1, T2, T3, T4, T5, T6, T7, C> p_331335_) {
		return new StreamCodec<>() {
			public C decode(B buffer) {
				T1 t1 = codec1.decode(buffer);
				T2 t2 = codec2.decode(buffer);
				T3 t3 = codec3.decode(buffer);
				T4 t4 = codec4.decode(buffer);
				T5 t5 = codec5.decode(buffer);
				T6 t6 = codec6.decode(buffer);
				T7 t7 = codec7.decode(buffer);
				return p_331335_.apply(t1, t2, t3, t4, t5, t6, t7);
			}

			public void encode(B buffer, C instance) {
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

	/**
	 * Combines twelve field codecs, accessors, and a constructor into a codec for a composite value.
	 *
	 * @param codec1 codec for the first field
	 * @param getter1 accessor for the first field
	 * @param codec2 codec for the second field
	 * @param getter2 accessor for the second field
	 * @param codec3 codec for the third field
	 * @param getter3 accessor for the third field
	 * @param codec4 codec for the fourth field
	 * @param getter4 accessor for the fourth field
	 * @param codec5 codec for the fifth field
	 * @param getter5 accessor for the fifth field
	 * @param codec6 codec for the sixth field
	 * @param getter6 accessor for the sixth field
	 * @param codec7 codec for the seventh field
	 * @param getter7 accessor for the seventh field
	 * @param codec8 codec for the eighth field
	 * @param getter8 accessor for the eighth field
	 * @param codec9 codec for the ninth field
	 * @param getter9 accessor for the ninth field
	 * @param codec10 codec for the tenth field
	 * @param getter10 accessor for the tenth field
	 * @param codec11 codec for the eleventh field
	 * @param getter11 accessor for the eleventh field
	 * @param codec12 codec for the twelfth field
	 * @param getter12 accessor for the twelfth field
	 * @param p_331335_ function that constructs the composite value from the decoded fields
	 * @param <B> buffer type
	 * @param <C> composite value type
	 * @param <T1> first field type
	 * @param <T2> second field type
	 * @param <T3> third field type
	 * @param <T4> fourth field type
	 * @param <T5> fifth field type
	 * @param <T6> sixth field type
	 * @param <T7> seventh field type
	 * @param <T8> eighth field type
	 * @param <T9> ninth field type
	 * @param <T10> tenth field type
	 * @param <T11> eleventh field type
	 * @param <T12> twelfth field type
	 * @return a codec that reads and writes the twelve fields in order
	 */
	static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
			final StreamCodec<? super B, T1> codec1,
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
			public C decode(B buffer) {
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

			public void encode(B buffer, C instance) {
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

	/**
	 * Compresses a registry-aware packet payload with LZ4.
	 *
	 * @param codec codec for the uncompressed payload
	 * @param <T> payload type
	 * @return a codec that compresses encoded payloads and decompresses them before decoding
	 */
	static <T> StreamCodec<RegistryFriendlyByteBuf, T> compressed(StreamCodec<RegistryFriendlyByteBuf, T> codec) {
		return new StreamCodec<>() {
			@Override
			public T decode(RegistryFriendlyByteBuf buffer) {
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
			public void encode(RegistryFriendlyByteBuf buffer, T value) {
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

	/**
	 * Selects a payload codec from a discriminator encoded before the payload.
	 *
	 * @param dispatcherCodec codec for the discriminator
	 * @param decoderFactory selects the payload codec for a decoded discriminator
	 * @param dispatcherSelector extracts the discriminator from a value when encoding
	 * @param <B> buffer type
	 * @param <R> payload type
	 * @param <D> discriminator type
	 * @return a codec that writes the discriminator before the corresponding payload
	 */
	static <B, R, D> StreamCodec<B, R> dispatch(
			StreamCodec<? super B, D> dispatcherCodec,
			Function<D, StreamCodec<B, ? extends R>> decoderFactory,
			Function<R, D> dispatcherSelector
	) {
		return new StreamCodec<>() {
			@Override
			public R decode(B buffer) {
				D dispatcher = dispatcherCodec.decode(buffer);
				StreamCodec<B, ? extends R> codec = decoderFactory.apply(dispatcher);
				return codec.decode(buffer);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void encode(B buffer, R value) {
				D dispatcher = dispatcherSelector.apply(value);
				dispatcherCodec.encode(buffer, dispatcher);
				StreamCodec<B, R> codec = (StreamCodec<B, R>) decoderFactory.apply(dispatcher);
				codec.encode(buffer, value);
			}
		};
	}

	/**
	 * Defers creation of a stream codec until its first encode or decode operation.
	 *
	 * @param supplier supplies the codec on its first use
	 * @param <B> buffer type
	 * @param <T> payload type
	 * @return a codec that initializes and caches the supplied codec on first use
	 */
	static <B, T> StreamCodec<B, T> lazyInitialized(Supplier<StreamCodec<B, T>> supplier) {
		return new StreamCodec<B, T>() {
			private StreamCodec<B, T> codec;

			@Override
			public T decode(B buffer) {
				if (codec == null) {
					codec = supplier.get();
				}

				return codec.decode(buffer);
			}

			@Override
			public void encode(B buffer, T value) {
				if (codec == null) {
					codec = supplier.get();
				}

				codec.encode(buffer, value);
			}
		};
	}

	/**
	 * Adds a presence bit around a codec so values may be encoded as null.
	 *
	 * @param <B> friendly byte-buffer type
	 * @param <S> non-null payload type handled by the wrapped codec
	 * @return a codec operation that encodes a presence bit before any non-null value
	 */
	static <B extends FriendlyByteBuf, S> StreamCodec.CodecOperation<B, S, S> nullable() {
		return new StreamCodec.CodecOperation<>() {
			@Override
			public StreamCodec<B, S> apply(StreamCodec<B, S> codec) {
				return new StreamCodec<>() {
					@SuppressWarnings("NullableProblems")
					@Override
					@Nullable
					public S decode(B buffer) {
						return buffer.readBoolean() ? codec.decode(buffer) : null;
					}

					@Override
					public void encode(B buffer, @Nullable S value) {
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
