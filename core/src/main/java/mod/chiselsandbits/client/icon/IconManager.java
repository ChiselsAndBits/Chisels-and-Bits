package mod.chiselsandbits.client.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.chiselsandbits.api.client.icon.IIconManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class IconManager implements IIconManager
{
    public static final ResourceLocation TEXTURE_MAP_NAME = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/atlases/icons.png");
    private static final IconManager INSTANCE = new IconManager();

    private static final ResourceLocation ICON_SWAP = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "swap");
    private static final ResourceLocation ICON_PLACE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "place");
    private static final ResourceLocation ICON_UNDO = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "undo");
    private static final ResourceLocation ICON_REDO = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "redo");
    private static final ResourceLocation ICON_TRASH = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "trash");
    private static final ResourceLocation ICON_SORT = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sort");
    private static final ResourceLocation ICON_ROLL_X = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "roll_x");
    private static final ResourceLocation ICON_ROLL_Z = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "roll_z");
    private static final ResourceLocation ICON_WHITE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "white");
    public static final ResourceLocation ATLAS_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "icons");

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
    public TextureAtlasSprite getIcon(final ResourceLocation name) {
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
}
