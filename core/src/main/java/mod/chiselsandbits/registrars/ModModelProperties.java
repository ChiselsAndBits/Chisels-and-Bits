package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.client.models.data.IModelDataKey;
import mod.chiselsandbits.client.model.information.ChiseledBlockModelInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModModelProperties
{
    private static final Logger                                LOGGER = LogManager.getLogger();
    public static IModelDataKey<ChiseledBlockModelInformation> MODEL  = IModelDataKey.create();

    private ModModelProperties()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModModelProperties. This is a utility class");
    }

    public static void onModConstruction()
    {
        LOGGER.info("Loaded model property configuration.");
    }

}
