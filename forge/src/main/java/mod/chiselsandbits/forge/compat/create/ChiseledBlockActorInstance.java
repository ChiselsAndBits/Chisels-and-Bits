package mod.chiselsandbits.forge.compat.create;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataManager;

public class ChiseledBlockActorInstance extends ActorInstance {

    private final ChiseledBlockInstance instance;

    public ChiseledBlockActorInstance(MaterialManager materialManager, VirtualRenderWorld world, MovementContext context) {
        super(materialManager, world, context);

        if (context.temporaryData == null) {
            final ChiseledBlockEntity chiseledBlockEntity = new ChiseledBlockEntity(context.localPos.offset(context.contraption.anchor), context.state);
            chiseledBlockEntity.setLevel(context.world);

            final ChiseledBlockOnContraptionModelCache modelUpdateHolder = new ChiseledBlockOnContraptionModelCache(IAreaShapeIdentifier.DUMMY);
            context.temporaryData = modelUpdateHolder;

            chiseledBlockEntity.deserializeNBT(context.blockEntityData, () -> ChiseledBlockModelDataManager.getInstance().updateModelData(
                    chiseledBlockEntity,
                    () -> modelUpdateHolder.setModelData(chiseledBlockEntity.createNewShapeIdentifier(), chiseledBlockEntity.getBlockModelData()),
                    true
            ));
        }

        instance = new ChiseledBlockInstance(materialManager, context.state, context.localPos, (ChiseledBlockOnContraptionModelCache) context.temporaryData);
        instance.setInitialBlockLight(localBlockLight());
    }

    @Override
    public void beginFrame() {
        instance.beginFrame();
    }
}
