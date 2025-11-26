package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ExportPatternCommandMessagePacket extends ModPacket
{
    public static final Identifier                                                  ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "export_pattern_command_message");
    public static final CustomPacketPayload.Type<ExportPatternCommandMessagePacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private BlockPos target;
    private String   name;

    public ExportPatternCommandMessagePacket(final BlockPos target, final String name)
    {
        this.target = target;
        this.name = name;
    }

    public ExportPatternCommandMessagePacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(target);
        buffer.writeUtf(name, 512);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        target = buffer.readBlockPos();
        name = buffer.readUtf(512);
    }

    @Override
    public void client()
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleExportPatternCommandMessage(target, name));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
