package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.rendering.IColorManager;
import mod.chiselsandbits.client.colors.ChiseledBlockColorProvider;
import mod.chiselsandbits.registrars.ModBlocks;

public final class ModBlockColors
{

    private ModBlockColors()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BlockColors. This is a utility class");
    }

    public static void onClientConstruction()
    {
        IColorManager.getInstance().setupDynamicBlockColors(
          configuration -> {
              configuration.register(
                      new ChiseledBlockColorProvider(),
                      ModBlocks.CHISELED_BLOCK.get()
              );
          }
        );
    }
}
