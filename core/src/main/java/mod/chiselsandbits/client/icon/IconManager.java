package mod.chiselsandbits.client.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.chiselsandbits.api.client.icon.IIconManager;
import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;

import static mod.chiselsandbits.client.icon.IconSpriteUploader.TEXTURE_MAP_NAME;

public class IconManager implements IIconManager
{
    private static final IconManager INSTANCE = new IconManager();

    private static final ResourceLocation ICON_SWAP = new ResourceLocation(Constants.MOD_ID, "icon/swap");
    private static final ResourceLocation ICON_PLACE = new ResourceLocation(Constants.MOD_ID, "icon/place");
    private static final ResourceLocation ICON_UNDO = new ResourceLocation(Constants.MOD_ID, "icon/undo");
    private static final ResourceLocation ICON_REDO = new ResourceLocation(Constants.MOD_ID, "icon/redo");
    private static final ResourceLocation ICON_TRASH = new ResourceLocation(Constants.MOD_ID, "icon/trash");
    private static final ResourceLocation ICON_SORT = new ResourceLocation(Constants.MOD_ID, "icon/sort");
    private static final ResourceLocation ICON_ROLL_X = new ResourceLocation(Constants.MOD_ID, "icon/roll_x");
    private static final ResourceLocation ICON_ROLL_Z = new ResourceLocation(Constants.MOD_ID, "icon/roll_z");
    private static final ResourceLocation ICON_WHITE = new ResourceLocation(Constants.MOD_ID, "icon/white");

    public static IconManager getInstance()
    {
        return INSTANCE;
    }

    private IconSpriteUploader iconSpriteUploader = null;

    private IconManager()
    {
    }

    public void initialize() {
        this.iconSpriteUploader = new IconSpriteUploader();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager reloadableResourceManager) {
            reloadableResourceManager.registerReloadListener(iconSpriteUploader);
        }
    }

    @Override
    public TextureAtlasSprite getIcon(final ResourceLocation name) {
        if (this.iconSpriteUploader == null)
            throw new IllegalStateException("Tried to get icon too early.");

        return this.iconSpriteUploader.getSprite(name);
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
    public void bindTexture()
    {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE_MAP_NAME);
    }
}
