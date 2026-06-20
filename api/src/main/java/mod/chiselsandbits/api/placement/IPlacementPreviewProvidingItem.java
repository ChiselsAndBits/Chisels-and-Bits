package mod.chiselsandbits.api.placement;

import mod.chiselsandbits.api.item.wireframe.IWireframeProvidingItem;

import net.minecraft.world.item.ItemStack;

public interface IPlacementPreviewProvidingItem extends IWireframeProvidingItem, IPlaceable, IPlacementProperties {
	/**
     * Returns whether to effectively ignore the depth buffer and render in front of everything for a given placement result.
     *
     * @param heldStack The stack to get depth logic for.
     * @param placementResult The placement result to get depth logic for.
     * @return Whether depth is effectively ignored.
     */
	default boolean ignoreDepthForPlacement(
			ItemStack heldStack,
			PlacementResult placementResult
	)

	{
		return !placementResult.isSuccess() || ignoreDepth(heldStack);
	}

	/**
     * Returns whether to effectively ignore the depth buffer and render in front of everything
     *
     * @param heldStack The stack to get depth logic for.
     * @return Whether depth is effectively ignored.
     */
	default boolean ignoreDepth(ItemStack heldStack)

	{
		return overridesOccupiedBits(heldStack);
	}
}
