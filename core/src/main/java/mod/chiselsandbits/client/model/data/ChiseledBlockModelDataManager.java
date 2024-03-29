package mod.chiselsandbits.client.model.data;

import com.google.common.collect.Table;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class ChiseledBlockModelDataManager
{
    private static final Runnable NOOP = () -> {};
    private static final ChiseledBlockModelDataManager INSTANCE = new ChiseledBlockModelDataManager();

    public static ChiseledBlockModelDataManager getInstance()
    {
        return INSTANCE;
    }


    private ChiseledBlockModelDataManager()
    {
    }

    public void updateModelData(@Nullable final ChiseledBlockEntity tileEntity) {
        this.updateModelData(tileEntity, NOOP, false);
    }

    public void updateModelData(
      final ChiseledBlockEntity tileEntity,
      final Runnable onCompleteCallback,
      final boolean force
    )
    {
        if (!force)
        {
            if (!tileEntity.hasLevel() || !tileEntity.getLevel().isClientSide() || tileEntity == null)
            {
                return;
            }
        }

        ChiseledBlockModelDataExecutor.updateModelDataCore(tileEntity, onCompleteCallback);
    }

    public void computeModelsSplit(
            final ChiseledBlockEntity tileEntity,
            final Consumer<Table<RenderType, IBlockInformation, BakedModel>> onCompleteCallback,
            final boolean force
    )
    {
        if (!force)
        {
            if (!tileEntity.hasLevel() || !tileEntity.getLevel().isClientSide() || tileEntity == null)
            {
                return;
            }
        }

        ChiseledBlockModelDataExecutor.updateModelDataPerContainedState(tileEntity, onCompleteCallback);
    }
}
