package mod.chiselsandbits.client.colors;

import com.communi.suggestu.scena.core.client.rendering.IColorManager;
import com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter;
import it.unimi.dsi.fastutil.ints.IntList;
import mod.chiselsandbits.api.multistate.accessor.ISingleBlockAxisAlignedAreaAccessor;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhood;
import mod.chiselsandbits.client.model.block.ChiseledBlockStateModelManager;
import mod.chiselsandbits.client.model.builder.ChiseledBlockModelMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ChiseledBlockColorProvider implements IColorManager.IDynamicColorProvider
{
    @Override
    public void collect(final BlockState blockState, final BlockAndTintGetter blockAndTintGetter, final BlockPos blockPos, final IntList intList)
    {
        var blockEntity = blockAndTintGetter.getBlockEntity(blockPos);
        if (!(blockEntity instanceof ISingleBlockAxisAlignedAreaAccessor accessor)) {
            return;
        }

        var blockNeighborHood = IBlockNeighborhood.around(blockEntity);

        var model = ChiseledBlockStateModelManager.getInstance()
            .get(
                blockAndTintGetter,
                blockPos,
                accessor,
                blockNeighborHood
            );

        intList.clear();
        intList.size(model.materials().size());
        List<ChiseledBlockModelMaterial> materials = model.materials();
        for (int i = 0; i < materials.size(); i++)
        {
            final ChiseledBlockModelMaterial material = materials.get(i);
            if (material.tintIndex() == -1)
            {
                intList.set(i, -1);
                continue;
            }

            var tintSource = Minecraft.getInstance().getBlockColors().getTintSource(
                material.blockInformation().blockState(),
                material.tintIndex()
            );

            if (tintSource == null)
            {
                intList.add(i, -1);
                continue;
            }

            var simulatedEnvironment = new SingleBlockBlockAndTintGetter.Builder()
                .withBlockState(material.blockInformation().blockState())
                .withPos(blockPos)
                .withSource(blockAndTintGetter)
                .withBlockEntity(() -> material.blockInformation().newBlockEntity(blockPos))
                .createSingleBlockBlockAndTintGetter();

            var tint = tintSource.colorInWorld(
                material.blockInformation().blockState(),
                simulatedEnvironment,
                blockPos
            );

            intList.add(i, tint);
        }
    }
}
