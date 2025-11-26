package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.BagContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class OpenBagGuiPacket extends ModPacket
{

    public static final Identifier                                 ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "open_bag_gui");
    public static final CustomPacketPayload.Type<OpenBagGuiPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public OpenBagGuiPacket(RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    public OpenBagGuiPacket()
    {
    }

    @Override
	public void server(
			final ServerPlayer player )
	{
	    player.openMenu(new SimpleMenuProvider(
          (id, playerInventory, playerEntity) -> new BagContainer(id, playerInventory),
          Component.translatable(LocalStrings.ContainerBitBag.toString())
        ));
	}

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
    }

    @Override
	public void readPayload(
			final RegistryFriendlyByteBuf buffer )
	{
	}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
