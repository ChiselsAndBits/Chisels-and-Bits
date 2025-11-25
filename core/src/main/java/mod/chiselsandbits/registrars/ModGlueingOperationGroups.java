package mod.chiselsandbits.registrars;

import mod.chiselsandbits.api.modification.operation.IModificationOperationGroup;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModGlueingOperationGroups
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static IModificationOperationGroup ACROSS_AXIS = new IModificationOperationGroup()
    {
        @Override
        public ResourceLocation getIcon()
        {
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "rotate");
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
        public ResourceLocation getIcon()
        {
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "rotate");
        }

        @Override
        public Component getDisplayName()
        {
            return LocalStrings.PatternModificationGroupRotate.getText();
        }
    };

    private ModGlueingOperationGroups()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModGlueingOperationGroups. This is a utility class");
    }

    public static void onModConstruction()
    {
        LOGGER.info("Loaded Glueing operation group configuration.");
    }
}
