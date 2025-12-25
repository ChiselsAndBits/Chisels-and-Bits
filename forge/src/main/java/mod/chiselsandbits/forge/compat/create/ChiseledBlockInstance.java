package mod.chiselsandbits.forge.compat.create;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.google.common.collect.Maps;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.ForgeBakedModelBuilder;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;

import java.util.Map;

public class ChiseledBlockInstance {

    private final InstancerProvider provider;
    private final Map<RenderType, TransformedInstance> modelData       = Maps.newConcurrentMap();
    private       int                                             localBlockLight = -1;
    private BlockPos localPos;

    public ChiseledBlockInstance(final InstancerProvider instancerProvider, final BlockPos localPos, ChiseledBlockOnContraptionModelCache cache) {
        this.provider = instancerProvider;
        this.localPos = localPos;

        cache.addConsumer((identifier, modelData) -> {
            Minecraft.getInstance().execute(() -> {
                init(modelData);
            });
        });
    }

    public void init(IBlockModelData data) {
        delete();
        modelData.clear();

        final Map<RenderType, BakedModel> models = data.getData(ModModelProperties.KNOWN_LAYER_MODEL_PROPERTY);
        if (models == null)
            return;

        models.forEach((renderType, model) -> {
            modelData.put(
                renderType,
                provider.instancer(InstanceTypes.TRANSFORMED, new ForgeBakedModelBuilder(model)
                    .build()).createInstance()
            );

            if (this.localBlockLight != -1) {
                this.modelData.get(renderType).light(this.localBlockLight);
            }
        });
    }

    public void beginFrame() {
        for (TransformedInstance value : modelData.values())
        {
            value.setIdentityTransform().translate(localPos).setChanged();
        }
    }

    public void setInitialBlockLight(int localBlockLight) {
        this.localBlockLight = localBlockLight;
    }

    void delete()
    {
        this.modelData.values().forEach(TransformedInstance::delete);
    }
}
