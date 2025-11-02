package mod.scena.client.models.item;

import net.minecraft.client.renderer.item.ItemModel;

/**
 * Represents an {@link ItemModel} which has a delegate contained within it that might need unwrapping to further process it.
 */
public interface DelegateAwareItemModel extends ItemModel
{

    /**
     * {@return The inner delegate {@link ItemModel}.}
     */
    ItemModel delegate();
}
