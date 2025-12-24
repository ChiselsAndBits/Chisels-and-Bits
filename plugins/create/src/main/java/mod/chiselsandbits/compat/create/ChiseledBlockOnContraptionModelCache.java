package mod.chiselsandbits.compat.create;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChiseledBlockOnContraptionModelCache {

    private IAreaShapeIdentifier identifier = null;
    private IBlockModelData modelData = null;
    private final Collection<BiConsumer<IAreaShapeIdentifier, IBlockModelData>> onSetConsumers = new ArrayList<>();

    public ChiseledBlockOnContraptionModelCache(IAreaShapeIdentifier identifier)
    {
        this.identifier = identifier;
    }

    public void setModelData(IAreaShapeIdentifier newShapeIdentifier, final IBlockModelData modelData)
    {
        this.identifier = newShapeIdentifier;
        this.modelData = modelData;
        this.onSetConsumers.forEach(consumer -> consumer.accept(this.identifier, this.modelData));
    }

    public void addConsumer(final BiConsumer<IAreaShapeIdentifier, IBlockModelData> modelDataConsumer) {
        if (this.modelData != null && this.identifier != null)
            modelDataConsumer.accept(this.identifier, this.modelData);

        this.onSetConsumers.add(modelDataConsumer);
    }

    public IAreaShapeIdentifier getIdentifier() {
        return identifier;
    }

    public IBlockModelData getModelData() {
        return modelData;
    }
}
