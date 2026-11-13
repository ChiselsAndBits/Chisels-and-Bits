package mod.chiselsandbits.forge;

import java.util.function.Consumer;

import com.communi.suggestu.scena.core.init.PlatformInitializationHandler;
import com.mojang.logging.LogUtils;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.client.ChiselsAndBitsClient;
import mod.chiselsandbits.client.model.block.ChiseledModelDataAccess;
import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataExecutor;
import com.communi.suggestu.scena.forge.platform.client.model.data.ForgeBlockModelDataPlatformDelegate;
import com.communi.suggestu.scena.core.entity.block.IBlockEntityWithModelData;
import mod.chiselsandbits.forge.gametest.ChiselsAndBitsGameTests;
import mod.chiselsandbits.forge.gametest.TerrainWorldClientCheck;
import mod.chiselsandbits.forge.platform.ForgeAdaptingBitInventoryManager;
import mod.chiselsandbits.forge.platform.ForgePluginDiscoverer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import org.slf4j.Logger;

@Mod(Constants.MOD_ID)
public class Forge {
	private static final Logger LOGGER = LogUtils.getLogger();

	private ChiselsAndBits chiselsAndBits;

	private void setChiselsAndBits(final ChiselsAndBits chiselsAndBits) {
		this.chiselsAndBits = chiselsAndBits;
	}

	public Forge(IEventBus modBus) {
		LOGGER.info("Initialized Chisels&Bits - Forge");
		//We need to use the platform initialization manager to handle the init in the constructor since this runs in parallel with scena itself.
		PlatformInitializationHandler.getInstance().onInit(platform -> {
			setChiselsAndBits(new ChiselsAndBits(
					block -> block == IBlockExtension.class,
					ForgeAdaptingBitInventoryManager.getInstance(),
					ForgePluginDiscoverer.getInstance()
			));
		});

		modBus.addListener((Consumer<FMLCommonSetupEvent>) event -> chiselsAndBits.onInitialize());
		ChiselsAndBitsGameTests.initialize(modBus);
	}

	@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
	public static final class Client {
		private static final ClassValue<Boolean> HAS_SNAPSHOT_MODEL_DATA = new ClassValue<>() {
			@Override
			protected Boolean computeValue(final Class<?> type) {
				try {
					return type.getMethod("getModelData", net.minecraft.core.BlockPos.class).getDeclaringClass()
							!= net.neoforged.neoforge.common.extensions.IBlockGetterExtension.class;
				} catch (final NoSuchMethodException exception) {
					throw new IllegalStateException("Block getter does not expose its required model-data method", exception);
				}
			}
		};

		public Client(ModContainer container) {
			LOGGER.info("Initialized Chisels&Bits-Forge client");
			if (Boolean.getBoolean("chiselsandbits.runTerrainWorldCheck")) {
				NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> TerrainWorldClientCheck.onClientTick());
			}
			NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
				if (event.getLevel() instanceof ClientLevel level) {
					ChiseledBlockModelDataExecutor.onChunkTransition(level, event.getChunk().getPos());
				}
			});
			NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload event) -> {
				if (event.getLevel() instanceof ClientLevel level) {
					ChiseledBlockModelDataExecutor.onChunkTransition(level, event.getChunk().getPos());
				}
			});
			ChiseledModelDataAccess.install((level, pos) -> {
				if (level instanceof net.minecraft.client.multiplayer.ClientLevel
						|| level instanceof com.communi.suggestu.scena.core.util.SingleBlockBlockAndTintGetter
						|| !HAS_SNAPSHOT_MODEL_DATA.get(level.getClass())) {
					return level.getBlockEntity(pos) instanceof IBlockEntityWithModelData entity
							? entity.getBlockModelData() : null;
				}
				return new ForgeBlockModelDataPlatformDelegate(level.getModelData(pos));
			});
			//We need to use the platform initialization manager to handle the init in the constructor since this runs in parallel with scena itself.
			PlatformInitializationHandler.getInstance().onInit(platform -> {
				setChiselsAndBitsClient(new ChiselsAndBitsClient());
			});

			container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));
		}

		private static ChiselsAndBitsClient chiselsAndBitsClient;

		public static void setChiselsAndBitsClient(final ChiselsAndBitsClient chiselsAndBitsClient) {
			Client.chiselsAndBitsClient = chiselsAndBitsClient;
		}
	}
}
