package mod.chiselsandbits.api.util.constants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.resources.Identifier;

/**
 * Compile-time constants shared by the Chisels &amp; Bits API: mod identifiers, resource/datapack directory paths, and chisel tick tunables.
 */
public final class Constants {
	private Constants() {
		throw new IllegalStateException("Tried to initialize: Constants but this is a Utility class.");
	}

	/**
	 * Namespace / mod identifier used to namespace all Chisels &amp; Bits resources.
	 */
	public static final String MOD_ID = "chiselsandbits";
	/**
	 * Human-readable display name of the mod.
	 */
	public static final String MOD_NAME = "Chisels & Bits";
	/**
	 * Version string for the mod (substituted from "%VERSION%" at build time).
	 */
	public static final String MOD_VERSION = "%VERSION%";

	/**
	 * Resource location of the custom model loader used to render interactable (tile) blocks.
	 */
	public static final String INTERACTABLE_MODEL_LOADER = Constants.MOD_ID + ":interactable_model";

	/**
	 * Paths and a shared Gson instance used by the data generator to emit resources into the resource pack and datapacks.
	 */
	public static class DataGenerator {
		/**
		 * Creates access to the shared resource-generation constants.
		 */
		public DataGenerator() { }

		/**
		 * Shared Gson configured for pretty-printed, non-HTML-escaped JSON output.
		 */
		public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		/**
		 * Path to the English (en_us) language file inside the mod resource pack.
		 */
		public static final String EN_US_LANG = "assets/" + Constants.MOD_ID + "/lang/en_us.json";
		/**
		 * Directory for item model JSON files within the mod resource pack.
		 */
		public static final String ITEM_MODEL_DIR = "assets/" + Constants.MOD_ID + "/models/item/";
		private static final String DATAPACK_DIR = "data/" + MOD_ID + "/";
		private static final String MINECRAFT_DATAPACK_DIR = "data/" + "minecraft" + "/";
		private static final String FORGE_DATAPACK_DIR = "data/forge" + "/";
		/**
		 * Directory for recipe JSON files in the mod datapack.
		 */
		public static final String RECIPES_DIR = DATAPACK_DIR + "recipes/";
		/**
		 * Root directory for this mod's datapack tag definitions.
		 */
		public static final String TAGS_DIR = DATAPACK_DIR + "tags/";
		/**
		 * Directory containing vanilla Minecraft tag definitions.
		 */
		public static final String MINECRAFT_TAGS_DIR = MINECRAFT_DATAPACK_DIR + "tags/";
		/**
		 * Directory for the mod's block tag definitions (datapack tags/blocks).
		 */
		public static final String BLOCK_TAGS_DIR = TAGS_DIR + "blocks/";
		/**
		 * Directory for the mod's item tag definitions (datapack tags/items).
		 */
		public static final String ITEM_TAGS_DIR = TAGS_DIR + "items/";
		/**
		 * Directory for item tag definitions in vanilla Minecraft's datapack namespace.
		 */
		public static final String MINECRAFT_ITEM_TAGS_DIR = MINECRAFT_TAGS_DIR + "items/";
		/**
		 * Directory for Forge-specific datapack tag definitions.
		 */
		public static final String FORGE_TAGS_DIR = FORGE_DATAPACK_DIR + "tags/";
		/**
		 * Directory for Forge-specific item tag definitions (datapack tags/items).
		 */
		public static final String FORGE_ITEM_TAGS_DIR = FORGE_TAGS_DIR + "items/";
		/**
		 * Directory for block loot-table JSON files in the mod datapack.
		 */
		public static final String LOOT_TABLES_DIR = DATAPACK_DIR + "loot_tables/blocks";
		private static final String RESOURCEPACK_DIR = "assets/" + MOD_ID + "/";
		/**
		 * Directory for block-state JSON files within the mod resource pack.
		 */
		public static final String BLOCKSTATE_DIR = RESOURCEPACK_DIR + "blockstates/";
		/**
		 * Directory for language files under the mod resource pack's lang/config path.
		 */
		public static final String CONFIG_LANG_DIR = RESOURCEPACK_DIR + "lang/config/";
		/**
		 * Resource location of the model used to render a chiseled block.
		 */
		public static final Identifier CHISELED_BLOCK_MODEL = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/chiseled");
		/**
		 * Resource location of the model used to render a chiseled printer.
		 */
		public static final Identifier CHISELED_PRINTER_MODEL = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/chiseled_printer");
	}

	/**
	 * Minimum number of ticks required between successful chisel usages.
	 */
	public static final int TICKS_BETWEEN_CHISEL_USAGE = 3;

	/**
	 * Number of ticks to wait between reported chisel usage errors.
	 */
	public static final int TICKS_BETWEEN_CHISEL_ERRORS = 15;
}
