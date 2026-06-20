package mod.chiselsandbits.registrars;

import java.util.function.Supplier;

import com.communi.suggestu.scena.core.registries.ICustomRegistry;
import com.communi.suggestu.scena.core.registries.deferred.ICustomRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.pattern.placement.IPatternPlacementType;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.pattern.placement.CarvePatternPlacementType;
import mod.chiselsandbits.pattern.placement.ImposePatternPlacementType;
import mod.chiselsandbits.pattern.placement.MergePatternPlacementType;
import mod.chiselsandbits.pattern.placement.PlacePatternPlacementType;
import mod.chiselsandbits.pattern.placement.RemovalPatternPlacementType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModPatternPlacementTypes {
	private static final Logger                                  LOGGER         = LogManager.getLogger();
	private static final ICustomRegistrar<IPatternPlacementType> TYPE_REGISTRAR = ICustomRegistrar.create(IPatternPlacementType.class, Constants.MOD_ID);

	public static final Supplier<ICustomRegistry<IPatternPlacementType>>
	  REGISTRY_SUPPLIER = TYPE_REGISTRAR.makeRegistry(ICustomRegistry.Builder::simple);

	public static final IRegistryObject<IPatternPlacementType> PLACEMENT  = TYPE_REGISTRAR.register(
	  "placement", PlacePatternPlacementType::new
	);
	public static final IRegistryObject<IPatternPlacementType> REMOVAL    = TYPE_REGISTRAR.register(
	  "removal", RemovalPatternPlacementType::new
	);
	public static final IRegistryObject<IPatternPlacementType> IMPOSEMENT = TYPE_REGISTRAR.register(
	  "imposement", ImposePatternPlacementType::new
	);
	public static final IRegistryObject<IPatternPlacementType> MERGE      = TYPE_REGISTRAR.register(
	  "merge", MergePatternPlacementType::new
	);
	public static final IRegistryObject<IPatternPlacementType> CARVING    = TYPE_REGISTRAR.register(
	  "carving", CarvePatternPlacementType::new
	);

	private ModPatternPlacementTypes()

	{
		throw new IllegalStateException("Can not instantiate an instance of: ModPatternPlacementTypes. This is a utility class");
	}

	public static void onModConstruction()

	{
		LOGGER.info("Loaded pattern placement type configuration.");
	}
}
