package mod.chiselsandbits.forge.data.model;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModItems;
import net.minecraft.data.DataGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class UnsealItemModelGenerator extends AbstractInteractableItemModelGenerator
{

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent.Client event)
    {
        event.getGenerator().addProvider(true, new UnsealItemModelGenerator(event.getGenerator()));
    }

    public UnsealItemModelGenerator(final DataGenerator generator)
    {
        super(generator, ModItems.UNSEAL_ITEM);
    }

    @Override
    public @NotNull String getName()
    {
        return "Unseal item model generator";
    }
}
