package mod.chiselsandbits.registrars;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.modification.operation.MirrorOverAxisModificationOperation;
import mod.chiselsandbits.modification.operation.RotateAroundAxisModificationOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.Direction;

public final class ModModificationOperation
{
	private static final Logger                                   LOGGER              = LogManager.getLogger();
	private static final ICustomRegistrar<IModificationOperation> OPERATION_REGISTRAR = ICustomRegistrar.create(IModificationOperation.class, Constants.MOD_ID);

	public static final Supplier<ICustomRegistry<IModificationOperation>> REGISTRY_SUPPLIER = OPERATION_REGISTRAR.makeRegistry(
	  ICustomRegistry.Builder::simple
	);

	public static final IRegistryObject<IModificationOperation> ROTATE_AROUND_X = OPERATION_REGISTRAR.register(
	  "rotate_around_x", () -> RotateAroundAxisModificationOperation.Builder.create().withAxis(Direction.Axis.X).build()
	);
	public static final IRegistryObject<IModificationOperation> ROTATE_AROUND_Y = OPERATION_REGISTRAR.register(
	  "rotate_around_y", () -> RotateAroundAxisModificationOperation.Builder.create().withAxis(Direction.Axis.Y).build()
	);
	public static final IRegistryObject<IModificationOperation> ROTATE_AROUND_Z = OPERATION_REGISTRAR.register(
	  "rotate_around_z", () -> RotateAroundAxisModificationOperation.Builder.create().withAxis(Direction.Axis.Z).build()
	);
	public static final IRegistryObject<IModificationOperation> MIRROR_OVER_X   = OPERATION_REGISTRAR.register(
	  "mirror_over_x", () -> MirrorOverAxisModificationOperation.Builder.create().withAxis(Direction.Axis.X).build()
	);
	public static final IRegistryObject<IModificationOperation> MIRROR_OVER_Y   = OPERATION_REGISTRAR.register(
	  "mirror_over_y", () -> MirrorOverAxisModificationOperation.Builder.create().withAxis(Direction.Axis.Y).build()
	);
	public static final IRegistryObject<IModificationOperation> MIRROR_OVER_Z   = OPERATION_REGISTRAR.register(
	  "mirror_over_z", () -> MirrorOverAxisModificationOperation.Builder.create().withAxis(Direction.Axis.Z).build()
	);

	private ModModificationOperation()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ModModificationOperation. This is a utility class");
	}

	public static void onModConstruction()

	{
		LOGGER.info("Loaded modification operation configuration.");
	}
}
