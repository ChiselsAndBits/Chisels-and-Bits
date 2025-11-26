package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import io.netty.buffer.Unpooled;
import mod.chiselsandbits.api.block.entity.INetworkUpdatableEntity;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.communi.suggestu.scena.core.dist.Dist.CLIENT;

public final class UpdateBlockEntityPacket extends ModPacket
{

    public static final Identifier                                        ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "update_block_entity");
    public static final CustomPacketPayload.Type<UpdateBlockEntityPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private BlockPos blockPos;
    private byte[] data;

    public UpdateBlockEntityPacket(final INetworkUpdatableEntity<?> blockEntity)
    {
        this.blockPos = blockEntity.blockPos();
        this.data = writeBlockEntity(blockEntity);
    }

    private static <T> byte[] writeBlockEntity(INetworkUpdatableEntity<T> blockEntity) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), blockEntity.registryAccess());
        final StreamCodec<RegistryFriendlyByteBuf, T> codec = blockEntity.streamCodec();

        codec.encode(buf, blockEntity.payload());

        byte[] data = buf.array();
        buf.release();
        return data;
    }

    public UpdateBlockEntityPacket(final RegistryFriendlyByteBuf buffer)
    {
        readPayload(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(blockPos);
        buffer.writeByteArray(data);
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.blockPos = buffer.readBlockPos();
        this.data = buffer.readByteArray();
    }

    @Override
    public void client()
    {
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        DistExecutor.unsafeRunWhenOn(CLIENT, () -> () -> ClientPacketHandlers.handleChiseledBlockUpdated(blockPos, buf));
        buf.release();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
