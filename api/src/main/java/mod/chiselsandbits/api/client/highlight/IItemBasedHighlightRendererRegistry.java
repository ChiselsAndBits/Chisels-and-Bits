package mod.chiselsandbits.api.client.highlight;

import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import net.minecraft.world.item.Item;

/**
 * Registry for item based highlight renderers.
 * This registry is used to register item based highlight renderers for items.
 */
public interface IItemBasedHighlightRendererRegistry {

    /**
     * Returns the instance of the registry.
     *
     * @return The instance of the registry.
     */
	static IItemBasedHighlightRendererRegistry getInstance() {
		return IChiselsAndBitsAPI.getInstance().getItemBasedHighlightRendererRegistry();
	}

    /**
     * Registers an item based highlight renderer for the given item.
     *
     * @param item The item to register the renderer for.
     * @param renderer The renderer to register.
     */
    void register(Item item, IItemBasedHighlightRenderer renderer);
}
