package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ImportPatternCommandMessagePacket extends ModPacket
{
    public static final Identifier                                                  ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "import_pattern_command_message");
    public static final CustomPacketPayload.Type<ImportPatternCommandMessagePacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private String   name;

    public ImportPatternCommandMessagePacket(final String name)
    {
        this.name = name;
    }

    public ImportPatternCommandMessagePacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeUtf(name, 512);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        name = buffer.readUtf(512);
    }

    @Override
    public void client()
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleImportPatternCommandMessage(name));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
