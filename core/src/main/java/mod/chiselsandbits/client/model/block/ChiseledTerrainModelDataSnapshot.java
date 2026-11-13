package mod.chiselsandbits.client.model.block;

import java.util.Objects;
import java.util.function.Supplier;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.client.models.data.IModelDataKey;
import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import org.jspecify.annotations.Nullable;

public record ChiseledTerrainModelDataSnapshot<T>(
		@Nullable T legacyModel,
		@Nullable T rectangleModel,
		@Nullable TerrainGeometry terrainGeometry
) {
	public static <T> ChiseledTerrainModelDataSnapshot<T> read(
			final Supplier<IBlockModelData> modelDataSupplier,
			final IModelDataKey<T> legacyKey,
			final @Nullable IModelDataKey<T> rectangleKey
	) {
		return read(modelDataSupplier, legacyKey, rectangleKey, null);
	}

	public static <T> ChiseledTerrainModelDataSnapshot<T> read(
			final Supplier<IBlockModelData> modelDataSupplier,
			final IModelDataKey<T> legacyKey,
			final @Nullable IModelDataKey<T> rectangleKey,
			final @Nullable IModelDataKey<TerrainGeometry> terrainGeometryKey
	) {
		final IBlockModelData modelData = Objects.requireNonNull(modelDataSupplier.get(), "modelData");
		return new ChiseledTerrainModelDataSnapshot<>(
				modelData.getData(legacyKey),
				rectangleKey == null ? null : modelData.getData(rectangleKey),
				terrainGeometryKey == null ? null : modelData.getData(terrainGeometryKey)
		);
	}

	public boolean hasAnyModel() {
		return legacyModel != null || rectangleModel != null || terrainGeometry != null;
	}
}
