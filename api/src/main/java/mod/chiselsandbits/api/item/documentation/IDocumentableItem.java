package mod.chiselsandbits.api.item.documentation;

import java.util.Map;

import com.communi.suggestu.scena.core.registries.IPlatformRegistryManager;
import com.google.common.collect.ImmutableMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents an item that is documented via the Chisels and Bits documentation export system.
 */
public interface IDocumentableItem {
	/**
	 * Gives access to the variants of the item that are exportable.
	 * @param item The item instance in question.
	 * @return The variants with their names as key.
	 */
	default Map<String, ItemStack> getDocumentableInstances(final Item item) {
		return ImmutableMap.of(IPlatformRegistryManager.getInstance().getItemRegistry().getKey(item).toString().replace(":", "/"), new ItemStack(item));
	}
}
