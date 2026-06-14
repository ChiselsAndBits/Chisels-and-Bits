package mod.chiselsandbits.api.item.chisel;

import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.item.change.IChangeTrackingItem;
import mod.chiselsandbits.api.item.click.ILeftClickControllingItem;
import mod.chiselsandbits.api.item.withhighlight.IWithHighlightItem;
import mod.chiselsandbits.api.item.withmode.IWithModeItem;

public interface IChiselingItem extends ILeftClickControllingItem, IWithModeItem<IChiselMode>, IWithHighlightItem, IChangeTrackingItem
{
	boolean isDamageableDuringChiseling();
}
