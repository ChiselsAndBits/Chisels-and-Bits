package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.rendering.type.IRenderTypeManager;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.registrars.ModBlocks;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class ItemBlockRenderTypes
{

    private ItemBlockRenderTypes()
    {
        throw new IllegalStateException("Can not instantiate an instance of: FallbackRenderTypes. This is a utility class");
    }

    public static void onClientConstruction() {
        IRenderTypeManager.getInstance().registerBlockFallbackRenderTypes(registrar -> {
            registrar.register(ModBlocks.CHISELED_BLOCK.get(), ChunkSectionLayer.TRANSLUCENT);
            registrar.register(ModBlocks.BIT_STORAGE.get(), ChunkSectionLayer.CUTOUT);
            registrar.register(ModBlocks.CHISELED_PRINTER.get(), ChunkSectionLayer.CUTOUT);
        });
    }
}
