package mod.chiselsandbits.client.model.baked.base;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import org.jetbrains.annotations.NotNull;

public abstract class BaseBakedBlockModel extends BaseBakedPerspectiveModel implements BakedModel
{

	@Override
	final public boolean useAmbientOcclusion()
	{
		return true;
	}

	@Override
	final public boolean isGui3d()
	{
		return true;
	}

	@Override
	final public boolean isCustomRenderer()
	{
		return false;
	}

	@Override
	public @NotNull ItemOverrides getOverrides()
	{
		return ItemOverrides.EMPTY;
	}
}
