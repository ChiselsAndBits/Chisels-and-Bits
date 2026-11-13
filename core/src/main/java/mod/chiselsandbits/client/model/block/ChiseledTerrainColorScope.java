package mod.chiselsandbits.client.model.block;

import java.util.List;

import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

public final class ChiseledTerrainColorScope {
	private static final ThreadLocal<Binding> CURRENT = new ThreadLocal<>();

	private ChiseledTerrainColorScope() {
	}

	public static void run(
			final BlockAndTintGetter level, final BlockPos pos,
			final List<ChiseledBlockModelMaterial> materials, final Runnable emission
	) {
		final Binding previous = CURRENT.get();
		CURRENT.set(new Binding(level, pos.immutable(), materials));

		try {
			emission.run();
		} finally {
			if (previous == null) {
				CURRENT.remove();
			} else {
				CURRENT.set(previous);
			}
		}
	}

	public static @Nullable List<ChiseledBlockModelMaterial> materials(final BlockAndTintGetter level, final BlockPos pos) {
		final Binding binding = CURRENT.get();
		return binding != null && binding.level() == level && binding.pos().equals(pos) ? binding.materials() : null;
	}

	private record Binding(BlockAndTintGetter level, BlockPos pos, List<ChiseledBlockModelMaterial> materials) {
	}
}
