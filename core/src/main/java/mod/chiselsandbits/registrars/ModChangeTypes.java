package mod.chiselsandbits.registrars;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.change.changes.IChangeType;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.change.changes.ChangeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModChangeTypes {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ICustomRegistrar<IChangeType> REGISTRAR = ICustomRegistrar.create(IChangeType.class, Constants.MOD_ID);

	public static final Supplier<ICustomRegistry<IChangeType>>
			REGISTRY_SUPPLIER = REGISTRAR.makeRegistry(ICustomRegistry.Builder::simple);

	public static final IRegistryObject<ChangeType> BIT = REGISTRAR.register("bit", () -> ChangeType.BIT);
	public static final IRegistryObject<ChangeType> COMBINED = REGISTRAR.register("combined", () -> ChangeType.COMBINED);

	private ModChangeTypes()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ModChangeTypes. This is a utility class");
	}

	public static void onModConstruction()

	{
		LOGGER.info("Loaded change type configuration.");
	}
}
