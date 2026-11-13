package mod.chiselsandbits.client.model.data;

import java.util.Objects;

import com.communi.suggestu.scena.core.client.rendering.terrain.TerrainGeometryGate;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

public class ChiseledBlockModelDataManager {
	private static final Runnable NOOP = () -> { };
	private static final ChiseledBlockModelDataManager INSTANCE = new ChiseledBlockModelDataManager();

	public static ChiseledBlockModelDataManager getInstance() {
		return INSTANCE;
	}

	private ChiseledBlockModelDataManager() {
	}

	public void updateModelData(@Nullable final ChiseledBlockEntity tileEntity) {
		this.updateModelData(tileEntity, NOOP, false);
	}

	public void updateNeighbours(final ChiseledBlockEntity tileEntity) {
		if (!TerrainGeometryGate.isEnabled()) {
			return;
		}
		final Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> updateNeighbours(tileEntity));
			return;
		}
		final var level = tileEntity.getLevel();
		if (level == null || !level.isClientSide() || tileEntity.isRemoved()) {
			return;
		}
		for (final Direction direction : Direction.values()) {
			final var pos = tileEntity.getBlockPos().relative(direction);
			if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ChiseledBlockEntity neighbour) {
				updateModelData(neighbour);
			}
		}
	}

	public void updateModelData(
			final ChiseledBlockEntity tileEntity,
			final Runnable onCompleteCallback,
			final boolean force
	) {
		if (!force) {
			if (tileEntity == null || !tileEntity.hasLevel() || !Objects.requireNonNull(tileEntity.getLevel()).isClientSide()) {
				return;
			}
		}

		ChiseledBlockModelDataExecutor.updateModelDataCore(tileEntity, onCompleteCallback);
	}
}
