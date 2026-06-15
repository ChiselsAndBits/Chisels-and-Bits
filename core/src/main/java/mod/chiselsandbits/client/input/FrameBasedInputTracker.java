package mod.chiselsandbits.client.input;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.input.ProcessingInputTracker;
import mod.chiselsandbits.network.packets.InputTrackerStatusUpdatePacket;

import net.minecraft.client.Minecraft;

public final class FrameBasedInputTracker
{
	private static final FrameBasedInputTracker INSTANCE = new FrameBasedInputTracker();

	public static FrameBasedInputTracker getInstance()

	{
		return INSTANCE;
	}

	private final InputTracker leftMouseTracker = new InputTracker(
	  () -> DistExecutor.runForDist(() -> () -> Minecraft.getInstance().options.keyAttack.isDown(), () -> () -> false),
	  () -> {
		  ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new InputTrackerStatusUpdatePacket(true, true));

		  DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ProcessingInputTracker.getInstance().onStartedLeftClicking(Minecraft.getInstance().player));
	  },
	  () -> {
		  ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new InputTrackerStatusUpdatePacket(true, false));

		  DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ProcessingInputTracker.getInstance().onStoppedLeftClicking(Minecraft.getInstance().player));
	  }

	);

	private final InputTracker rightMouseTracker = new InputTracker(
	  () -> DistExecutor.runForDist(() -> () -> Minecraft.getInstance().options.keyUse.isDown(), () -> () -> false),
	  () -> {
		  ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new InputTrackerStatusUpdatePacket(false, true));

		  DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ProcessingInputTracker.getInstance().onStartedRightClicking(Minecraft.getInstance().player));
	  },
	  () -> {
		  ChiselsAndBits.getInstance().getNetworkChannel().sendToServer(new InputTrackerStatusUpdatePacket(false, false));

		  DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ProcessingInputTracker.getInstance().onStoppedRightClicking(Minecraft.getInstance().player));
	  }

	);

	private FrameBasedInputTracker()

	{
	}

	public void onRenderFrame() {
		leftMouseTracker.tick();
		rightMouseTracker.tick();
	}

	private static final class InputTracker {
		private final Supplier<Boolean> isActiveChecker;
		private final Runnable onToggleOn;
		private final Runnable onToggleOff;

		private boolean isActive = false;

		private InputTracker(final Supplier<Boolean> isActiveChecker, final Runnable onToggleOn, final Runnable onToggleOff) {
			this.isActiveChecker = isActiveChecker;
			this.onToggleOn = onToggleOn;
			this.onToggleOff = onToggleOff;
		}

		public void tick() {
			final boolean newPotentialState = isActiveChecker.get();

			if (isActive && !newPotentialState) {
				onToggleOff.run();
				isActive = false;
			}

			else if (!isActive && newPotentialState) {
				onToggleOn.run();
				isActive = true;
			}
		}
	}
}
