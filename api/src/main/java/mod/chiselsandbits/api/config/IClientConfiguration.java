package mod.chiselsandbits.api.config;

import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * All client configuration values that are relevant for the client side of C{@literal &}B.
 *
 * These options are only available on the client, and are not synced between
 * the server and the client.
 */
public interface IClientConfiguration
{
    /**
     * The client configuration.
     * Elements in this configuration are only relevant for the client side of C{@literal &}B.
     * This configuration does not need to be in-sync with the server values.
     *
     * @return The client configuration.
     */
    static IClientConfiguration getInstance() {
        return IChiselsAndBitsConfiguration.getInstance().getClient();
    }

    /**
     * Indicates if currently the bit bag fullness indication via the damage bar of the stack
     * shows fullness or emptiness.
     *
     * @return A configuration supplier that indicates the current bit bag fullness indication system.
     */
    Supplier<Boolean> getInvertBitBagFullness();

    /**
     * Determines the render mode used for successful pattern or chiseled block placement.
     *
     * @return A configuration supplier that indicates the current successful placement render mode.
     */
    Supplier<PlacementPreviewRenderMode> getSuccessfulPlacementRenderMode();

    /**
     * Determines the render mode used for successful pattern or chiseled block placement.
     *
     * @return A configuration supplier that indicates the current successful placement render mode.
     */
    Supplier<PlacementPreviewRenderMode> getFailedPlacementRenderMode();

    /**
     * Determines the color used for successful pattern or chiseled block placement.
     *
     * @return A configuration supplier that indicates the current successful placement color.
     */
    Supplier<Vector4f> getSuccessfulPlacementColor();

    /**
     * Determines the color used for pattern placement which does not fit on the targeted position.
     *
     * @return A configuration supplier that indicates the current not fitting placement color.
     */
    Supplier<Vector4f> getNotFittingPatternPlacementColor();

    /**
     * Determines the color which indicates that the player is either missing bits, or does not have
     * enough space to pick up the bits.
     *
     * @return A configuration supplier that indicates the current missing bits or insufficient space placement color.
     */
    Supplier<Vector4f> getMissingBitsOrSpacePatternPlacementColor();

    /**
     * Provides the RGBA color channels for the color of the chiseling outline.
     *
     * @return A configuration supplier that indicates the current chiseling outline color.
     */
    Supplier<List<? extends Float>> getPreviewChiselingColor();

    /**
     * Provides the RGBA color channels for the color of the placement outline.
     *
     * @return A configuration supplier that indicates the current placement outline color.
     */
    Supplier<List<? extends Float>> getPreviewPlacementColor();

    /**
     * Indicates which preview renderer should be used.
     * Returns the id of the preview renderer.
     *
     * @return A configuration supplier that indicates the current preview renderer.
     */
    Supplier<String> getPreviewRenderer();

    /**
     * Indicates which tool mode renderer should be used.
     * Returns the id of the tool mode renderer.
     *
     * @return A configuration supplier that indicates the current tool mode renderer.
     */
    Supplier<String> getToolModeRenderer();

    /**
     * Indicates if the block picking behaviour of chiseled blocks needs to be inverted.
     * By default, a bit is picked from a chiseled block, if this is true however
     * then the full block is picked and shift needs to be held to pick the bit.
     *
     * @return A configuration supplier which indicates if the block picking behaviour of chiseled blocks needs to be inverted.
     */
    Supplier<Boolean> getInvertPickBlockBehaviour();

    /**
     * Indicates the size of the bit storage content model cache size.
     *
     * @return A configuration supplier that indicates the current bit storage content cache size.
     */
    Supplier<Long> getBitStorageContentCacheSize();

    /**
     * Indicates the size of the printer content model cache size.
     *
     * @return A configuration supplier that indicates the current printer content cache size.
     */
    Supplier<Long> getPrinterContentCacheSize();

    /**
     * Indicates if the lightmap values of a face should be used to extract lighting information for a model.
     *
     * @return A configuration supplier that indicates if the lightmap values of a face should be used to extract lighting information for a model.
     */
    Supplier<Boolean> getEnableFaceLightmapExtraction();

    /**
     * Indicates if the light emission values of a block should be used to extract lighting information for a model.
     *
     * @return A configuration supplier that indicates if the light emission values of a block should be used to extract lighting information for a model.
     */
    Supplier<Boolean> getUseGetLightValue();

    /**
     * Indicates if a mouse indicator should be shown when the mouse is in a selection area in the radial menu.
     *
     * @return A configuration supplier which indicates if a mouse indicator should be rendered in the radial menu.
     */
    Supplier<Boolean> getEnableMouseIndicatorInRadialMenu();

    /**
     * Indicates how many block models should be kept in the model cache.
     *
     * @return A configuration supplier which indicates how many block models should be kept in the model cache.
     */
    Supplier<Long> getModelCacheSize();

    /**
     * Indicates how many faces should be kept in the face layer cache.
     *
     * @return A configuration supplier which indicates how many faces should be kept in the face layer cache.
     */
    Supplier<Long> getFaceLayerCacheSize();

    /**
     * Indicates how many threads should be used during building of the model cache.
     *
     * @return A configuration supplier which indicates how many threads should be used.
     */
    Supplier<Integer> getModelBuildingThreadCount();

    /**
     * Indicates the amount of itemstacks that can be stored in the clipboard.
     *
     * @return A configuration supplier which indicates the amount of itemstacks that can be stored in the clipboard.
     */
    Supplier<Integer> getClipboardSize();

    /**
     * Indicates if a broken chiseled block should be added to the clipboard.
     *
     * @return A configuration supplier which indicates if a broken chiseled block should be added to the clipboard.
     */
    Supplier<Boolean> getShouldBrokenBlocksBeAddedToClipboard();

    /**
     * Indicates if a picked chiseled block should be added to the clipboard.
     *
     * @return A configuration supplier which indicates if a picked chiseled block should be added to the clipboard.
     */
    Supplier<Boolean> getShouldPickedBlocksBeAddedToClipboard();

    /**
     * Indicates the path where chisels and bits will export and import patterns from.
     *
     * @return A configuration supplier which indicates the path where chisels and bits will export and import patterns from.
     */
    Supplier<String> getPatternExportPath();

    /**
     * Indicates the size of the stack model cache.
     *
     * @return A configuration supplier which indicates the size of the stack model cache.
     */
    Supplier<Long> getStackModelCacheSize();

    /**
     * Indicates the size of the cull testing cache.
     *
     * @return The size of the cull testing cache.
     */
    Supplier<Long> getCullTestingCacheSize();

    /**
     * Determines the color used during debug rendering of the mutator area when chiseling.
     *
     * @return A configuration supplier which indicates the color used during debug rendering of the mutator area when chiseling.
     */
    Supplier<List<? extends Float>> getMutatorPreviewChiselingColor();

    /**
     * Determines the color used during debug rendering of the mutator area when placing.
     *
     * @return A configuration supplier which indicates the color used during debug rendering of the mutator area when placing.
     */
    Supplier<List<? extends Float>> getMutatorPreviewPlacementColor();

    /**
     * Indicates if the preview rendering should run in debug mode.
     *
     * @return A configuration supplier which indicates if the preview rendering should run in debug mode.
     */
    Supplier<Boolean> getMutatorPreviewDebug();

    /**
     * Indicates if an error notification should be shown when the player attempts to chisel during a cool down or not.
     *
     * @return A configuration supplier which indicates if an error notification should be shown when the player attempts to chisel during a cool down or not.
     */
    Supplier<Boolean> getShowCoolDownError();

    /**
     * Indicates if a scroll interactions should change the mode of a held item.
     *
     * @return A configuration supplier which indicates if a scroll interactions should change the mode of a held item.
     */
    Supplier<Boolean> getShouldScrollInteractionsChangeMode();
}
