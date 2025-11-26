package mod.chiselsandbits.registrars;

import mod.chiselsandbits.api.modification.operation.IModificationOperationGroup;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModCuttingOperationGroups
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static IModificationOperationGroup ACROSS_AXIS = new IModificationOperationGroup()
    {
        @Override
        public Identifier getIcon()
        {
            return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "rotate");
        }

        @Override
        public Component getDisplayName()
        {
            return LocalStrings.PatternModificationGroupRotate.getText();
        }
    };
    public static IModificationOperationGroup ALL_AXI     = new IModificationOperationGroup()
    {
        @Override
        public Identifier getIcon()
        {
            return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "rotate");
        }

        @Override
        public Component getDisplayName()
        {
            return LocalStrings.PatternModificationGroupRotate.getText();
        }
    };

    private ModCuttingOperationGroups()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModCuttingOperationGroups. This is a utility class");
    }

    public static void onModConstruction()
    {
        LOGGER.info("Loaded cutting operation group configuration.");
    }
}
