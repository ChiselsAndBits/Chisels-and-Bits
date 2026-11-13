package mod.chiselsandbits.fabric.mixin.client;

import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

@Mixin(ChiseledBlockEntity.class)
public abstract class ChiseledBlockEntityRenderDataMixin implements RenderDataBlockEntity {
	@Shadow
	public abstract IBlockModelData getBlockModelData();

	@Override
	public Object getRenderData() {
		return getBlockModelData();
	}
}
