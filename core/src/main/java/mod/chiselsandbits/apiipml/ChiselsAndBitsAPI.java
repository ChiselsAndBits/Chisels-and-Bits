package mod.chiselsandbits.apiipml;

import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.IChiselsAndBitsAPI;
import mod.chiselsandbits.api.block.state.id.IBlockStateIdManager;
import mod.chiselsandbits.api.change.IChangeTrackerManager;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.chiseling.ILocalChiselingContextCache;
import mod.chiselsandbits.api.chiseling.conversion.IConversionManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityOptions;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.client.clipboard.ICreativeClipboardManager;
import mod.chiselsandbits.api.client.highlight.IItemBasedHighlightRendererRegistry;
import mod.chiselsandbits.api.client.icon.IIconManager;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.api.client.sharing.IPatternSharingManager;
import mod.chiselsandbits.api.client.tool.mode.icon.ISelectedToolModeIconRendererRegistry;
import mod.chiselsandbits.api.client.variant.state.IClientStateVariantManager;
import mod.chiselsandbits.api.config.IChiselsAndBitsConfiguration;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperation;
import mod.chiselsandbits.api.glueing.operation.IGlueingOperation;
import mod.chiselsandbits.api.inventory.bit.IAdaptingBitInventoryManager;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemFactory;
import mod.chiselsandbits.api.launch.ILaunchPropertyManager;
import mod.chiselsandbits.api.measuring.IMeasuringManager;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.multistate.accessor.IAccessorFactory;
import mod.chiselsandbits.api.multistate.mutator.IMutatorFactory;
import mod.chiselsandbits.api.multistate.snapshot.ISnapshotFactory;
import mod.chiselsandbits.api.neighborhood.IBlockNeighborhoodBuilder;
import mod.chiselsandbits.api.notifications.INotificationManager;
import mod.chiselsandbits.api.permissions.IPermissionHandler;
import mod.chiselsandbits.api.plugin.IPluginDiscoverer;
import mod.chiselsandbits.api.plugin.IPluginManager;
import mod.chiselsandbits.api.profiling.IProfilingManager;
import mod.chiselsandbits.api.registries.IRegistryManager;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.api.voxelshape.IVoxelShapeManager;
import mod.chiselsandbits.change.ChangeTrackerManger;
import mod.chiselsandbits.chiseling.ChiselingManager;
import mod.chiselsandbits.chiseling.LocalChiselingContextCache;
import mod.chiselsandbits.chiseling.conversion.ConversionManager;
import mod.chiselsandbits.chiseling.eligibility.EligibilityManager;
import mod.chiselsandbits.client.chiseling.preview.render.ChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.client.clipboard.CreativeClipboardManager;
import mod.chiselsandbits.client.highlight.ItemBasedHighlightRendererRegistry;
import mod.chiselsandbits.client.icon.IconManager;
import mod.chiselsandbits.client.sharing.PatternSharingManager;
import mod.chiselsandbits.client.tool.mode.icon.SelectedToolModeRendererRegistry;
import mod.chiselsandbits.client.variant.state.ClientStateVariantManager;
import mod.chiselsandbits.inventory.management.BitInventoryManager;
import mod.chiselsandbits.item.bit.BitItemManager;
import mod.chiselsandbits.item.multistate.MultiStateItemFactory;
import mod.chiselsandbits.launch.LaunchPropertyManager;
import mod.chiselsandbits.measures.MeasuringManager;
import mod.chiselsandbits.multistate.mutator.MutatorFactory;
import mod.chiselsandbits.multistate.snapshot.SnapshotFactory;
import mod.chiselsandbits.neighborhood.BlockNeighborhoodBuilder;
import mod.chiselsandbits.notifications.NotificationManager;
import mod.chiselsandbits.permissions.PermissionHandler;
import mod.chiselsandbits.plugin.PluginManger;
import mod.chiselsandbits.profiling.ProfilingManager;
import mod.chiselsandbits.registrars.ModChiselModes;
import mod.chiselsandbits.registrars.ModModificationOperation;
import mod.chiselsandbits.registrars.ModTags;
import mod.chiselsandbits.registries.RegistryManager;
import mod.chiselsandbits.stateinfo.additional.StateVariantManager;
import mod.chiselsandbits.voxelshape.VoxelShapeManager;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("DataFlowIssue")
public record ChiselsAndBitsAPI(IEligibilityOptions eligibilityOptions,
								IAdaptingBitInventoryManager adaptingBitInventoryManager,
								IPluginDiscoverer pluginDiscoverer) implements IChiselsAndBitsAPI {

	/**
	 * Gives access to the factory that can produce different accessors.
	 *
	 * @return The factory used to create new accessors.
	 */
	@Override
	public @NonNull IAccessorFactory getAccessorFactory() {
		return MutatorFactory.getInstance();
	}

	/**
	 * Gives access to the factory that can produce different mutators.
	 *
	 * @return The factory used to create new mutators.
	 */
	@Override
	public @NonNull IMutatorFactory getMutatorFactory() {
		return MutatorFactory.getInstance();
	}

	/**
	 * Manager which deals with chiseling eligibility.
	 *
	 * @return The manager.
	 */
	@Override
	public @NonNull IEligibilityManager getEligibilityManager() {
		return EligibilityManager.getInstance();
	}

	/**
	 * Manager which deals with converting eligible blocks, blockstates and IItemProviders into their chiseled variants.
	 *
	 * @return The conversion manager.
	 */
	@Override
	public @NonNull IConversionManager getConversionManager() {
		return ConversionManager.getInstance();
	}

	/**
	 * Manager which deals with calculating, and optionally caching, the voxel shapes, which can be constructed from a given area.
	 *
	 * @return The voxel shape manager.
	 */
	@Override
	public @NonNull IVoxelShapeManager getVoxelShapeManager() {
		return VoxelShapeManager.getInstance();
	}

	/**
	 * A factory which can produce a multistate item from a given source.
	 *
	 * @return The factory.
	 */
	@Override
	public @NonNull IMultiStateItemFactory getMultiStateItemFactory() {
		return MultiStateItemFactory.getInstance();
	}

	/**
	 * Represents the default mode for the chiseling system.
	 *
	 * @return The default mode.
	 */
	@Override
	public @NonNull IChiselMode getDefaultChiselMode() {
		return ModChiselModes.SINGLE_BIT.get();
	}

	/**
	 * Gives access to all registries which are used by chisels and bits.
	 *
	 * @return The manager for registries used by chisels and bits.
	 */
	@Override
	public @NonNull IRegistryManager getRegistryManager() {
		return RegistryManager.getInstance();
	}

	/**
	 * Gives access to the manager which controls chiseling operations.
	 *
	 * @return The current chiseling manager.
	 */
	@Override
	public @NonNull IChiselingManager getChiselingManager() {
		return ChiselingManager.getInstance();
	}

	/**
	 * The configuration on top of which chisels and bits is running.
	 *
	 * @return The current configuration.
	 */
	@Override
	public @NonNull IChiselsAndBitsConfiguration getConfiguration() {
		return ChiselsAndBits.getInstance().getConfiguration();
	}

	/**
	 * The manager which deals with calculating the given blockstate ids in the current running session.
	 *
	 * @return The blockstate id manager.
	 */
	@Override
	public @NonNull IBlockStateIdManager getBlockStateIdManager() {
		return new IBlockStateIdManager() {
		};
	}

	@Override
	public @NonNull IBitInventoryManager getBitInventoryManager() {
		return BitInventoryManager.getInstance();
	}

	@Override
	public @NonNull IBitItemManager getBitItemManager() {
		return BitItemManager.getInstance();
	}

	@Override
	public @NonNull IMeasuringManager getMeasuringManager() {
		return MeasuringManager.getInstance();
	}

	@Override
	public @NonNull IProfilingManager getProfilingManager() {
		return ProfilingManager.getInstance();
	}

	@Override
	public @NonNull ILocalChiselingContextCache getLocalChiselingContextCache() {
		return LocalChiselingContextCache.getInstance();
	}

	@Override
	public @NonNull IChangeTrackerManager getChangeTrackerManager() {
		return ChangeTrackerManger.getInstance();
	}

	@Override
	public @NonNull IBlockNeighborhoodBuilder getBlockNeighborhoodBuilder() {
		return BlockNeighborhoodBuilder.getInstance();
	}

	@Override
	public @NonNull IModificationOperation getDefaultModificationOperation() {
		return ModModificationOperation.ROTATE_AROUND_X.get();
	}

	@Override
	public @NonNull IPluginManager getPluginManager() {
		return PluginManger.getInstance();
	}

	@Override
	public @NonNull IChiselContextPreviewRendererRegistry getChiselContextPreviewRendererRegistry() {
		return ChiselContextPreviewRendererRegistry.getInstance();
	}

	@Override
	public @NonNull ISelectedToolModeIconRendererRegistry getSelectedToolModeIconRenderer() {
		return SelectedToolModeRendererRegistry.getInstance();
	}

	@Override
	public @NonNull TagKey<Block> getForcedTag() {
		return ModTags.Blocks.FORCED_CHISELABLE;
	}

	@Override
	public @NonNull TagKey<Block> getBlockedTag() {
		return ModTags.Blocks.BLOCKED_CHISELABLE;
	}

	@Override
	public @NonNull IPermissionHandler getPermissionHandler() {
		return PermissionHandler.getInstance();
	}

	@Override
	public @NonNull ICreativeClipboardManager getCreativeClipboardManager() {
		return CreativeClipboardManager.getInstance();
	}

	@Override
	public @NonNull IPatternSharingManager getPatternSharingManager() {
		return PatternSharingManager.getInstance();
	}

	@Override
	public @NonNull INotificationManager getNotificationManager() {
		return NotificationManager.getInstance();
	}

	@Override
	public @NonNull IStateVariantManager getStateVariantManager() {
		return StateVariantManager.getInstance();
	}

	@Override
	public @NonNull ICuttingOperation getDefaultCuttingOperation() {
		return null;
	}

	@Override
	public @NonNull IGlueingOperation getDefaultGlueingOperation() {
		return null;
	}

	@Override
	public @NonNull ISnapshotFactory getSnapshotFactory() {
		return SnapshotFactory.getInstance();
	}


	@Override
	public @NonNull IClientStateVariantManager getClientStateVariantManager() {
		return DistExecutor.unsafeRunForDist(
				() -> ClientStateVariantManager::getInstance,
				() -> () -> null
		);
	}

	@Override
	public @NonNull ILaunchPropertyManager getLaunchPropertyManager() {
		return LaunchPropertyManager.getInstance();
	}

	@Override
	public @NonNull IIconManager getIconManager() {
		return DistExecutor.unsafeRunForDist(
				() -> IconManager::getInstance,
				() -> () -> null
		);
	}

	@Override
	public @NonNull IItemBasedHighlightRendererRegistry getItemBasedHighlightRendererRegistry() {
		return ItemBasedHighlightRendererRegistry.getInstance();
	}
}
