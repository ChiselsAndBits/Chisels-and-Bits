package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.BagContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ClearBagGuiPacket extends ModPacket
{

    public static final Identifier                                  ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "clear_bag_gui");
    public static final CustomPacketPayload.Type<ClearBagGuiPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private ItemStack stack = null;

    public ClearBagGuiPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    public ClearBagGuiPacket(
      final ItemStack inHandItem)
    {
        stack = inHandItem;
    }

    @Override
    public void server(
      final ServerPlayer player)
    {
        execute(player);
    }

    public void execute(final Player player) {
        if (player.containerMenu instanceof BagContainer)
        {
            ((BagContainer) player.containerMenu).clear(stack);
        }
    }

    @Override
    public void readPayload(
      final RegistryFriendlyByteBuf buffer)
    {
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
