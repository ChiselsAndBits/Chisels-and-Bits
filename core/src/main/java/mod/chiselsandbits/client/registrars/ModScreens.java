package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.screens.IScreenManager;
import mod.chiselsandbits.client.screens.BitBagScreen;
import mod.chiselsandbits.client.screens.ChiseledPrinterScreen;
import mod.chiselsandbits.client.screens.ModificationTableScreen;
import mod.chiselsandbits.registrars.ModContainerTypes;

public final class ModScreens
{

    private ModScreens() {
        throw new IllegalStateException("Can not instantiate an instance of: Screens. This is a utility class");
    }

    public static void onClientConstruction() {
        IScreenManager.getInstance().registerMenus(registrar -> {
            registrar.register(
                    ModContainerTypes.BIT_BAG.get(),
                    BitBagScreen::new
            );
            registrar.register(
                    ModContainerTypes.MODIFICATION_TABLE.get(),
                    ModificationTableScreen::new
            );
            registrar.register(
                    ModContainerTypes.CHISELED_PRINTER_CONTAINER.get(),
                    ChiseledPrinterScreen::new
            );
        });
    }
}
