package mod.chiselsandbits.client.registrars;

import com.communi.suggestu.scena.core.client.models.IModelManager;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.item.properties.IsMeasuringItemProperty;
import net.minecraft.resources.Identifier;

public final class ModItemProperties
{

    private ModItemProperties() {
        throw new IllegalStateException("Can not instantiate an instance of: ItemProperties. This is a utility class");
    }

    public static void onClientConstruction() {
        IModelManager.getInstance().registerItemModelProperty(registrar -> {
            registrar.registerConditionalProperty(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "is_measuring"),
                IsMeasuringItemProperty.CODEC
            );
        });
    }
}
