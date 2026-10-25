package mod.chiselsandbits.api.chiseling.mode;

import java.util.Optional;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import com.mojang.serialization.Codec;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.withmode.IToolMode;
import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.registries.IRegistryManager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Represents a chiselable operation that can be completed by a chisel or bit for example.
 * <p>
 * Has to be immutable as it is used in data components.
 */
public interface IChiselMode extends ICustomRegistryEntry, IToolMode<IToolModeGroup> {
	/**
	 * The default mode of the chisel or bit.
	 * @return The default mode.
	 */
	static IChiselMode getDefaultMode() {
		return IChiselsAndBitsAPI.getInstance().getDefaultChiselMode();
	}

	/**
	 * The underlying registry that contains the different modes a chisel can assume.
	 * @return The underlying forge registry.
	 */
	static ICustomRegistry<IChiselMode> getRegistry() {
		return IRegistryManager.getInstance().getChiselModeRegistry();
	}

	/**
	 * Invoked by the system when a chisel or a bit is left clicked to perform an associated operation.
	 *
	 * @param playerEntity The entity that is executing the operation.
	 * @param context The chiseling context.
	 *
	 * @return The processing state, which indicates how the interaction should proceed.
	 */
	ClickProcessingState onLeftClickBy(
			final Player playerEntity,
			final IChiselingContext context
	);

	/**
	 * Invoked by the system when the player has released the left click button.
	 *
	 * Currently not invoked by the system.Future endpoint.
	 *
	 * @param playerEntity The player who released the button.
	 * @param context The chiseling context.
	 */
	void onStoppedLeftClicking(
			final Player playerEntity,
			final IChiselingContext context
	);

	/**
	 * Invoked by the system when a chisel or a bit is right clicked to perform an associated operation.
	 *
	 * @param playerEntity The entity that is executing the operation.
	 * @param context The chiseling context.
	 *
	 * @return The processing state, which indicates how the interaction should proceed.
	 */
	ClickProcessingState onRightClickBy(
			final Player playerEntity,
			final IChiselingContext context
	);

	/**
	 * Invoked by the system when the player has released the right click button.
	 *
	 * Currently not invoked by the system.Future endpoint.
	 *
	 * @param playerEntity The player who released the button.
	 * @param context The chiseling context.
	 */
	void onStoppedRightClicking(
			final Player playerEntity,
			final IChiselingContext context
	);

	/**
	 * Performs an extraction and potential modification of the accessor for the given context as determined by this mode.
	 *
	 * @param context The chiseling context.
	 * @return An optional, potentially containing an area accessor.
	 */
	Optional<IAreaAccessor> getCurrentAccessor(final IChiselingContext context);

	/**
	 * Checks if the passed context is still valid for the given entity.
	 * @param playerEntity The entity to check for.
	 * @param context The context to check.
	 * @param modeOfOperation The mode of operandus for the check.
	 * @return True when still valid, false when not.
	 */
	default boolean isStillValid(
			final Player playerEntity,
			final IChiselingContext context,
			final ChiselingOperation modeOfOperation
	) {
		final IChiselingContext snapshot = IChiselingManager.getInstance().create(
				playerEntity,
				this,
				modeOfOperation,
				true,
				ItemStack.EMPTY
		);

		if (modeOfOperation == ChiselingOperation.CHISELING) {
			onLeftClickBy(playerEntity, snapshot);
		} else {
			onRightClickBy(playerEntity, snapshot);
		}

		if (snapshot.getMutator().isEmpty()) {
			return context.getMutator().isEmpty();
		}

		if (context.getMutator().isEmpty()) {
			return false;
		}

		return context.getMutator().get().getInWorldBoundingBox().equals(snapshot.getMutator().get().getInWorldBoundingBox());
	}

	/**
	 * Indicates that this mode is only available on a stack which has a placeable mode.
	 * Useful when placement and removal perform the same task AND requires a bit to be held.
	 * @return True when a "bit" needs to be held.
	 */
	default boolean requiresPlaceableEditStack() {
		return false;
	}

	/**
	 * Defines the shape of the area that is to be chiseled.
	 * This is the general shape, so a sphere, cube, or line any area specific changes are not returned by this method.
	 *
	 * @implNote This shape should make use of a heavily cached version, it will be called during rendering.
	 * @param context The current context that it is supposed to be for.
	 * @return The shape of the area to be chiseled.
	 */
	VoxelShape getShape(IChiselingContext context);

	/**
	 * Indicates if this mode requires a single click to trigger or not.
	 *
	 * @return True if single click, false if multiple clicks are needed.
	 */
	default boolean isSingleClickUse() {
		return true;
	}
}
