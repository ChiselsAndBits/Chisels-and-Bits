package mod.chiselsandbits.helpers;

import mod.chiselsandbits.ChiselMode;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.ReflectionWrapper;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.network.NetworkRouter;
import mod.chiselsandbits.network.packets.PacketSetChiselMode;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

public class ChiselModeManager
{
	private static ChiselMode clientChiselMode = ChiselMode.SINGLE;
	private static ChiselMode clientBitMode = ChiselMode.SINGLE;

	public static void changeChiselMode(
			final ChiselModeSetting setting,
			final ChiselMode originalMode,
			final ChiselMode newClientChiselMode )
	{
		final boolean chatNotification = ChiselsAndBits.getConfig().chatModeNotification;
		final boolean itemNameModeDisplay = ChiselsAndBits.getConfig().itemNameModeDisplay;

		if ( ChiselsAndBits.getConfig().perChiselMode && setting == ChiselModeSetting.CHISEL )
		{
			final PacketSetChiselMode packet = new PacketSetChiselMode();
			packet.mode = newClientChiselMode;
			packet.chatNotification = chatNotification;

			if ( !itemNameModeDisplay )
			{
				newClientChiselMode.setMode( Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem() );
			}

			NetworkRouter.instance.sendToServer( packet );
		}
		else
		{
			if ( setting == ChiselModeSetting.CHISEL )
			{
				clientChiselMode = newClientChiselMode;
			}
			else
			{
				clientBitMode = newClientChiselMode;
			}

			if ( originalMode != newClientChiselMode && chatNotification )
			{
				Minecraft.getMinecraft().thePlayer.addChatComponentMessage( new ChatComponentTranslation( newClientChiselMode.string.toString() ) );
			}

			ReflectionWrapper.instance.clearHighlightedStack();
		}

		if ( !itemNameModeDisplay )
		{
			ReflectionWrapper.instance.endHighlightedStack();
		}

	}

	public static void scrollOption(
			final ChiselModeSetting setting,
			final ChiselMode originalMode,
			ChiselMode currentMode,
			final int dwheel )
	{
		int offset = currentMode.ordinal() + ( dwheel < 0 ? -1 : 1 );

		if ( offset >= ChiselMode.values().length )
		{
			offset = 0;
		}

		if ( offset < 0 )
		{
			offset = ChiselMode.values().length - 1;
		}

		currentMode = ChiselMode.values()[offset];

		if ( currentMode.isDisabled )
		{
			scrollOption( setting, originalMode, currentMode, dwheel );
		}
		else
		{
			changeChiselMode( setting, originalMode, currentMode );
		}
	}

	public static ChiselMode getChiselMode(
			final ChiselModeSetting setting )
	{
		if ( setting == ChiselModeSetting.CHISEL )
		{
			if ( ChiselsAndBits.getConfig().perChiselMode )
			{
				final ItemStack ei = Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem();
				if ( ei != null && ei.getItem() instanceof IChiselModeItem )
				{
					return ChiselMode.getMode( ei );
				}
			}

			return clientChiselMode;
		}

		return clientBitMode;
	}

}