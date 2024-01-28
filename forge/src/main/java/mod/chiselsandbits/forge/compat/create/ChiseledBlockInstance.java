package mod.chiselsandbits.forge.compat.create;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import com.google.common.collect.Maps;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.backend.RenderLayer;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.core.model.BlockModel;
import mod.chiselsandbits.api.multistate.accessor.identifier.IAreaShapeIdentifier;
import mod.chiselsandbits.registrars.ModModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChiseledBlockInstance {

    private final MaterialManager materialManager;
    private final BlockState blockState;
    private final BlockPos instancePos;

    private Map<RenderType, ModelData> modelData = Maps.newConcurrentMap();
    private int localBlockLight = -1;

    public ChiseledBlockInstance(MaterialManager materialManager, BlockState blockState, BlockPos instancePos, ChiseledBlockOnContraptionModelCache cache) {
        this.materialManager = materialManager;
        this.blockState = blockState;
        this.instancePos = instancePos;

        cache.addConsumer((identifier, modelData) -> {
            Minecraft.getInstance().execute(() -> {
                init(identifier, modelData);
            });
        });
    }

    public void init(IAreaShapeIdentifier identifier, IBlockModelData data) {
        record ModelKey(IAreaShapeIdentifier identifier, RenderType type){};

        final Map<RenderType, BakedModel> models = data.getData(ModModelProperties.KNOWN_LAYER_MODEL_PROPERTY);
        if (models == null)
            return;

        final Set<RenderType> removedTypes = modelData.keySet().stream().filter(t -> !models.containsKey(t)).collect(Collectors.toSet());
        removedTypes.forEach(key -> {
            modelData.get(key).delete();
            modelData.remove(key);
        });

        models.forEach((renderType, model) -> {
            final RenderLayer layer = RenderLayer.getLayer(renderType);
            if (layer == null)
                return;

            if (!modelData.containsKey(renderType)) {
                modelData.put(renderType, materialManager.state(layer, renderType)
                        .material(ChiseledBlockMaterials.CHISELED_BLOCK)
                        .model(new ModelKey(identifier, renderType), () -> BlockModel.of(model, blockState))
                        .createInstance());

                if (this.localBlockLight != -1) {
                    this.modelData.get(renderType).setBlockLight(this.localBlockLight);
                }
            }
            else {
                materialManager.state(layer, renderType)
                        .material(ChiseledBlockMaterials.CHISELED_BLOCK)
                        .model(new ModelKey(identifier, renderType), () -> BlockModel.of(model, blockState))
                        .stealInstance(modelData.get(renderType));
            }
        });
    }

    public void beginFrame() {
        if (modelData != null) {
            for (ModelData value : modelData.values()) {
                value.loadIdentity().translate(instancePos);
            }
        }
    }

    public void setInitialBlockLight(int localBlockLight) {
        this.localBlockLight = localBlockLight;
    }
}