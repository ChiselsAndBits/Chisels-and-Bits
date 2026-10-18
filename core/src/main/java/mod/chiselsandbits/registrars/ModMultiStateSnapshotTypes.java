package mod.chiselsandbits.registrars;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshotType;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.multistate.snapshot.MultiStateSnapshotTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModMultiStateSnapshotTypes {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ICustomRegistrar<IMultiStateSnapshotType> REGISTRAR = ICustomRegistrar.create(IMultiStateSnapshotType.class, Constants.MOD_ID);

	public static final Supplier<ICustomRegistry<IMultiStateSnapshotType>
			> REGISTRY_SUPPLIER = REGISTRAR.makeRegistry(ICustomRegistry.Builder::simple);

	public static final IRegistryObject<MultiStateSnapshotTypes> EMPTY = REGISTRAR.register("empty", () -> MultiStateSnapshotTypes.EMPTY);
	public static final IRegistryObject<MultiStateSnapshotTypes> MULTI_BLOCK = REGISTRAR.register("multi_block", () -> MultiStateSnapshotTypes.MULTI_BLOCK);
	public static final IRegistryObject<MultiStateSnapshotTypes> SIMPLE = REGISTRAR.register("simple", () -> MultiStateSnapshotTypes.SIMPLE);

	private ModMultiStateSnapshotTypes() {
		throw new IllegalStateException("Can not instantiate an instance of: ModMultiStateSnapshotTypes.This is a utility class");
	}

	public static void onModConstruction() {
		LOGGER.info("Loaded change type configuration.");
	}
}
