package mod.chiselsandbits.api.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum LocalStrings
{

    ChiselModeSingle("chiselmode.single"),
    ChiselModeSnap2("chiselmode.snap2"),
    ChiselModeSnap4("chiselmode.snap4"),
    ChiselModeSnap8("chiselmode.snap8"),
    ChiselModeLine("chiselmode.line"),
    ChiselModeLine2("chiselmode.line2"),
    ChiselModeLine4("chiselmode.line4"),
    ChiselModeLine8("chiselmode.line8"),
    ChiselModePlane("chiselmode.plane"),
    ChiselModePlane2("chiselmode.plane2"),
    ChiselModePlane4("chiselmode.plane4"),
    ChiselModePlane8("chiselmode.plane8"),
    ChiselModeConnectedPlane("chiselmode.connected_plane"),
    ChiselModeConnectedMaterial("chiselmode.connected_material"),
    ChiselModeCubeSmall("chiselmode.cube_small"),
    ChiselModeCubeMedium("chiselmode.cube_medium"),
    ChiselModeCubeLarge("chiselmode.cube_large"),
    ChiselModeCubeFull("chiselmode.cube_full"),
    ChiselModeSameMaterial("chiselmode.same_material"),
    ChiselModeDrawnCube("chiselmode.drawn_cube"),
    ChiselModeDrawnLine("chiselmode.drawn_line"),
    ChiselModeDrawnWallThin("chiselmode.drawn_wall_thin"),
    ChiselModeDrawnWallMedium("chiselmode.drawn_wall_medium"),
    ChiselModeDrawnWallFat("chiselmode.drawn_wall_fat"),
    ChiselModeReplace("chiselmode.replace"),
    ChiselModeSphereSmall("chiselmode.sphere_small"),
    ChiselModeSphereMedium("chiselmode.sphere_medium"),
    ChiselModeSphereLarge("chiselmode.sphere_large"),

    ChiselModeMultiLineSingle("chiselmode.multiline.single"),
    ChiselModeMultiLineSnap2("chiselmode.multiline.snap2"),
    ChiselModeMultiLineSnap4("chiselmode.multiline.snap4"),
    ChiselModeMultiLineSnap8("chiselmode.multiline.snap8"),
    ChiselModeMultiLineLine("chiselmode.multiline.line"),
    ChiselModeMultiLineLine2("chiselmode.multiline.line2"),
    ChiselModeMultiLineLine4("chiselmode.multiline.line4"),
    ChiselModeMultiLineLine8("chiselmode.multiline.line8"),
    ChiselModeMultiLinePlane("chiselmode.multiline.plane"),
    ChiselModeMultiLinePlane2("chiselmode.multiline.plane2"),
    ChiselModeMultiLinePlane4("chiselmode.multiline.plane4"),
    ChiselModeMultiLinePlane8("chiselmode.multiline.plane8"),
    ChiselModeMultiLineConnectedPlane("chiselmode.multiline.connected_plane"),
    ChiselModeMultiLineConnectedMaterial("chiselmode.multiline.connected_material"),
    ChiselModeMultiLineCubeSmall("chiselmode.multiline.cube_small"),
    ChiselModeMultiLineCubeMedium("chiselmode.multiline.cube_medium"),
    ChiselModeMultiLineCubeLarge("chiselmode.multiline.cube_large"),
    ChiselModeMultiLineCubeFull("chiselmode.multiline.cube_full"),
    ChiselModeMultiLineSameMaterial("chiselmode.multiline.same_material"),
    ChiselModeMultiLineDrawnCube("chiselmode.multiline.drawn_cube"),
    ChiselModeMultiLineDrawnLine("chiselmode.multiline.drawn_line"),
    ChiselModeMultiLineDrawnWallThin("chiselmode.multiline.drawn_wall_thin"),
    ChiselModeMultiLineDrawnWallMedium("chiselmode.multiline.drawn_wall_medium"),
    ChiselModeMultiLineDrawnWallFat("chiselmode.multiline.drawn_wall_fat"),
    ChiselModeMultiLineReplace("chiselmode.multiline.replace"),
    ChiselModeMultiLineSphereSmall("chiselmode.multiline.sphere_small"),
    ChiselModeMultiLineSphereMedium("chiselmode.multiline.sphere_medium"),
    ChiselModeMultiLineSphereLarge("chiselmode.multiline.sphere_large"),

    ShiftDetails("help.shiftdetails"),
    Empty("help.empty"),
    Filled("help.filled"),

    HelpChiseledBlock("help.chiseled_block"),
    LongHelpChiseledBlock("help.chiseled_block.long"),

    HelpBitSaw("help.bitsaw"),
    LongHelpBitSaw("help.bitsaw.long"),

    HelpBitBag("help.bit_bag"),
    LongHelpBitBag("help.bit_bag.long"),
    ContainerBitBag("container.bit_bag"),

    HelpWrench("help.wrench"),
    LongHelpWrench("help.wrench.long"),

    HelpBit("help.bit"),
    LongHelpBit("help.bit.long"),
    AnyHelpBit("help.bit.any"),

    HelpBitStorageFilled("help.bittank.full"),
    HelpBitStorageEmpty("help.bittank.empty"),
    LongHelpBitTank("help.bittank.long"),

    HelpSimplePattern("help.pattern.simple"),
    LongHelpSimplePattern("help.pattern.simple.long"),

    HelpSealedPattern("help.pattern.sealed"),
    LongHelpSealedPattern("help.pattern.sealed.long"),

    HelpChisel("help.chisel"),
    LongHelpChisel("help.chisel.long"),

    noBind("help.nobind"),

    leftShift("help.leftshift"),
    rightShift("help.rightshift"),

    leftAlt("help.leftalt"),
    rightAlt("help.rightalt"),

    Sort("help.sort"),
    Trash("help.trash"),
    TrashItem("help.trashitem"),
    ReallyTrash("help.reallytrash"),
    ReallyTrashItem("help.reallytrash_blank"),
    TrashInvalidItem("help.trash.invalid"),
    Convert("help.convert"),

    PositivePatternReplace("positivepatternmode.replace"),
    PositivePatternAdditive("positivepatternmode.additive"),
    PositivePatternPlacement("positivepatternmode.placement"),
    PositivePatternImpose("positivepatternmode.impose"),

    HelpTapeMeasure("help.tape_measure"),
    LongHelpTapeMeasure("help.tape_measure.long"),

    HelpQuill("help.quill"),
    LongHelpQuill("help.quill.long"),

    HelpSealant("help.sealant"),
    LongHelpSealant("help.sealant.long"),

    HelpUnseal("help.unseal"),
    LongHelpUnseal("help.unseal.long"),

    TapeMeasureBit("tapemeasure.bit"),
    TapeMeasureBlock("tapemeasure.block"),
    TapeMeasureDistance("tapemeasure.distance"),

    BitOptionPlace("bitoption.place"),
    BitOptionReplace("bitoption.replace"),

    HelpMagnifyingGlass("help.magnifying_glass"),
    LongHelpMagnifyingGlass("help.magnifying_glass.long"),

    ChiselSupportIsAlreadyChiseled("chisel.support.already-chiseled"),
    ChiselSupportGenericNotSupported("chisel.support.not.supported.generic"),
    ChiselSupportLogicIgnored("chisel.support.supported.code"),
    ChiselSupportGenericSupported("chisel.support.supported.generic"),
    ChiselSupportGenericFluidSupport("chisel.support.supported.fluid"),
    ChiselSupportCompatDeactivated("chisel.support.not.supported.compatibility.deactivated"),
    ChiselSupportCustomCollision("chisel.support.not.supported.collision"),
    ChiselSupportNoHardness("chisel.support.not.supported.hardness"),
    ChiselSupportNotFullBlock("chisel.support.not.supported.shape"),
    ChiselSupportHasBehaviour("chisel.support.not.supported.entity"),
    ChiselSupportIsSlab("chisel.support.not.supported.slab"),
    ChiselSupportHasCustomDrops("chisel.support.not.supported.drops"),
    ChiselSupportFailureToAnalyze("chisel.support.not.supported.failure"),
    ChiselSupportForcedUnsupported("chisel.support.not.supported.forced"),
    ChiselSupportForcedSupported("chisel.support.supported.forced"),
    ChiselSupportTagBlackListed("chisel.support.not.supported.tag"),
    ChiselSupportTagWhitelisted("chisel.support.supported.tag"),

    ChiselStationName("chisel.station.name"),
    ChiselStationHelp("chisel.station.help"),
    LongChiselStationHelp("chisel.station.help.long"),

    ModificationTableHelp("modification.table.help"),
    LongModificationTableHelp("modification.table.help.long"),

    CommandGiveErrorBlockStateNotChiselable("command.give.blockstate.not-chiselable"),
    CommandFillCompleted("command.fill.completed"),

    PatternItemTooltipModeGrouped("pattern.item.tooltip.mode.grouped"),
    PatternItemTooltipModeSimple("pattern.item.tooltip.mode.simple"),

    PatternPlacementModePlacement("pattern.placement.mode.placement"),
    PatternPlacementModeRemoval("pattern.placement.mode.removal"),
    PatternPlacementModeImposement("pattern.placement.mode.imposement"),
    PatternPlacementModeMerge("pattern.placement.mode.merge"),
    PatternPlacementModeCarving("pattern.placement.mode.carving"),

    PatternPlacementCollision("pattern.placement.failure.collision"),
    PatternPlacementNoBitSpace("pattern.placement.failure.no.bit.space"),
    PatternPlacementNotEnoughBits("pattern.placement.failure.no.bits"),
    PatternPlacementNotASolidBlock("pattern.placement.failure.not.a.solid.block"),
    PatternPlacementNotAChiseledBlock("pattern.placement.failure.not.a.chiseled.block"),
    PatternPlacementNotAnAirBlock("pattern.placement.failure.not.a.air.block"),
    PatternPlacementNotASupportedBlock("pattern.placement.failure.not.a.supported.block"),
    PatternModificationGroupMirror("pattern.modification.group.mirror"),
    PatternModificationGroupRotate("pattern.modification.group.rotate"),
    PatternModificationAcrossXAxis("pattern.modification.across.axis.x"),
    PatternModificationAcrossYAxis("pattern.modification.across.axis.y"),
    PatternModificationAcrossZAxis("pattern.modification.across.axis.z"),
    CanNotUndo("change-tracking.undo.failed"),
    UndoSuccessful("change-tracking.undo.success"),
    CanNotRedo("change-tracking.redo.failed"),
    RedoSuccessful("change-tracking.redo.success"),

    ToolMenuPageSelectorName("ui-components.tool-menu.page-selector.name"),
    ToolMenuPreviousPageName("ui-components.tool-menu.page-selector.previous.name"),
    ToolMenuNextPageName("ui-components.tool-menu.page-selector.next.name"),
    ToolMenuGroupSelectorName("ui-components.tool-menu.group-selector.name"),
    ToolMenuModeSelectorName("ui-components.tool-menu.mode-selector.name"),
    ToolMenuSelectorName("ui-components.tool-menu.selector.name"),
    ToolMenuScreenName("ui-screens.tool-menu.name"),
    ChangeTrackerOperations("ui-components.change-tracker.operations.name"),
    ChangeTrackerOperationsButtonUndoName("ui-components.change-tracker.operations.undo.name"),
    ChangeTrackerOperationsButtonRedoName("ui-components.change-tracker.operations.redo.name"),
    ChangeTrackerOperationsButtonClearName("ui-components.change-tracker.operations.clear.name"),

    CreativeTabClipboard("creative-tab.clipboard.name"),

    CreativeTabBits("creative-tab.bits.name"),

    PatternExportFailedCouldNotWriteAtlas("patterns.export.failed.could-not-write-atlas"),
    PatternExportFailedGenericAtlasWriteFailure("patterns.export.failed.generic-atlas-write-failure"),
    PatternExportFailedCouldNotWriteChiselData("patterns.export.failed.could-not-write-chisel-data"),
    PatternExportFailedCouldNotWriteFile("patterns.export.failed.could-not-write-file"),
    PatternImportFailedFileNotFound("patterns.import.failed.file-not-found"),
    PatternImportFailedCouldNotReadFile("patterns.import.failed.could-not-read-file"),
    PatternImportFailedCouldNotDecompressFile("patterns.import.failed.could-not-decompress-file"),
    PatternImportFailedCompressedDataInWrongFormat("patterns.import.failed.compressed-data-in-wrong-format"),
    PatternImportFailedInvalidChiselData("patterns.import.failed.invalid-chisel-data"),
    PatternImportFailedUnknownVersion("patterns.import.failed.unknown-version"),
    PatternImportInvokedFromTheServer("patterns.import.invoked-from-the-server"),
    ChiselAttemptFailedNoBlock("chisel.attempt.failed.no-block"),
    ChiselAttemptFailedTargetedBlockNotChiselable("chisel.attempt.failed.targeted-block-not-chiselable"),
    ChiselAttemptFailedChiselBroke("chisel.attempt.failed.chisel-broke"),
    ChiselAttemptFailedNoValidStateFound("chisel.attempt.failed.no-valid-state-found"),
    ChiselAttemptFailedNoPlaceableBitHeld("chisel.attempt.failed.no-placeable-bit-held"),
    ChiselAttemptFailedNotEnoughBits("chisel.attempt.failed.not-enough-bits"),
    ChiselAttemptFailedAttemptTooHigh("chisel.attempt.failed.attempt-too-high"),
    ChiselAttemptFailedAttemptTooLow("chisel.attempt.failed.attempt-too-low"),
    ChiselAttemptFailedWaitForCoolDown("chisel.attempt.failed.wait-for-cooldown"),
    PatternCuttingAcrossXAxis("pattern.cutting.across.axis.x"),
    PatternCuttingAcrossYAxis("pattern.cutting.across.axis.y"),
    PatternCuttingAcrossZAxis("pattern.cutting.across.axis.z"),
    DefaultChiseledBlockItemName("default-chiseled-block-item.name"),
    ChiselsAndBitsName("name");

    private final String string;

    LocalStrings(
      final String postFix)
    {
        string = "mod.chiselsandbits." + postFix;
    }

    @Override
    public String toString()
    {
        return string;
    }

    public MutableComponent getText()
    {
        return Component.translatable(string);
    }

    public MutableComponent getText(
      final Object... args
    )
    {
        return Component.translatable(string, args);
    }

    public List<MutableComponent> getTextLines(
      final Object... args
    )
    {
        final MutableComponent component = Component.translatable(string, args);
        final String componentString = component.getString();

        final MutableComponent result = Component.literal("");
        final String[] lines = componentString.split("\n");
        return Arrays.stream(lines).map(Component::literal).collect(Collectors.toList());
    }
}
