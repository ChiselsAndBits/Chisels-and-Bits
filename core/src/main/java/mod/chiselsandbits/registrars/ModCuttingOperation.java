package mod.chiselsandbits.registrars;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperation;
import mod.chiselsandbits.api.util.constants.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModCuttingOperation {
	private static final Logger                                   LOGGER              = LogManager.getLogger();
	private static final ICustomRegistrar<ICuttingOperation> OPERATION_REGISTRAR = ICustomRegistrar.create(ICuttingOperation.class, Constants.MOD_ID);

	public static final Supplier<ICustomRegistry<ICuttingOperation>> REGISTRY_SUPPLIER = OPERATION_REGISTRAR.makeRegistry(
			ICustomRegistry.Builder::simple
	);

	private ModCuttingOperation()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ModCuttingOperation. This is a utility class");
	}

	public static void onModConstruction()

	{
		LOGGER.info("Loaded modification operation configuration.");
	}
}
