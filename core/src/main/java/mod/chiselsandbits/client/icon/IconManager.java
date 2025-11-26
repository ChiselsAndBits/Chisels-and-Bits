package mod.chiselsandbits.client.icon;

import mod.chiselsandbits.api.client.icon.IIconManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class IconManager implements IIconManager
{
    public static final  Identifier  TEXTURE_MAP_NAME = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/atlases/icons.png");
    private static final IconManager INSTANCE         = new IconManager();

    private static final Identifier ICON_SWAP       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "swap");
    private static final Identifier ICON_PLACE      = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "place");
    private static final Identifier ICON_UNDO       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "undo");
    private static final Identifier ICON_REDO       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "redo");
    private static final Identifier ICON_TRASH      = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "trash");
    private static final Identifier ICON_SORT       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sort");
    private static final Identifier ICON_ROLL_X     = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "roll_x");
    private static final Identifier ICON_ROLL_Z     = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "roll_z");
    private static final Identifier ICON_WHITE      = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "white");
    private static final Identifier BAG_MODE_FILTER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag_mode_filter");
    private static final Identifier BAG_MODE_NORMAL = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag_mode_normal");
    private static final Identifier BAG_MODE_FIRST  = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag_mode_first");
    private static final Identifier BAG_MODE_SECOND = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag_mode_second");


    public static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "icons");

    public static IconManager getInstance()
    {
        return INSTANCE;
    }

    private AtlasManager.AtlasConfig iconSpriteUploader = null;

    private IconManager()
    {
    }

    public void initialize(final Consumer<AtlasManager.AtlasConfig> consumer) {
        this.iconSpriteUploader = new AtlasManager.AtlasConfig(
            TEXTURE_MAP_NAME, ATLAS_ID,
            false
        );
        consumer.accept(this.iconSpriteUploader);
    }

    @Override
    public TextureAtlasSprite getIcon(final Identifier name) {
        return getAtlasOrThrow()
            .getSprite(name);
    }

    private @NotNull TextureAtlas getAtlasOrThrow()
    {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ATLAS_ID);
    }

    @Override
    public TextureAtlasSprite getSwapIcon() {
        return getIcon(ICON_SWAP);
    }

    @Override
    public TextureAtlasSprite getPlaceIcon() {
        return getIcon(ICON_PLACE);
    }

    @Override
    public TextureAtlasSprite getUndoIcon() {
        return getIcon(ICON_UNDO);
    }

    @Override
    public TextureAtlasSprite getRedoIcon() {
        return getIcon(ICON_REDO);
    }

    @Override
    public TextureAtlasSprite getTrashIcon() {
        return getIcon(ICON_TRASH);
    }

    @Override
    public TextureAtlasSprite getSortIcon() {
        return getIcon(ICON_SORT);
    }

    @Override
    public TextureAtlasSprite getRollXIcon() {
        return getIcon(ICON_ROLL_X);
    }

    @Override
    public TextureAtlasSprite getRollZIcon() {
        return getIcon(ICON_ROLL_Z);
    }

    @Override
    public TextureAtlasSprite getWhiteIcon() {
        return getIcon(ICON_WHITE);
    }

    @Override
    public TextureAtlasSprite getFilterBagModeIcon()
    {
        return getIcon(BAG_MODE_FILTER);
    }

    @Override
    public TextureAtlasSprite getNormalBagModeIcon()
    {
        return getIcon(BAG_MODE_NORMAL);
    }

    @Override
    public TextureAtlasSprite getBagPicksUpFirstIcon()
    {
        return getIcon(BAG_MODE_FIRST);
    }

    @Override
    public TextureAtlasSprite getBagPicksUpSecondIcon()
    {
        return getIcon(BAG_MODE_SECOND);
    }
}
