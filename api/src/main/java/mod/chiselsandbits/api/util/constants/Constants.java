package mod.chiselsandbits.api.util.constants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;

public final class Constants
{

    private Constants()
    {
        throw new IllegalStateException("Tried to initialize: Constants but this is a Utility class.");
    }

    public static final String MOD_ID = "chiselsandbits";
    public static final String MOD_NAME = "Chisels & Bits";
    public static final String MOD_VERSION = "%VERSION%";

    public static final String INTERACTABLE_MODEL_LOADER = Constants.MOD_ID + ":interactable_model";

    public static class DataGenerator {

        public static final  Gson   GSON                           = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        public static final  String EN_US_LANG                     = "assets/" + Constants.MOD_ID + "/lang/en_us.json";
        public static final  String ITEM_MODEL_DIR                 = "assets/" + Constants.MOD_ID + "/models/item/";
        private static final String DATAPACK_DIR                   = "data/" + MOD_ID + "/";
        private static final String MINECRAFT_DATAPACK_DIR         = "data/" + "minecraft" + "/";
        private static final String FORGE_DATAPACK_DIR             = "data/forge" + "/";
        public static final  String RECIPES_DIR                    = DATAPACK_DIR + "recipes/";
        public static final  String TAGS_DIR                       = DATAPACK_DIR + "tags/";
        public static final  String MINECRAFT_TAGS_DIR             = MINECRAFT_DATAPACK_DIR + "tags/";
        public static final  String BLOCK_TAGS_DIR                 = TAGS_DIR + "blocks/";
        public static final  String ITEM_TAGS_DIR                  = TAGS_DIR + "items/";
        public static final  String MINECRAFT_ITEM_TAGS_DIR        = MINECRAFT_TAGS_DIR + "items/";
        public static final  String FORGE_TAGS_DIR                 = FORGE_DATAPACK_DIR + "tags/";
        public static final String  FORGE_ITEM_TAGS_DIR            = FORGE_TAGS_DIR + "items/";
        public static final  String LOOT_TABLES_DIR                = DATAPACK_DIR + "loot_tables/blocks";
        private static final String RESOURCEPACK_DIR               = "assets/" + MOD_ID + "/";
        public static final  String BLOCKSTATE_DIR                 = RESOURCEPACK_DIR + "blockstates/";
        public static final String     CONFIG_LANG_DIR        = RESOURCEPACK_DIR + "lang/config/";
        public static final Identifier CHISELED_BLOCK_MODEL   = Identifier.fromNamespaceAndPath( Constants.MOD_ID, "block/chiseled" );
        public static final Identifier CHISELED_PRINTER_MODEL = Identifier.fromNamespaceAndPath( Constants.MOD_ID, "block/chiseled_printer" );
    }

    public static final int TICKS_BETWEEN_CHISEL_USAGE = 3;

    public static final int TICKS_BETWEEN_CHISEL_ERRORS = 15;
}
