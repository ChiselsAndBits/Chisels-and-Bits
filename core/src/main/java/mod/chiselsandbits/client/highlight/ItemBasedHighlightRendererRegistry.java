package mod.chiselsandbits.client.highlight;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.client.highlight.IItemBasedHighlightRenderer;
import mod.chiselsandbits.api.client.highlight.IItemBasedHighlightRendererRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemBasedHighlightRendererRegistry implements IItemBasedHighlightRendererRegistry {

    private static final ItemBasedHighlightRendererRegistry INSTANCE = new ItemBasedHighlightRendererRegistry();

    public static ItemBasedHighlightRendererRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<Item, IItemBasedHighlightRenderer> itemBasedHighlightRenderers = Maps.newConcurrentMap();

    private ItemBasedHighlightRendererRegistry() {}

    @Override
    public void register(Item item, IItemBasedHighlightRenderer renderer) {
        itemBasedHighlightRenderers.put(item, renderer);
    }

    @Nullable
    public IItemBasedHighlightRenderer get(Item item) {
        return itemBasedHighlightRenderers.get(item);
    }

	public static ItemStack getItemWithCustomHighlightsFromHandsFor(@Nullable final Player playerEntity) {
		if (playerEntity == null) {
			return ItemStack.EMPTY;
		}

        if (getInstance().get(playerEntity.getMainHandItem().getItem()) != null) {
            return playerEntity.getMainHandItem();
        }

        if (getInstance().get(playerEntity.getOffhandItem().getItem()) != null) {
            return playerEntity.getOffhandItem();
        }

		return ItemStack.EMPTY;
	}
}
