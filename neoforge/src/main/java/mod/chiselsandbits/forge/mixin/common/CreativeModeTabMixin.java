package mod.chiselsandbits.forge.mixin.common;

import mod.chiselsandbits.registrars.ModCreativeTabs;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin
{

    @Inject(
        method = "hasAnyItems()Z",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    public void onHasAnyItems(final CallbackInfoReturnable<Boolean> cir) {
        final CreativeModeTab self = (CreativeModeTab) ((Object) this);
        if (self == ModCreativeTabs.CLIPBOARD.get()) {
            cir.setReturnValue(true);
        }
    }


}
