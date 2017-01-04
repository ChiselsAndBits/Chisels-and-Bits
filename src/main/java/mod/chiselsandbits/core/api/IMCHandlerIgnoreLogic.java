package mod.chiselsandbits.core.api;

import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.core.Log;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;

public class IMCHandlerIgnoreLogic implements IMCMessageHandler
{

	@Override
	public void excuteIMC(
			final IMCMessage message )
	{
		try
		{
			String errorName = "UNKNOWN";
			Block blk = null;

			if ( message.isStringMessage() )
			{
				final String name = message.getStringValue();

				errorName = name;
				blk = Block.REGISTRY.getObject( new ResourceLocation( name ) );

				// try finding the block in the mod instead...
				if ( blk == null || blk == Blocks.AIR )
				{
					errorName = message.getSender() + ":" + name;
					blk = Block.REGISTRY.getObject( new ResourceLocation( message.getSender(), name ) );
				}
			}
			else if ( message.isResourceLocationMessage() )
			{
				errorName = message.getResourceLocationValue().toString();
				blk = Block.REGISTRY.getObject( message.getResourceLocationValue() );
			}
			else
			{
				Log.info( "Invalid Type for IMC: " + message.getMessageType().getName() );
				return;
			}

			if ( blk != null && blk != Blocks.AIR )
			{
				BlockBitInfo.ignoreBlockLogic( blk );
			}
			else
			{
				throw new RuntimeException( "Unable to locate block " + errorName );
			}
		}
		catch ( final Throwable e )
		{
			Log.logError( "IMC ignoreblocklogic From " + message.getSender(), e );
		}
	}
}
