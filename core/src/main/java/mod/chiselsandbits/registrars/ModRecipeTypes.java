package mod.chiselsandbits.registrars;

import com.communi.suggestu.scena.core.registries.deferred.IRegistrar;
import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.recipe.modificationtable.ModificationTableRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipeTypes {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final IRegistrar<RecipeType<?>> REGISTRAR = IRegistrar.create(Registries.RECIPE_TYPE, Constants.MOD_ID);

	public static IRegistryObject<RecipeType<ModificationTableRecipe>> MODIFICATION_TABLE = REGISTRAR.register("modification_table", () -> new RecipeType<>() {
			@Override
			public String toString() {
				return Constants.MOD_ID + ":modification_table";
			}
	}

	);

	public static void onModConstruction() {
		LOGGER.info("Registering recipe types.");
	}
}
