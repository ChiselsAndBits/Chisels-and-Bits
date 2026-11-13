package mod.chiselsandbits.fabric.client;

import mod.chiselsandbits.client.ChiselsAndBitsClient;
import mod.chiselsandbits.client.model.block.ChiseledModelDataAccess;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataExecutor;
import com.communi.suggestu.scena.core.client.models.data.IBlockModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;

public class FabricClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("chisels-and-bits-fabric");

	private ChiselsAndBitsClient chiselsAndBits;

	public FabricClient() {
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initialized chisels-and-bits for Fabric client module");
		ChiseledModelDataAccess.install((level, pos) ->
				level.getBlockEntityRenderData(pos) instanceof IBlockModelData data ? data : null);
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
				ChiseledBlockModelDataExecutor.onChunkTransition(level, chunk.getPos()));
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
				ChiseledBlockModelDataExecutor.onChunkTransition(level, chunk.getPos()));
		setChiselsAndBits(new ChiselsAndBitsClient());
	}

	public void setChiselsAndBits(final ChiselsAndBitsClient chiselsAndBits) {
		this.chiselsAndBits = chiselsAndBits;
	}
}
