package mod.chiselsandbits.core.api;

import mod.chiselsandbits.client.ModConflictContext;
import mod.chiselsandbits.core.Log;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;

public class IMCHandlerKeyBinding implements IMCMessageHandler
{

	@Override
	public void excuteIMC(
			final IMCMessage message )
	{
		try
		{
			String errorName = "UNKNOWN";
			Item item = null;

			if ( message.isStringMessage() )
			{
				final String name = message.getStringValue();

				errorName = name;
				item = Item.REGISTRY.getObject( new ResourceLocation( name ) );

				// try finding the item in the mod instead...
				if ( item == null || item == Items.field_190931_a )
				{
					errorName = message.getSender() + ":" + name;
					item = Item.REGISTRY.getObject( new ResourceLocation( message.getSender(), name ) );
				}
			}
			else if ( message.isResourceLocationMessage() )
			{
				errorName = message.getResourceLocationValue().toString();
				item = Item.REGISTRY.getObject( message.getResourceLocationValue() );
			}
			else
			{
				Log.info( "Invalid Type for IMC: " + message.getMessageType().getName() );
				return;
			}

			if ( item == null || item == Items.field_190931_a )
			{
				throw new RuntimeException( "Unable to locate item " + errorName );
			}

			for ( ModConflictContext conflictContext : ModConflictContext.values() )
			{
				if ( conflictContext.getName().equals( message.key ) )
				{
					conflictContext.setItemActive( item );
				}
			}
		}
		catch ( final Throwable e )
		{
			Log.logError( "IMC registeritemwithkeybinding From " + message.getSender(), e );
		}
	}
}
