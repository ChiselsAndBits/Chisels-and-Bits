package mod.chiselsandbits.compat.create;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataManager;

public class ChiseledBlockActorVisual extends ActorVisual
{
    private final ChiseledBlockInstance instance;

    public ChiseledBlockActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld world, MovementContext context) {
        super(visualizationContext, world, context);

        if (context.temporaryData == null) {
            final ChiseledBlockEntity chiseledBlockEntity = new ChiseledBlockEntity(context.localPos.offset(context.contraption.anchor), context.state);
            chiseledBlockEntity.setLevel(context.world);

            final ChiseledBlockOnContraptionModelCache modelUpdateHolder = new ChiseledBlockOnContraptionModelCache(IAreaShapeIdentifier.DUMMY);
            context.temporaryData = modelUpdateHolder;

            chiseledBlockEntity.deserializeNBT(context.data, world.registryAccess(), () -> ChiseledBlockModelDataManager.getInstance().updateModelData(
                    chiseledBlockEntity,
                    () -> modelUpdateHolder.setModelData(chiseledBlockEntity.createNewShapeIdentifier(), chiseledBlockEntity.getBlockModelData()),
                    true
            ));
        }

        instance = new ChiseledBlockInstance(visualizationContext.instancerProvider(), context.localPos, (ChiseledBlockOnContraptionModelCache) context.temporaryData);
        instance.setInitialBlockLight(localBlockLight());
    }

    @Override
    public void beginFrame() {
        instance.beginFrame();
    }

    @Override
    protected void _delete()
    {
        instance.delete();
    }
}
