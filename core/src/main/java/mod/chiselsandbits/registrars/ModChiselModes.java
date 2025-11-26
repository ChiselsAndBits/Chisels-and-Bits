package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.chiseling.modes.connected.material.ConnectedMaterialChiselingModeBuilder;
import mod.chiselsandbits.chiseling.modes.connected.plane.ConnectedPlaneChiselingModeBuilder;
import mod.chiselsandbits.chiseling.modes.cubed.CubedChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.draw.DrawnCubeChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.draw.DrawnLineChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.draw.DrawnWallChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.line.LinedChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.plane.PlaneChiselModeBuilder;
import mod.chiselsandbits.chiseling.modes.replace.ReplaceChiselingModeBuilder;
import mod.chiselsandbits.chiseling.modes.sphere.SphereChiselModeBuilder;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class ModChiselModes
{
    private static final Logger                        LOGGER         = LogManager.getLogger();
    private static final ICustomRegistrar<IChiselMode> MODE_REGISTRAR = ICustomRegistrar.create(IChiselMode.class, Constants.MOD_ID);
    public static final  IRegistryObject<IChiselMode>  SINGLE_BIT     = MODE_REGISTRAR.register(
        "single_bit",
        () -> new CubedChiselModeBuilder().setBitsPerSide(1)
            .setDisplayName(LocalStrings.ChiselModeSingle.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSingle.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> DRAWN_CUBE = MODE_REGISTRAR.register(
        "drawn_cube",
        () -> new DrawnCubeChiselModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeDrawnCube.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineDrawnCube.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "drawn_cube"))
            .createDrawnCubeChiselMode()
    );

    public static final IRegistryObject<IChiselMode> DRAWN_LINE = MODE_REGISTRAR.register(
        "drawn_line",
        () -> new DrawnLineChiselModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeDrawnLine.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineDrawnLine.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "drawn_line"))
            .createDrawnLineChiselMode()
    );

    public static final IRegistryObject<IChiselMode> DRAWN_WALL_THIN = MODE_REGISTRAR.register(
        "drawn_wall_thin",
        () -> new DrawnWallChiselModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeDrawnWallThin.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineDrawnWallThin.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "drawn_wall_1"))
            .setWidth(1)
            .createDrawnWallChiselMode()
    );

    public static final IRegistryObject<IChiselMode> DRAWN_WALL_MEDIUM = MODE_REGISTRAR.register(
        "drawn_wall_medium",
        () -> new DrawnWallChiselModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeDrawnWallMedium.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineDrawnWallMedium.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "drawn_wall_2"))
            .setWidth(2)
            .createDrawnWallChiselMode()
    );

    public static final IRegistryObject<IChiselMode> DRAWN_WALL_FAT = MODE_REGISTRAR.register(
        "drawn_wall_fat",
        () -> new DrawnWallChiselModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeDrawnWallFat.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineDrawnWallFat.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "drawn_wall_3"))
            .setWidth(3)
            .createDrawnWallChiselMode()
    );

    public static final IRegistryObject<IChiselMode> SMALL_BIT = MODE_REGISTRAR.register(
        "small_bit",
        () -> new CubedChiselModeBuilder().setBitsPerSide(2)
            .setDisplayName(LocalStrings.ChiselModeCubeSmall.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineCubeSmall.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cube_small"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> MEDIUM_BIT = MODE_REGISTRAR.register(
        "medium_bit",
        () -> new CubedChiselModeBuilder().setBitsPerSide(4)
            .setDisplayName(LocalStrings.ChiselModeCubeMedium.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineCubeMedium.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cube_medium"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LARGE_BIT = MODE_REGISTRAR.register(
        "large_bit",
        () -> new CubedChiselModeBuilder().setBitsPerSide(8)
            .setDisplayName(LocalStrings.ChiselModeCubeLarge.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineCubeLarge.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cube_large"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> SMALL_BIT_ALIGNED = MODE_REGISTRAR.register(
        "small_bit_aligned",
        () -> new CubedChiselModeBuilder().setBitsPerSide(2)
            .setAligned(true)
            .setDisplayName(LocalStrings.ChiselModeSnap2.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSnap2.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "snap2"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> MEDIUM_BIT_ALIGNED = MODE_REGISTRAR.register(
        "medium_bit_aligned",
        () -> new CubedChiselModeBuilder().setBitsPerSide(4)
            .setAligned(true)
            .setDisplayName(LocalStrings.ChiselModeSnap4.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSnap4.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "snap4"))
            .createCubedChiselMode()
    );
    public static final IRegistryObject<IChiselMode> LARGE_BIT_ALIGNED  = MODE_REGISTRAR.register(
        "large_bit_aligned",
        () -> new CubedChiselModeBuilder().setBitsPerSide(8)
            .setAligned(true)
            .setDisplayName(LocalStrings.ChiselModeSnap8.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSnap8.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "snap8"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> FULL_BLOCK = MODE_REGISTRAR.register(
        "full_cube",
        () -> new CubedChiselModeBuilder().setBitsPerSide(16)
            .setAligned(true)
            .setDisplayName(LocalStrings.ChiselModeCubeFull.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeCubeFull.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "same_material"))
            .createCubedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LINE_ONE = MODE_REGISTRAR.register(
        "line_1",
        () -> new LinedChiselModeBuilder()
            .setBitsPerSide(1)
            .setDisplayName(LocalStrings.ChiselModeLine.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineLine.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "line"))
            .createLinedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LINE_TWO = MODE_REGISTRAR.register(
        "line_2",
        () -> new LinedChiselModeBuilder()
            .setBitsPerSide(2)
            .setDisplayName(LocalStrings.ChiselModeLine2.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineLine2.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "line2"))
            .createLinedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LINE_FOUR = MODE_REGISTRAR.register(
        "line_4",
        () -> new LinedChiselModeBuilder()
            .setBitsPerSide(4)
            .setDisplayName(LocalStrings.ChiselModeLine4.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineLine4.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "line4"))
            .createLinedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LINE_EIGHT = MODE_REGISTRAR.register(
        "line_8",
        () -> new LinedChiselModeBuilder()
            .setBitsPerSide(8)
            .setDisplayName(LocalStrings.ChiselModeLine8.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineLine8.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "line8"))
            .createLinedChiselMode()
    );

    public static final IRegistryObject<IChiselMode> PLANE_ONE = MODE_REGISTRAR.register(
        "plane_1",
        () -> new PlaneChiselModeBuilder()
            .setDepth(1)
            .setDisplayName(LocalStrings.ChiselModePlane.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane"))
            .createPlaneChiselMode()
    );

    public static final IRegistryObject<IChiselMode> PLANE_TWO = MODE_REGISTRAR.register(
        "plane_2",
        () -> new PlaneChiselModeBuilder()
            .setDepth(2)
            .setDisplayName(LocalStrings.ChiselModePlane2.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane2.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane2"))
            .createPlaneChiselMode()
    );

    public static final IRegistryObject<IChiselMode> PLANE_FOUR = MODE_REGISTRAR.register(
        "plane_4",
        () -> new PlaneChiselModeBuilder()
            .setDepth(4)
            .setDisplayName(LocalStrings.ChiselModePlane4.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane4.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane4"))
            .createPlaneChiselMode()
    );

    public static final IRegistryObject<IChiselMode> PLANE_EIGHT = MODE_REGISTRAR.register(
        "plane_8",
        () -> new PlaneChiselModeBuilder()
            .setDepth(8)
            .setDisplayName(LocalStrings.ChiselModePlane8.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane8.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane8"))
            .createPlaneChiselMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_MATERIAL_ONE = MODE_REGISTRAR.register(
        "connected_material_1",
        () -> new ConnectedMaterialChiselingModeBuilder()
            .setDepth(1)
            .setDisplayName(LocalStrings.ChiselModePlane.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane"))
            .createConnectedMaterialChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_MATERIAL_TWO = MODE_REGISTRAR.register(
        "connected_material_2",
        () -> new ConnectedMaterialChiselingModeBuilder()
            .setDepth(2)
            .setDisplayName(LocalStrings.ChiselModePlane2.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane2.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane2"))
            .createConnectedMaterialChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_MATERIAL_FOUR = MODE_REGISTRAR.register(
        "connected_material_4",
        () -> new ConnectedMaterialChiselingModeBuilder()
            .setDepth(4)
            .setDisplayName(LocalStrings.ChiselModePlane4.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane4.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane4"))
            .createConnectedMaterialChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_MATERIAL_EIGHT = MODE_REGISTRAR.register(
        "connected_material_8",
        () -> new ConnectedMaterialChiselingModeBuilder()
            .setDepth(8)
            .setDisplayName(LocalStrings.ChiselModePlane8.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane8.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane8"))
            .createConnectedMaterialChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> SMALL_SPHERE = MODE_REGISTRAR.register(
        "small_sphere",
        () -> new SphereChiselModeBuilder().setDiameter(4)
            .setDisplayName(LocalStrings.ChiselModeSphereSmall.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSphereSmall.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sphere_small"))
            .createSphereChiselMode()
    );

    public static final IRegistryObject<IChiselMode> MEDIUM_SPHERE = MODE_REGISTRAR.register(
        "medium_sphere",
        () -> new SphereChiselModeBuilder().setDiameter(8)
            .setDisplayName(LocalStrings.ChiselModeSphereMedium.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSphereMedium.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sphere_medium"))
            .createSphereChiselMode()
    );

    public static final IRegistryObject<IChiselMode> LARGE_SPHERE = MODE_REGISTRAR.register(
        "large_sphere",
        () -> new SphereChiselModeBuilder().setDiameter(16)
            .setDisplayName(LocalStrings.ChiselModeSphereLarge.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineSphereLarge.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sphere_large"))
            .createSphereChiselMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_PLANE_ONE = MODE_REGISTRAR.register(
        "connected_plane_1",
        () -> new ConnectedPlaneChiselingModeBuilder()
            .setDepth(1)
            .setDisplayName(LocalStrings.ChiselModePlane.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane"))
            .createConnectedPlaneChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_PLANE_TWO = MODE_REGISTRAR.register(
        "connected_plane_2",
        () -> new ConnectedPlaneChiselingModeBuilder()
            .setDepth(2)
            .setDisplayName(LocalStrings.ChiselModePlane2.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane2.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane2"))
            .createConnectedPlaneChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_PLANE_FOUR = MODE_REGISTRAR.register(
        "connected_plane_4",
        () -> new ConnectedPlaneChiselingModeBuilder()
            .setDepth(4)
            .setDisplayName(LocalStrings.ChiselModePlane4.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane4.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane4"))
            .createConnectedPlaneChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> CONNECTED_PLANE_EIGHT = MODE_REGISTRAR.register(
        "connected_plane_8",
        () -> new ConnectedPlaneChiselingModeBuilder()
            .setDepth(8)
            .setDisplayName(LocalStrings.ChiselModePlane8.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLinePlane8.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "plane8"))
            .createConnectedPlaneChiselingMode()
    );

    public static final IRegistryObject<IChiselMode> REPLACE = MODE_REGISTRAR.register(
        "replace",
        () -> new ReplaceChiselingModeBuilder()
            .setDisplayName(LocalStrings.ChiselModeReplace.getText())
            .setMultiLineDisplayName(LocalStrings.ChiselModeMultiLineReplace.getText())
            .setIconName(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "replace"))
            .createReplaceChiselingMode()
    );

    public static Supplier<ICustomRegistry<IChiselMode>> REGISTRY =
        () -> {
            throw new IllegalStateException("Registry is not setup yet. Use a Deferred Register!");
        };

    private ModChiselModes()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ModChiselModes. This is a utility class");
    }

    public static void onModConstruction()
    {
        REGISTRY = MODE_REGISTRAR.makeRegistry(ICustomRegistry.Builder::simple);
        LOGGER.info("Loaded chisel mode configuration.");
    }
}
