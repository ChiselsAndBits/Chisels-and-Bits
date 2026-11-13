package mod.chiselsandbits.client.model.block;

import java.util.Objects;
import java.util.function.Function;

import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometry;
import org.jspecify.annotations.Nullable;

public final class ChiseledTerrainModelSelection {
	private ChiseledTerrainModelSelection() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	public static <T> Selection<T> select(final T fallback, final @Nullable T rectangleModel) {
		return rectangleModel == null
				? new Selection<>(Objects.requireNonNull(fallback, "fallback"), false)
				: new Selection<>(rectangleModel, true);
	}

	public static <T> Prepared<T> prepare(
			final T fallback,
			final @Nullable T rectangleModel,
			final Function<T, @Nullable Runnable> nativePreparer
	) {
		final Selection<T> selection = select(fallback, rectangleModel);
		final Runnable nativeEmission = selection.rectangleSelected() ? nativePreparer.apply(selection.model()) : null;
		return new Prepared<>(selection, nativeEmission);
	}

	public static <T> NativePrepared<T> prepareNativeGeometry(
			final T fallbackModel,
			final @Nullable TerrainGeometry geometry,
			final Function<TerrainGeometry, @Nullable Runnable> nativePreparer
	) {
		Objects.requireNonNull(fallbackModel, "fallbackModel");
		final Runnable emission = geometry == null ? null : nativePreparer.apply(geometry);
		return new NativePrepared<>(fallbackModel, emission);
	}

	public record Selection<T>(T model, boolean rectangleSelected) {
	}

	public record Prepared<T>(Selection<T> selection, @Nullable Runnable nativeEmission) {
	}

	public record NativePrepared<T>(T fallbackModel, @Nullable Runnable nativeEmission) {
		public boolean usesNativeGeometry() {
			return nativeEmission != null;
		}
	}
}
