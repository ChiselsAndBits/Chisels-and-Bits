package mod.chiselsandbits.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import org.jetbrains.annotations.Nullable;

public class ChiseledBlockMovementBehaviour implements MovementBehaviour
{

    @Override
    public @Nullable ActorVisual createVisual(final VisualizationContext visualizationContext, final VirtualRenderWorld simulationWorld, final MovementContext movementContext)
    {
        return new ChiseledBlockActorVisual(visualizationContext, simulationWorld, movementContext);
    }

    @Override
    public void startMoving(final MovementContext context)
    {
        context.data = context.blockEntityData;
    }
}
