package mod.chiselsandbits.api.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Translation keys for the mod's chisel modes, help text, UI labels, and operation feedback.
 */
public enum LocalStrings {
	/**
	 * Translation key for the single chisel mode; resource id {@code mod.chiselsandbits.chiselmode.single}.
	 */
	ChiselModeSingle("chiselmode.single"),
	/**
	 * Translation key for the snap2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.snap2}.
	 */
	ChiselModeSnap2("chiselmode.snap2"),
	/**
	 * Translation key for the snap4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.snap4}.
	 */
	ChiselModeSnap4("chiselmode.snap4"),
	/**
	 * Translation key for the snap8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.snap8}.
	 */
	ChiselModeSnap8("chiselmode.snap8"),
	/**
	 * Translation key for the line chisel mode; resource id {@code mod.chiselsandbits.chiselmode.line}.
	 */
	ChiselModeLine("chiselmode.line"),
	/**
	 * Translation key for the line2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.line2}.
	 */
	ChiselModeLine2("chiselmode.line2"),
	/**
	 * Translation key for the line4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.line4}.
	 */
	ChiselModeLine4("chiselmode.line4"),
	/**
	 * Translation key for the line8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.line8}.
	 */
	ChiselModeLine8("chiselmode.line8"),
	/**
	 * Translation key for the plane chisel mode; resource id {@code mod.chiselsandbits.chiselmode.plane}.
	 */
	ChiselModePlane("chiselmode.plane"),
	/**
	 * Translation key for the plane2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.plane2}.
	 */
	ChiselModePlane2("chiselmode.plane2"),
	/**
	 * Translation key for the plane4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.plane4}.
	 */
	ChiselModePlane4("chiselmode.plane4"),
	/**
	 * Translation key for the plane8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.plane8}.
	 */
	ChiselModePlane8("chiselmode.plane8"),
	/**
	 * Translation key for the connected plane chisel mode; resource id {@code mod.chiselsandbits.chiselmode.connected_plane}.
	 */
	ChiselModeConnectedPlane("chiselmode.connected_plane"),
	/**
	 * Translation key for the connected material chisel mode; resource id {@code mod.chiselsandbits.chiselmode.connected_material}.
	 */
	ChiselModeConnectedMaterial("chiselmode.connected_material"),
	/**
	 * Translation key for the cube small chisel mode; resource id {@code mod.chiselsandbits.chiselmode.cube_small}.
	 */
	ChiselModeCubeSmall("chiselmode.cube_small"),
	/**
	 * Translation key for the cube medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.cube_medium}.
	 */
	ChiselModeCubeMedium("chiselmode.cube_medium"),
	/**
	 * Translation key for the cube large chisel mode; resource id {@code mod.chiselsandbits.chiselmode.cube_large}.
	 */
	ChiselModeCubeLarge("chiselmode.cube_large"),
	/**
	 * Translation key for the cube full chisel mode; resource id {@code mod.chiselsandbits.chiselmode.cube_full}.
	 */
	ChiselModeCubeFull("chiselmode.cube_full"),
	/**
	 * Translation key for the same material chisel mode; resource id {@code mod.chiselsandbits.chiselmode.same_material}.
	 */
	ChiselModeSameMaterial("chiselmode.same_material"),
	/**
	 * Translation key for the drawn cube chisel mode; resource id {@code mod.chiselsandbits.chiselmode.drawn_cube}.
	 */
	ChiselModeDrawnCube("chiselmode.drawn_cube"),
	/**
	 * Translation key for the drawn line chisel mode; resource id {@code mod.chiselsandbits.chiselmode.drawn_line}.
	 */
	ChiselModeDrawnLine("chiselmode.drawn_line"),
	/**
	 * Translation key for the drawn wall thin chisel mode; resource id {@code mod.chiselsandbits.chiselmode.drawn_wall_thin}.
	 */
	ChiselModeDrawnWallThin("chiselmode.drawn_wall_thin"),
	/**
	 * Translation key for the drawn wall medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.drawn_wall_medium}.
	 */
	ChiselModeDrawnWallMedium("chiselmode.drawn_wall_medium"),
	/**
	 * Translation key for the drawn wall fat chisel mode; resource id {@code mod.chiselsandbits.chiselmode.drawn_wall_fat}.
	 */
	ChiselModeDrawnWallFat("chiselmode.drawn_wall_fat"),
	/**
	 * Translation key for the replace chisel mode; resource id {@code mod.chiselsandbits.chiselmode.replace}.
	 */
	ChiselModeReplace("chiselmode.replace"),
	/**
	 * Translation key for the sphere small chisel mode; resource id {@code mod.chiselsandbits.chiselmode.sphere_small}.
	 */
	ChiselModeSphereSmall("chiselmode.sphere_small"),
	/**
	 * Translation key for the sphere medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.sphere_medium}.
	 */
	ChiselModeSphereMedium("chiselmode.sphere_medium"),
	/**
	 * Translation key for the sphere large chisel mode; resource id {@code mod.chiselsandbits.chiselmode.sphere_large}.
	 */
	ChiselModeSphereLarge("chiselmode.sphere_large"),

	/**
	 * Translation key for the multiline single chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.single}.
	 */
	ChiselModeMultiLineSingle("chiselmode.multiline.single"),
	/**
	 * Translation key for the multiline snap2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.snap2}.
	 */
	ChiselModeMultiLineSnap2("chiselmode.multiline.snap2"),
	/**
	 * Translation key for the multiline snap4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.snap4}.
	 */
	ChiselModeMultiLineSnap4("chiselmode.multiline.snap4"),
	/**
	 * Translation key for the multiline snap8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.snap8}.
	 */
	ChiselModeMultiLineSnap8("chiselmode.multiline.snap8"),
	/**
	 * Translation key for the multiline line chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.line}.
	 */
	ChiselModeMultiLineLine("chiselmode.multiline.line"),
	/**
	 * Translation key for the multiline line2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.line2}.
	 */
	ChiselModeMultiLineLine2("chiselmode.multiline.line2"),
	/**
	 * Translation key for the multiline line4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.line4}.
	 */
	ChiselModeMultiLineLine4("chiselmode.multiline.line4"),
	/**
	 * Translation key for the multiline line8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.line8}.
	 */
	ChiselModeMultiLineLine8("chiselmode.multiline.line8"),
	/**
	 * Translation key for the multiline plane chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.plane}.
	 */
	ChiselModeMultiLinePlane("chiselmode.multiline.plane"),
	/**
	 * Translation key for the multiline plane2 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.plane2}.
	 */
	ChiselModeMultiLinePlane2("chiselmode.multiline.plane2"),
	/**
	 * Translation key for the multiline plane4 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.plane4}.
	 */
	ChiselModeMultiLinePlane4("chiselmode.multiline.plane4"),
	/**
	 * Translation key for the multiline plane8 chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.plane8}.
	 */
	ChiselModeMultiLinePlane8("chiselmode.multiline.plane8"),
	/**
	 * Translation key for the multiline connected plane chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.connected_plane}.
	 */
	ChiselModeMultiLineConnectedPlane("chiselmode.multiline.connected_plane"),
	/**
	 * Translation key for the multiline connected material chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.connected_material}.
	 */
	ChiselModeMultiLineConnectedMaterial("chiselmode.multiline.connected_material"),
	/**
	 * Translation key for the multiline cube small chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.cube_small}.
	 */
	ChiselModeMultiLineCubeSmall("chiselmode.multiline.cube_small"),
	/**
	 * Translation key for the multiline cube medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.cube_medium}.
	 */
	ChiselModeMultiLineCubeMedium("chiselmode.multiline.cube_medium"),
	/**
	 * Translation key for the multiline cube large chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.cube_large}.
	 */
	ChiselModeMultiLineCubeLarge("chiselmode.multiline.cube_large"),
	/**
	 * Translation key for the multiline cube full chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.cube_full}.
	 */
	ChiselModeMultiLineCubeFull("chiselmode.multiline.cube_full"),
	/**
	 * Translation key for the multiline same material chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.same_material}.
	 */
	ChiselModeMultiLineSameMaterial("chiselmode.multiline.same_material"),
	/**
	 * Translation key for the multiline drawn cube chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.drawn_cube}.
	 */
	ChiselModeMultiLineDrawnCube("chiselmode.multiline.drawn_cube"),
	/**
	 * Translation key for the multiline drawn line chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.drawn_line}.
	 */
	ChiselModeMultiLineDrawnLine("chiselmode.multiline.drawn_line"),
	/**
	 * Translation key for the multiline drawn wall thin chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.drawn_wall_thin}.
	 */
	ChiselModeMultiLineDrawnWallThin("chiselmode.multiline.drawn_wall_thin"),
	/**
	 * Translation key for the multiline drawn wall medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.drawn_wall_medium}.
	 */
	ChiselModeMultiLineDrawnWallMedium("chiselmode.multiline.drawn_wall_medium"),
	/**
	 * Translation key for the multiline drawn wall fat chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.drawn_wall_fat}.
	 */
	ChiselModeMultiLineDrawnWallFat("chiselmode.multiline.drawn_wall_fat"),
	/**
	 * Translation key for the multiline replace chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.replace}.
	 */
	ChiselModeMultiLineReplace("chiselmode.multiline.replace"),
	/**
	 * Translation key for the multiline sphere small chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.sphere_small}.
	 */
	ChiselModeMultiLineSphereSmall("chiselmode.multiline.sphere_small"),
	/**
	 * Translation key for the multiline sphere medium chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.sphere_medium}.
	 */
	ChiselModeMultiLineSphereMedium("chiselmode.multiline.sphere_medium"),
	/**
	 * Translation key for the multiline sphere large chisel mode; resource id {@code mod.chiselsandbits.chiselmode.multiline.sphere_large}.
	 */
	ChiselModeMultiLineSphereLarge("chiselmode.multiline.sphere_large"),

	/**
	 * Translation key for help text for ShiftDetails; resource id {@code mod.chiselsandbits.help.shiftdetails}.
	 */
	ShiftDetails("help.shiftdetails"),
	/**
	 * Translation key for help text for Empty; resource id {@code mod.chiselsandbits.help.empty}.
	 */
	Empty("help.empty"),
	/**
	 * Translation key for help text for Filled; resource id {@code mod.chiselsandbits.help.filled}.
	 */
	Filled("help.filled"),

	/**
	 * Translation key for help text for ChiseledBlock; resource id {@code mod.chiselsandbits.help.chiseled_block}.
	 */
	HelpChiseledBlock("help.chiseled_block"),
	/**
	 * Translation key for help text for ChiseledBlock; resource id {@code mod.chiselsandbits.help.chiseled_block.long}.
	 */
	LongHelpChiseledBlock("help.chiseled_block.long"),

	/**
	 * Translation key for help text for BitSaw; resource id {@code mod.chiselsandbits.help.bitsaw}.
	 */
	HelpBitSaw("help.bitsaw"),
	/**
	 * Translation key for help text for BitSaw; resource id {@code mod.chiselsandbits.help.bitsaw.long}.
	 */
	LongHelpBitSaw("help.bitsaw.long"),

	/**
	 * Translation key for help text for BitBag; resource id {@code mod.chiselsandbits.help.bit_bag}.
	 */
	HelpBitBag("help.bit_bag"),
	/**
	 * Translation key for help text for BitBag; resource id {@code mod.chiselsandbits.help.bit_bag.long}.
	 */
	LongHelpBitBag("help.bit_bag.long"),
	/**
	 * Translation key for a container title; resource id {@code mod.chiselsandbits.container.bit_bag}.
	 */
	ContainerBitBag("container.bit_bag"),

	/**
	 * Translation key for help text for Wrench; resource id {@code mod.chiselsandbits.help.wrench}.
	 */
	HelpWrench("help.wrench"),
	/**
	 * Translation key for help text for Wrench; resource id {@code mod.chiselsandbits.help.wrench.long}.
	 */
	LongHelpWrench("help.wrench.long"),

	/**
	 * Translation key for help text for Bit; resource id {@code mod.chiselsandbits.help.bit}.
	 */
	HelpBit("help.bit"),
	/**
	 * Translation key for help text for Bit; resource id {@code mod.chiselsandbits.help.bit.long}.
	 */
	LongHelpBit("help.bit.long"),
	/**
	 * Translation key for help text for AnyHelpBit; resource id {@code mod.chiselsandbits.help.bit.any}.
	 */
	AnyHelpBit("help.bit.any"),

	/**
	 * Translation key for help text for BitStorageFilled; resource id {@code mod.chiselsandbits.help.bittank.full}.
	 */
	HelpBitStorageFilled("help.bittank.full"),
	/**
	 * Translation key for help text for BitStorageEmpty; resource id {@code mod.chiselsandbits.help.bittank.empty}.
	 */
	HelpBitStorageEmpty("help.bittank.empty"),
	/**
	 * Translation key for help text for BitStorage; resource id {@code mod.chiselsandbits.help.bittank.long}.
	 */
	LongHelpBitStorage("help.bittank.long"),

	/**
	 * Translation key for help text for SimplePattern; resource id {@code mod.chiselsandbits.help.pattern.simple}.
	 */
	HelpSimplePattern("help.pattern.simple"),
	/**
	 * Translation key for help text for SimplePattern; resource id {@code mod.chiselsandbits.help.pattern.simple.long}.
	 */
	LongHelpSimplePattern("help.pattern.simple.long"),

	/**
	 * Translation key for help text for SealedPattern; resource id {@code mod.chiselsandbits.help.pattern.sealed}.
	 */
	HelpSealedPattern("help.pattern.sealed"),
	/**
	 * Translation key for help text for SealedPattern; resource id {@code mod.chiselsandbits.help.pattern.sealed.long}.
	 */
	LongHelpSealedPattern("help.pattern.sealed.long"),

	/**
	 * Translation key for help text for Chisel; resource id {@code mod.chiselsandbits.help.chisel}.
	 */
	HelpChisel("help.chisel"),
	/**
	 * Translation key for help text for Chisel; resource id {@code mod.chiselsandbits.help.chisel.long}.
	 */
	LongHelpChisel("help.chisel.long"),

	/**
	 * Translation key for help text for noBind; resource id {@code mod.chiselsandbits.help.nobind}.
	 */
	noBind("help.nobind"),

	/**
	 * Translation key for help text for leftShift; resource id {@code mod.chiselsandbits.help.leftshift}.
	 */
	leftShift("help.leftshift"),
	/**
	 * Translation key for help text for rightShift; resource id {@code mod.chiselsandbits.help.rightshift}.
	 */
	rightShift("help.rightshift"),

	/**
	 * Translation key for help text for leftAlt; resource id {@code mod.chiselsandbits.help.leftalt}.
	 */
	leftAlt("help.leftalt"),
	/**
	 * Translation key for help text for rightAlt; resource id {@code mod.chiselsandbits.help.rightalt}.
	 */
	rightAlt("help.rightalt"),

	/**
	 * Translation key for help text for Sort; resource id {@code mod.chiselsandbits.help.sort}.
	 */
	Sort("help.sort"),
	/**
	 * Translation key for help text for Trash; resource id {@code mod.chiselsandbits.help.trash}.
	 */
	Trash("help.trash"),
	/**
	 * Translation key for help text for TrashItem; resource id {@code mod.chiselsandbits.help.trashitem}.
	 */
	TrashItem("help.trashitem"),
	/**
	 * Translation key for help text for ReallyTrash; resource id {@code mod.chiselsandbits.help.reallytrash}.
	 */
	ReallyTrash("help.reallytrash"),
	/**
	 * Translation key for help text for ReallyTrashItem; resource id {@code mod.chiselsandbits.help.reallytrash_blank}.
	 */
	ReallyTrashItem("help.reallytrash_blank"),
	/**
	 * Translation key for help text for TrashInvalidItem; resource id {@code mod.chiselsandbits.help.trash.invalid}.
	 */
	TrashInvalidItem("help.trash.invalid"),
	/**
	 * Translation key for help text for Convert; resource id {@code mod.chiselsandbits.help.convert}.
	 */
	Convert("help.convert"),
	/**
	 * Translation key for help text for NonPreferredPickup; resource id {@code mod.chiselsandbits.help.non-preferred-pickup}.
	 */
	NonPreferredPickup("help.non-preferred-pickup"),
	/**
	 * Translation key for help text for PreferredPickup; resource id {@code mod.chiselsandbits.help.preferred-pickup}.
	 */
	PreferredPickup("help.preferred-pickup"),
	/**
	 * Translation key for help text for FilteredPickup; resource id {@code mod.chiselsandbits.help.filtered-pickup}.
	 */
	FilteredPickup("help.filtered-pickup"),
	/**
	 * Translation key for help text for NonFilteredPickup; resource id {@code mod.chiselsandbits.help.non-filtered-pickup}.
	 */
	NonFilteredPickup("help.non-filtered-pickup"),

	/**
	 * Translation key for a positive-pattern mode label; resource id {@code mod.chiselsandbits.positivepatternmode.replace}.
	 */
	PositivePatternReplace("positivepatternmode.replace"),
	/**
	 * Translation key for a positive-pattern mode label; resource id {@code mod.chiselsandbits.positivepatternmode.additive}.
	 */
	PositivePatternAdditive("positivepatternmode.additive"),
	/**
	 * Translation key for a positive-pattern mode label; resource id {@code mod.chiselsandbits.positivepatternmode.placement}.
	 */
	PositivePatternPlacement("positivepatternmode.placement"),
	/**
	 * Translation key for a positive-pattern mode label; resource id {@code mod.chiselsandbits.positivepatternmode.impose}.
	 */
	PositivePatternImpose("positivepatternmode.impose"),

	/**
	 * Translation key for help text for TapeMeasure; resource id {@code mod.chiselsandbits.help.tape_measure}.
	 */
	HelpTapeMeasure("help.tape_measure"),
	/**
	 * Translation key for help text for TapeMeasure; resource id {@code mod.chiselsandbits.help.tape_measure.long}.
	 */
	LongHelpTapeMeasure("help.tape_measure.long"),

	/**
	 * Translation key for help text for Quill; resource id {@code mod.chiselsandbits.help.quill}.
	 */
	HelpQuill("help.quill"),
	/**
	 * Translation key for help text for Sealant; resource id {@code mod.chiselsandbits.help.sealant}.
	 */
	HelpSealant("help.sealant"),
	/**
	 * Translation key for help text for Unseal; resource id {@code mod.chiselsandbits.help.unseal}.
	 */
	HelpUnseal("help.unseal"),

	/**
	 * Translation key for a tape-measure label; resource id {@code mod.chiselsandbits.tapemeasure.bit}.
	 */
	TapeMeasureBit("tapemeasure.bit"),
	/**
	 * Translation key for a tape-measure label; resource id {@code mod.chiselsandbits.tapemeasure.block}.
	 */
	TapeMeasureBlock("tapemeasure.block"),
	/**
	 * Translation key for a tape-measure label; resource id {@code mod.chiselsandbits.tapemeasure.distance}.
	 */
	TapeMeasureDistance("tapemeasure.distance"),

	/**
	 * Translation key for a bit placement option label; resource id {@code mod.chiselsandbits.bitoption.place}.
	 */
	BitOptionPlace("bitoption.place"),
	/**
	 * Translation key for a bit placement option label; resource id {@code mod.chiselsandbits.bitoption.replace}.
	 */
	BitOptionReplace("bitoption.replace"),

	/**
	 * Translation key for help text for MagnifyingGlass; resource id {@code mod.chiselsandbits.help.magnifying_glass}.
	 */
	HelpMagnifyingGlass("help.magnifying_glass"),
	/**
	 * Translation key for help text for MagnifyingGlass; resource id {@code mod.chiselsandbits.help.magnifying_glass.long}.
	 */
	LongHelpMagnifyingGlass("help.magnifying_glass.long"),

	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.already-chiseled}.
	 */
	ChiselSupportIsAlreadyChiseled("chisel.support.already-chiseled"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.generic}.
	 */
	ChiselSupportGenericNotSupported("chisel.support.not.supported.generic"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.supported.code}.
	 */
	ChiselSupportLogicIgnored("chisel.support.supported.code"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.supported.generic}.
	 */
	ChiselSupportGenericSupported("chisel.support.supported.generic"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.supported.fluid}.
	 */
	ChiselSupportGenericFluidSupport("chisel.support.supported.fluid"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.compatibility.deactivated}.
	 */
	ChiselSupportCompatDeactivated("chisel.support.not.supported.compatibility.deactivated"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.collision}.
	 */
	ChiselSupportCustomCollision("chisel.support.not.supported.collision"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.hardness}.
	 */
	ChiselSupportNoHardness("chisel.support.not.supported.hardness"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.shape}.
	 */
	ChiselSupportNotFullBlock("chisel.support.not.supported.shape"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.entity}.
	 */
	ChiselSupportHasBehaviour("chisel.support.not.supported.entity"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.slab}.
	 */
	ChiselSupportIsSlab("chisel.support.not.supported.slab"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.drops}.
	 */
	ChiselSupportHasCustomDrops("chisel.support.not.supported.drops"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.failure}.
	 */
	ChiselSupportFailureToAnalyze("chisel.support.not.supported.failure"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.forced}.
	 */
	ChiselSupportForcedUnsupported("chisel.support.not.supported.forced"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.supported.forced}.
	 */
	ChiselSupportForcedSupported("chisel.support.supported.forced"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.not.supported.tag}.
	 */
	ChiselSupportTagBlackListed("chisel.support.not.supported.tag"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.support.supported.tag}.
	 */
	ChiselSupportTagWhitelisted("chisel.support.supported.tag"),

	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.station.name}.
	 */
	ChiselStationName("chisel.station.name"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.station.help}.
	 */
	ChiselStationHelp("chisel.station.help"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.station.help.long}.
	 */
	LongChiselStationHelp("chisel.station.help.long"),

	/**
	 * Translation key for localized text for modification table help; resource id {@code mod.chiselsandbits.modification.table.help}.
	 */
	ModificationTableHelp("modification.table.help"),
	/**
	 * Translation key for localized text for long modification table help; resource id {@code mod.chiselsandbits.modification.table.help.long}.
	 */
	LongModificationTableHelp("modification.table.help.long"),

	/**
	 * Translation key for feedback for a mod command; resource id {@code mod.chiselsandbits.command.give.blockstate.not-chiselable}.
	 */
	CommandGiveErrorBlockStateNotChiselable("command.give.blockstate.not-chiselable"),
	/**
	 * Translation key for feedback for a mod command; resource id {@code mod.chiselsandbits.command.fill.completed}.
	 */
	CommandFillCompleted("command.fill.completed"),
	/**
	 * Translation key for feedback for a mod command; resource id {@code mod.chiselsandbits.command.fill.in-progress}.
	 */
	CommandFillingInProgress("command.fill.in-progress"),
	/**
	 * Translation key for feedback for a mod command; resource id {@code mod.chiselsandbits.command.fill.to-fill}.
	 */
	CommandToFill("command.fill.to-fill"),
	/**
	 * Translation key for feedback for a mod command; resource id {@code mod.chiselsandbits.command.fill.processing-block-updates}.
	 */
	CommandProcessingBlockUpdates("command.fill.processing-block-updates"),

	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.item.tooltip.mode.grouped}.
	 */
	PatternItemTooltipModeGrouped("pattern.item.tooltip.mode.grouped"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.item.tooltip.mode.simple}.
	 */
	PatternItemTooltipModeSimple("pattern.item.tooltip.mode.simple"),

	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.mode.placement}.
	 */
	PatternPlacementModePlacement("pattern.placement.mode.placement"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.mode.removal}.
	 */
	PatternPlacementModeRemoval("pattern.placement.mode.removal"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.mode.imposement}.
	 */
	PatternPlacementModeImposement("pattern.placement.mode.imposement"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.mode.merge}.
	 */
	PatternPlacementModeMerge("pattern.placement.mode.merge"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.mode.carving}.
	 */
	PatternPlacementModeCarving("pattern.placement.mode.carving"),

	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.collision}.
	 */
	PatternPlacementCollision("pattern.placement.failure.collision"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.no.bit.space}.
	 */
	PatternPlacementNoBitSpace("pattern.placement.failure.no.bit.space"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.no.bits}.
	 */
	PatternPlacementNotEnoughBits("pattern.placement.failure.no.bits"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.not.a.solid.block}.
	 */
	PatternPlacementNotASolidBlock("pattern.placement.failure.not.a.solid.block"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.not.a.chiseled.block}.
	 */
	PatternPlacementNotAChiseledBlock("pattern.placement.failure.not.a.chiseled.block"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.not.a.air.block}.
	 */
	PatternPlacementNotAnAirBlock("pattern.placement.failure.not.a.air.block"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.placement.failure.not.a.supported.block}.
	 */
	PatternPlacementNotASupportedBlock("pattern.placement.failure.not.a.supported.block"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.modification.group.mirror}.
	 */
	PatternModificationGroupMirror("pattern.modification.group.mirror"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.modification.group.rotate}.
	 */
	PatternModificationGroupRotate("pattern.modification.group.rotate"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.modification.across.axis.x}.
	 */
	PatternModificationAcrossXAxis("pattern.modification.across.axis.x"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.modification.across.axis.y}.
	 */
	PatternModificationAcrossYAxis("pattern.modification.across.axis.y"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.modification.across.axis.z}.
	 */
	PatternModificationAcrossZAxis("pattern.modification.across.axis.z"),
	/**
	 * Translation key for change-tracking undo or redo feedback; resource id {@code mod.chiselsandbits.change-tracking.undo.failed}.
	 */
	CanNotUndo("change-tracking.undo.failed"),
	/**
	 * Translation key for change-tracking undo or redo feedback; resource id {@code mod.chiselsandbits.change-tracking.undo.success}.
	 */
	UndoSuccessful("change-tracking.undo.success"),
	/**
	 * Translation key for change-tracking undo or redo feedback; resource id {@code mod.chiselsandbits.change-tracking.redo.failed}.
	 */
	CanNotRedo("change-tracking.redo.failed"),
	/**
	 * Translation key for change-tracking undo or redo feedback; resource id {@code mod.chiselsandbits.change-tracking.redo.success}.
	 */
	RedoSuccessful("change-tracking.redo.success"),

	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.page-selector.name}.
	 */
	ToolMenuPageSelectorName("ui-components.tool-menu.page-selector.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.page-selector.previous.name}.
	 */
	ToolMenuPreviousPageName("ui-components.tool-menu.page-selector.previous.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.page-selector.next.name}.
	 */
	ToolMenuNextPageName("ui-components.tool-menu.page-selector.next.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.group-selector.name}.
	 */
	ToolMenuGroupSelectorName("ui-components.tool-menu.group-selector.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.mode-selector.name}.
	 */
	ToolMenuModeSelectorName("ui-components.tool-menu.mode-selector.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.tool-menu.selector.name}.
	 */
	ToolMenuSelectorName("ui-components.tool-menu.selector.name"),
	/**
	 * Translation key for a user-interface screen title; resource id {@code mod.chiselsandbits.ui-screens.tool-menu.name}.
	 */
	ToolMenuScreenName("ui-screens.tool-menu.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.change-tracker.operations.name}.
	 */
	ChangeTrackerOperations("ui-components.change-tracker.operations.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.change-tracker.operations.undo.name}.
	 */
	ChangeTrackerOperationsButtonUndoName("ui-components.change-tracker.operations.undo.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.change-tracker.operations.redo.name}.
	 */
	ChangeTrackerOperationsButtonRedoName("ui-components.change-tracker.operations.redo.name"),
	/**
	 * Translation key for a user-interface component label; resource id {@code mod.chiselsandbits.ui-components.change-tracker.operations.clear.name}.
	 */
	ChangeTrackerOperationsButtonClearName("ui-components.change-tracker.operations.clear.name"),

	/**
	 * Translation key for a creative inventory tab title; resource id {@code mod.chiselsandbits.creative-tab.clipboard.name}.
	 */
	CreativeTabClipboard("creative-tab.clipboard.name"),

	/**
	 * Translation key for a creative inventory tab title; resource id {@code mod.chiselsandbits.creative-tab.bits.name}.
	 */
	CreativeTabBits("creative-tab.bits.name"),

	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.export.failed.could-not-write-atlas}.
	 */
	PatternExportFailedCouldNotWriteAtlas("patterns.export.failed.could-not-write-atlas"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.export.failed.generic-atlas-write-failure}.
	 */
	PatternExportFailedGenericAtlasWriteFailure("patterns.export.failed.generic-atlas-write-failure"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.export.failed.could-not-write-chisel-data}.
	 */
	PatternExportFailedCouldNotWriteChiselData("patterns.export.failed.could-not-write-chisel-data"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.export.failed.could-not-write-file}.
	 */
	PatternExportFailedCouldNotWriteFile("patterns.export.failed.could-not-write-file"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.file-not-found}.
	 */
	PatternImportFailedFileNotFound("patterns.import.failed.file-not-found"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.could-not-read-file}.
	 */
	PatternImportFailedCouldNotReadFile("patterns.import.failed.could-not-read-file"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.could-not-decompress-file}.
	 */
	PatternImportFailedCouldNotDecompressFile("patterns.import.failed.could-not-decompress-file"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.compressed-data-in-wrong-format}.
	 */
	PatternImportFailedCompressedDataInWrongFormat("patterns.import.failed.compressed-data-in-wrong-format"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.invalid-chisel-data}.
	 */
	PatternImportFailedInvalidChiselData("patterns.import.failed.invalid-chisel-data"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.failed.unknown-version}.
	 */
	PatternImportFailedUnknownVersion("patterns.import.failed.unknown-version"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.patterns.import.invoked-from-the-server}.
	 */
	PatternImportInvokedFromTheServer("patterns.import.invoked-from-the-server"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.no-block}.
	 */
	ChiselAttemptFailedNoBlock("chisel.attempt.failed.no-block"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.targeted-block-not-chiselable}.
	 */
	ChiselAttemptFailedTargetedBlockNotChiselable("chisel.attempt.failed.targeted-block-not-chiselable"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.chisel-broke}.
	 */
	ChiselAttemptFailedChiselBroke("chisel.attempt.failed.chisel-broke"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.no-valid-state-found}.
	 */
	ChiselAttemptFailedNoValidStateFound("chisel.attempt.failed.no-valid-state-found"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.no-placeable-bit-held}.
	 */
	ChiselAttemptFailedNoPlaceableBitHeld("chisel.attempt.failed.no-placeable-bit-held"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.not-enough-bits}.
	 */
	ChiselAttemptFailedNotEnoughBits("chisel.attempt.failed.not-enough-bits"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.attempt-too-high}.
	 */
	ChiselAttemptFailedAttemptTooHigh("chisel.attempt.failed.attempt-too-high"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.attempt-too-low}.
	 */
	ChiselAttemptFailedAttemptTooLow("chisel.attempt.failed.attempt-too-low"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.wait-for-cooldown}.
	 */
	ChiselAttemptFailedWaitForCoolDown("chisel.attempt.failed.wait-for-cooldown"),
	/**
	 * Translation key for chiseling support, station, or attempt feedback; resource id {@code mod.chiselsandbits.chisel.attempt.failed.missing-chisel-in-offhand}.
	 */
	ChiselAttemptMissingChiselInOffhand("chisel.attempt.failed.missing-chisel-in-offhand"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.cutting.across.axis.x}.
	 */
	PatternCuttingAcrossXAxis("pattern.cutting.across.axis.x"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.cutting.across.axis.y}.
	 */
	PatternCuttingAcrossYAxis("pattern.cutting.across.axis.y"),
	/**
	 * Translation key for pattern placement, editing, import, export, or cutting feedback; resource id {@code mod.chiselsandbits.pattern.cutting.across.axis.z}.
	 */
	PatternCuttingAcrossZAxis("pattern.cutting.across.axis.z"),
	/**
	 * Translation key for localized text for default chiseled block item name; resource id {@code mod.chiselsandbits.default-chiseled-block-item.name}.
	 */
	DefaultChiseledBlockItemName("default-chiseled-block-item.name"),
	/**
	 * Translation key for localized text for chiseled block item name; resource id {@code mod.chiselsandbits.chiseled-block-item.name}.
	 */
	ChiseledBlockItemName("chiseled-block-item.name"),
	/**
	 * Translation key for localized text for chisels and bits name; resource id {@code mod.chiselsandbits.name}.
	 */
	ChiselsAndBitsName("name"),
	/**
	 * Translation key for localized text for bit bag empty; resource id {@code mod.chiselsandbits.bit-bag-empty}.
	 */
	BitBagEmpty("bit-bag-empty"),
	/**
	 * Translation key for localized text for bit bag entry; resource id {@code mod.chiselsandbits.bit-bag-entry}.
	 */
	BitBagEntry("bit-bag-entry"),
	/**
	 * Translation key for localized text for bit bag more entries; resource id {@code mod.chiselsandbits.bit-bag-more-entries}.
	 */
	BitBagMoreEntries("bit-bag-more-entries"),
	/**
	 * Translation key for localized text for bag picks up first; resource id {@code mod.chiselsandbits.bag-pick-up.bag-first}.
	 */
	BagPicksUpFirst("bag-pick-up.bag-first"),
	/**
	 * Translation key for localized text for player picks up first; resource id {@code mod.chiselsandbits.bag-pick-up.player-first}.
	 */
	PlayerPicksUpFirst("bag-pick-up.player-first"),
	/**
	 * Translation key for localized text for filtered; resource id {@code mod.chiselsandbits.bag-pick-up.filtered}.
	 */
	Filtered("bag-pick-up.filtered"),
	/**
	 * Translation key for localized text for non filtered; resource id {@code mod.chiselsandbits.bag-pick-up.non-filtered}.
	 */
	NonFiltered("bag-pick-up.non-filtered"),
	/**
	 * Translation key for help text for BagPickupMode; resource id {@code mod.chiselsandbits.help.bit_bag.pickup-mode}.
	 */
	HelpBagPickupMode("help.bit_bag.pickup-mode");

	private final String string;

	/**
	 * Creates a translation key by prefixing the mod namespace.
	 */
	LocalStrings(
			final String postFix) {
		string = "mod.chiselsandbits." + postFix;
	}

	/**
	 * Returns this key as its fully qualified translation identifier.
	 */
	@Override
	public String toString() {
		return string;
	}

	/**
	 * Creates a translatable component for this key without format arguments.
	 *
	 * @return the requested value.
	 */
	public MutableComponent getText() {
		return Component.translatable(string);
	}

	/**
	 * Creates a translatable component for this key using the supplied format arguments.
	 *
	 * @param args values substituted into the translation template
	 * @return the translatable component
	 */
	public MutableComponent getText(
			final Object... args
	) {
		return Component.translatable(string, args);
	}

	/**
	 * Resolves this translation and returns one literal component for each newline-separated line.
	 *
	 * @param args values substituted into the translation template
	 * @return translated lines as literal components
	 */
	public List<MutableComponent> getTextLines(
			final Object... args
	) {
		final MutableComponent component = Component.translatable(string, args);
		final String componentString = component.getString();

		final MutableComponent result = Component.literal("");
		final String[] lines = componentString.split("\n");
		return Arrays.stream(lines).map(Component::literal).collect(Collectors.toList());
	}
}
