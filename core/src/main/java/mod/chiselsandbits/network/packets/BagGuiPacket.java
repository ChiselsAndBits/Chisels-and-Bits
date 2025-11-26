package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class BagGuiPacket extends ModPacket
{

    public static final Identifier                             ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag_gui");
    public static final CustomPacketPayload.Type<BagGuiPacket> TYPE = new CustomPacketPayload.Type<>(ID);

	private int slotNumber = -1;
	private int mouseButton = -1;
	private boolean duplicateButton = false;
	private boolean holdingShift = false;

    public BagGuiPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    public BagGuiPacket(final int slotNumber, final int mouseButton, final boolean duplicateButton, final boolean holdingShift)
    {
        this.slotNumber = slotNumber;
        this.mouseButton = mouseButton;
        this.duplicateButton = duplicateButton;
        this.holdingShift = holdingShift;
    }

    @Override
	public void server(
			final ServerPlayer player )
	{
		//doAction( player );
	}

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt( slotNumber );
        buffer.writeInt( mouseButton );
        buffer.writeBoolean( duplicateButton );
        buffer.writeBoolean( holdingShift );
    }

	@Override
	public void readPayload(
			final RegistryFriendlyByteBuf buffer )
	{
        slotNumber = buffer.readInt();
        mouseButton = buffer.readInt();
        duplicateButton = buffer.readBoolean();
        holdingShift = buffer.readBoolean();
	}

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
