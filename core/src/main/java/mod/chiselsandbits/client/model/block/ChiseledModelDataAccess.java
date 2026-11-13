package mod.chiselsandbits.client.model.block;

import java.util.Objects;
import java.util.function.BiFunction;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.communi.suggestu.scena.core.entity.block.IBlockEntityWithModelData;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

/** Reads the loader's captured render data, rather than a live block entity during section compilation. */
public final class ChiseledModelDataAccess {
	private static volatile BiFunction<BlockAndTintGetter, BlockPos, @Nullable IBlockModelData> reader =
			(level, pos) -> level.getBlockEntity(pos) instanceof IBlockEntityWithModelData entity
				? entity.getBlockModelData() : null;

	private ChiseledModelDataAccess() {
	}

	public static void install(final BiFunction<BlockAndTintGetter, BlockPos, @Nullable IBlockModelData> platformReader) {
		reader = Objects.requireNonNull(platformReader);
	}

	public static @Nullable IBlockModelData read(final BlockAndTintGetter level, final BlockPos pos) {
		return reader.apply(level, pos);
	}
}
