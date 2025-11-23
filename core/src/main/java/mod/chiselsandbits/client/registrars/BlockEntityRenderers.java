package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.rendering.IRenderingManager;
import mod.chiselsandbits.client.besr.BitStorageBESR;
import mod.chiselsandbits.client.besr.ChiseledPrinterBESR;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;

public final class BlockEntityRenderers
{

    private BlockEntityRenderers()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BlockEntityRenderers. This is a utility class");
    }

    public static void onClientConstruction() {
        IRenderingManager.getInstance().registerBlockEntityRenderer(
                registrar -> {
                    registrar.registerBlockEntityRenderer(ModBlockEntityTypes.BIT_STORAGE.get(), context -> new BitStorageBESR());
                }
        );

        IRenderingManager.getInstance().registerBlockEntityRenderer(
            registrar -> {
                registrar.registerBlockEntityRenderer(ModBlockEntityTypes.CHISELED_PRINTER.get(), context -> new ChiseledPrinterBESR());
            }
        );
    }
}
