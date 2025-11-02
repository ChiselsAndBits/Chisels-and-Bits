package mod.chiselsandbits.network.packets;

import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.network.handlers.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NeighborBlockUpdatedPacket extends ModPacket
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "neighbor_block_updated");
    public static final CustomPacketPayload.Type<NeighborBlockUpdatedPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private BlockPos toUpdate = BlockPos.ZERO;
    private Block    neighborBlock = Blocks.AIR;
    private @Nullable Orientation orientation = null;

    public NeighborBlockUpdatedPacket(final BlockPos toUpdate, final Block neighborBlock, @Nullable final Orientation orientation)
    {
        this.toUpdate = toUpdate;
        this.neighborBlock = neighborBlock;
        this.orientation = orientation;
    }

    public NeighborBlockUpdatedPacket(RegistryFriendlyByteBuf buffer)
    {
        super();
        readPayload(buffer);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(this.toUpdate);
        buffer.writeById(buffer.registryAccess().lookupOrThrow(Registries.BLOCK)::getId, this.neighborBlock);
        buffer.writeNullable(
            this.orientation,
            Orientation.STREAM_CODEC
        );
    }

    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.toUpdate = buffer.readBlockPos();
        this.neighborBlock = buffer.readById(buffer.registryAccess().lookupOrThrow(Registries.BLOCK)::byIdOrThrow);
        this.orientation = buffer.readNullable(Orientation.STREAM_CODEC);
    }

    @Override
    public void client()
    {
        ClientPacketHandlers.handleNeighborUpdated(toUpdate, neighborBlock, orientation);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
