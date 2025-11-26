package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static com.communi.suggestu.scena.core.dist.Dist.CLIENT;

public final class AddMultiStateItemStackToClipboardPacket extends ModPacket
{

    public static final Identifier                                                        ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "add_multi_state_item_stack_to_clipboard");
    public static final CustomPacketPayload.Type<AddMultiStateItemStackToClipboardPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private ItemStack stack = ItemStack.EMPTY;

    public AddMultiStateItemStackToClipboardPacket(final ItemStack stack) {
        this.stack = stack;
    }

    public AddMultiStateItemStackToClipboardPacket(final RegistryFriendlyByteBuf byteBuf)
    {
        readPayload(byteBuf);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.stack);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void client()
    {
        DistExecutor.unsafeRunWhenOn(CLIENT, () -> () -> ClientPacketHandlers.handleAddMultiStateToClipboard(stack));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
