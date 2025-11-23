package mod.chiselsandbits.config;

import com.communi.suggestu.scena.core.config.ConfigurationType;
import com.communi.suggestu.scena.core.config.IConfigurationBuilder;
import com.communi.suggestu.scena.core.config.IConfigurationManager;
import com.google.common.collect.Lists;
import mod.chiselsandbits.api.client.render.preview.placement.PlacementPreviewRenderMode;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.util.constants.Constants;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Supplier;

public class ClientConfiguration implements IClientConfiguration
{

    private final Supplier<Boolean>                    bitBagFullness;
    private final Supplier<PlacementPreviewRenderMode> successfulPlacementRenderMode;
    private final Supplier<PlacementPreviewRenderMode> failedPlacementRenderMode;
    private final Supplier<Vector4f>                   successfulPlacementColor;
    private final Supplier<Vector4f>                   notFittingPatternPlacementColor;
    private final Supplier<Vector4f>                   missingBitsOrSpacePatternPlacementColor;
    private final Supplier<List<? extends Float>>      previewChiselingColor;
    private final Supplier<List<? extends Float>>      previewPlacementColor;
    private final Supplier<List<? extends Float>>      mutatorPreviewChiselingColor;
    private final Supplier<List<? extends Float>>      mutatorPreviewPlacementColor;
    private final Supplier<Boolean>                    mutatorPreviewDebug;
    private final Supplier<String>                     previewRenderer;
    private final Supplier<String>                     toolModeRenderer;
    private final Supplier<Boolean>                    invertPickBlockBehaviour;
    private final Supplier<Long>                       bitStorageContentCacheSize;
    private final Supplier<Long>                       printerContentCacheSize;
    private final Supplier<Boolean>                    faceLightMapExtraction;
    private final Supplier<Boolean>                    blockLightEmissionExtraction;
    private final Supplier<Boolean>                    radialMenuMouseIndicator;
    private final Supplier<Long>                       modelCacheSize;
    private final Supplier<Long>                       stackModelCacheSize;
    private final Supplier<Long>                       faceLayerCacheSize;
    private final Supplier<Integer>                    modelBuildingThreadCount;
    private final Supplier<Integer>                    clipboardSize;
    private final Supplier<Boolean>                    addBrokenBlocksToClipboard;
    private final Supplier<Boolean>                    addPickedBlocksToClipboard;
    private final Supplier<String>                     patternExportPath;
    private final Supplier<Boolean> showCoolDownError;
    private final Supplier<Long> cullTestingCacheSize;
    private final Supplier<Boolean> shouldScrollInteractionsChangeMode;

    public ClientConfiguration()
    {
        final IConfigurationBuilder builder = IConfigurationManager.getInstance().createBuilder(
                ConfigurationType.CLIENT_ONLY, Constants.MOD_ID + "-client", "mod.chiselsandbits.config");

        bitBagFullness = builder.defineBoolean("settings.bit-bag.invert-durability-bar-indication", false);
        successfulPlacementRenderMode = builder.defineEnum("settings.placement.render-mode.success", PlacementPreviewRenderMode.GHOST_BLOCK_MODEL);
        failedPlacementRenderMode = builder.defineEnum("settings.placement.render-mode.failed", PlacementPreviewRenderMode.GHOST_BLOCK_MODEL_SOLID_COLOR);
        successfulPlacementColor = builder.defineVector4f("settings.placement.color.success", new Vector4f(48/255f, 120/255f, 201/255f, 180/255f));
        notFittingPatternPlacementColor = builder.defineVector4f("settings.placement.color.not-fitting", new Vector4f(183/255f, 65/255f, 14/255f, 180/255f));
        missingBitsOrSpacePatternPlacementColor = builder.defineVector4f("settings.placement.color.missing-bits-or-space", new Vector4f(255/255f, 219/255f, 88/255f, 180/255f));
        previewChiselingColor = builder.defineList("settings.chiseling-previews.default.colors.chiseling", Lists.newArrayList(0.85f, 0.0f, 0.0f, 0.65f), Float.class);
        previewPlacementColor = builder.defineList("settings.chiseling-previews.default.colors.placement", Lists.newArrayList(0.0f, 0.85f, 0.0f, 0.65f), Float.class);
        previewRenderer = builder.defineString("settings.chiseling-previews.renderer", Constants.MOD_ID + ":default");
        mutatorPreviewDebug = builder.defineBoolean("settings.chiseling-previews.debug.enabled", false);
        mutatorPreviewChiselingColor = builder.defineList("settings.chiseling-previews.debug.mutator.colors.chiseling", Lists.newArrayList(0.0f, 0.0f, 0.85f, 0.65f), Float.class);
        mutatorPreviewPlacementColor = builder.defineList("settings.chiseling-previews.debug.mutator.colors.placement", Lists.newArrayList(0.85f, 0.85f, 0.0f, 0.65f), Float.class);
        toolModeRenderer = builder.defineString("settings.selected-tool-mode-icons.renderer", Constants.MOD_ID + ":group");
        invertPickBlockBehaviour = builder.defineBoolean("settings.invert-pick-block-behaviour", false);
        clipboardSize = builder.defineInteger("settings.clipboard.size", 64, 0, 64);
        addBrokenBlocksToClipboard = builder.defineBoolean("settings.clipboard.add-broken-blocks", true);
        addPickedBlocksToClipboard = builder.defineBoolean("settings.clipboard.add-picked-blocks", true);
        patternExportPath = builder.defineString("settings.patterns.export-path", "./chiselsandbits/patterns");
        bitStorageContentCacheSize = builder.defineLong("performance.caches.sizes.bit-storage-content-models", 100, 0, Long.MAX_VALUE);
        printerContentCacheSize = builder.defineLong("performance.caches.sizes.printer-content-models", 100, 0, Long.MAX_VALUE);
        faceLightMapExtraction = builder.defineBoolean("performance.lighting.extract-lighting-values-from-faces", true);
        blockLightEmissionExtraction = builder.defineBoolean("performance.lighting.extract-lighting-values-from-blockstates", false);
        radialMenuMouseIndicator = builder.defineBoolean("gui.radial-menu.display-mouse-indicator", false);
        modelCacheSize = builder.defineLong("performance.caches.sizes.block-models", 1000, 3500, 20000);
        faceLayerCacheSize = builder.defineLong("performance.caches.sizes.block-faces", 1000000, 350000, 2000000);
        cullTestingCacheSize = builder.defineLong("performance.caches.sizes.cull-testing", 1000, 3500, 200000);
        modelBuildingThreadCount = builder.defineInteger("performance.model-building.thread-count", Math.max(1, Runtime.getRuntime().availableProcessors()) / 2, 1, Runtime.getRuntime()
          .availableProcessors());
        stackModelCacheSize = builder.defineLong("performance.caches.sizes.stack-models", 100, 0, Long.MAX_VALUE);
        showCoolDownError = builder.defineBoolean("settings.warnings.show-cool-down-error", false);
        shouldScrollInteractionsChangeMode = builder.defineBoolean("settings.interactions.scrolling-changes-mode", true);

        builder.setup();
    }

    @Override
    public Supplier<Boolean> getInvertBitBagFullness()
    {
        return bitBagFullness;
    }

    @Override
    public Supplier<PlacementPreviewRenderMode> getSuccessfulPlacementRenderMode()
    {
        return successfulPlacementRenderMode;
    }

    @Override
    public Supplier<PlacementPreviewRenderMode> getFailedPlacementRenderMode()
    {
        return failedPlacementRenderMode;
    }

    @Override
    public Supplier<Vector4f> getSuccessfulPlacementColor()
    {
        return successfulPlacementColor;
    }

    @Override
    public Supplier<Vector4f> getNotFittingPatternPlacementColor()
    {
        return notFittingPatternPlacementColor;
    }

    @Override
    public Supplier<Vector4f> getMissingBitsOrSpacePatternPlacementColor()
    {
        return missingBitsOrSpacePatternPlacementColor;
    }

    @Override
    public Supplier<List<? extends Float>> getPreviewChiselingColor()
    {
        return previewChiselingColor;
    }

    @Override
    public Supplier<List<? extends Float>> getPreviewPlacementColor()
    {
        return previewPlacementColor;
    }

    @Override
    public Supplier<String> getPreviewRenderer()
    {
        return previewRenderer;
    }

    @Override
    public Supplier<String> getToolModeRenderer()
    {
        return toolModeRenderer;
    }

    @Override
    public Supplier<Boolean> getInvertPickBlockBehaviour()
    {
        return invertPickBlockBehaviour;
    }

    @Override
    public Supplier<Long> getBitStorageContentCacheSize()
    {
        return bitStorageContentCacheSize;
    }

    @Override
    public Supplier<Long> getPrinterContentCacheSize()
    {
        return printerContentCacheSize;
    }

    @Override
    public Supplier<Boolean> getEnableFaceLightmapExtraction()
    {
        return faceLightMapExtraction;
    }

    @Override
    public Supplier<Boolean> getUseGetLightValue()
    {
        return blockLightEmissionExtraction;
    }

    @Override
    public Supplier<Boolean> getEnableMouseIndicatorInRadialMenu()
    {
        return radialMenuMouseIndicator;
    }

    @Override
    public Supplier<Long> getModelCacheSize()
    {
        return modelCacheSize;
    }

    @Override
    public Supplier<Long> getFaceLayerCacheSize()
    {
        return faceLayerCacheSize;
    }

    @Override
    public Supplier<Integer> getModelBuildingThreadCount()
    {
        return modelBuildingThreadCount;
    }

    @Override
    public Supplier<Integer> getClipboardSize()
    {
        return clipboardSize;
    }

    @Override
    public Supplier<Boolean> getShouldBrokenBlocksBeAddedToClipboard()
    {
        return addBrokenBlocksToClipboard;
    }

    @Override
    public Supplier<Boolean> getShouldPickedBlocksBeAddedToClipboard()
    {
        return addPickedBlocksToClipboard;
    }

    @Override
    public Supplier<String> getPatternExportPath()
    {
        return patternExportPath;
    }

    @Override
    public Supplier<Long> getStackModelCacheSize()
    {
        return stackModelCacheSize;
    }

    @Override
    public Supplier<Long> getCullTestingCacheSize() {
        return cullTestingCacheSize;
    }

    @Override
    public Supplier<List<? extends Float>> getMutatorPreviewChiselingColor()
    {
        return mutatorPreviewChiselingColor;
    }

    @Override
    public Supplier<List<? extends Float>> getMutatorPreviewPlacementColor()
    {
        return mutatorPreviewPlacementColor;
    }

    @Override
    public Supplier<Boolean> getMutatorPreviewDebug()
    {
        return mutatorPreviewDebug;
    }

    @Override
    public Supplier<Boolean> getShowCoolDownError() {
        return showCoolDownError;
    }

    @Override
    public Supplier<Boolean> getShouldScrollInteractionsChangeMode() {
        return shouldScrollInteractionsChangeMode;
    }
}
