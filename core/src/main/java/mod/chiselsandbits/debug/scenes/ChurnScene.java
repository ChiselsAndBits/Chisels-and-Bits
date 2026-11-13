package mod.chiselsandbits.debug.scenes;

import java.util.List;

import com.mojang.logging.LogUtils;
import mod.chiselsandbits.api.block.storage.StateEntryStorage;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Re-chisels a set of chiseled blocks on every server tick, to create a re-mesh storm.
 * Only ever accessed from the server thread.
 */
public final class ChurnScene {
	private static final ChurnScene INSTANCE = new ChurnScene();
	private static final Logger LOGGER = LogUtils.getLogger();

	private ServerLevel level;
	private List<BlockPos> positions = List.of();
	private List<StateEntryStorage> templates = List.of();
	private int remainingTicks;
	private long elapsedTicks;

	private ChurnScene() {
	}

	public static ChurnScene getInstance() {
		return INSTANCE;
	}

	/**
	 * Starts re-chiseling the given positions, cycling through the templates each tick.
	 * Replaces any churn that is still running.
	 */
	public void start(final ServerLevel level, final List<BlockPos> positions, final List<StateEntryStorage> templates, final int ticks) {
		this.level = level;
		this.positions = List.copyOf(positions);
		this.templates = List.copyOf(templates);
		this.remainingTicks = ticks;
		this.elapsedTicks = 0;
	}

	/**
	 * Stops the running churn.
	 *
	 * @return true if a churn was running.
	 */
	public boolean stop() {
		final boolean wasRunning = isRunning();
		this.remainingTicks = 0;
		this.level = null;
		return wasRunning;
	}

	public boolean isRunning() {
		return level != null && remainingTicks > 0;
	}

	public void onServerTick() {
		if (!isRunning()) {
			return;
		}

		for (int index = 0; index < positions.size(); index++) {
			if (level.getBlockEntity(positions.get(index)) instanceof ChiseledBlockEntity chiseledBlockEntity) {
				final int templateIndex = (int) ((elapsedTicks + index) % templates.size());
				chiseledBlockEntity.replaceStorage(templates.get(templateIndex).createSnapshot());
			}
		}

		elapsedTicks++;
		remainingTicks--;

		if (remainingTicks == 0) {
			LOGGER.info("Churn scene finished after {} ticks", elapsedTicks);
			level = null;
		}
	}
}
