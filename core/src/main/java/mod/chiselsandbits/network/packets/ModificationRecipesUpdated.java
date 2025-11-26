package mod.chiselsandbits.network.packets;

import com.communi.suggestu.scena.core.dist.Dist;
import com.communi.suggestu.scena.core.dist.DistExecutor;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.container.ModificationTableContainer;
import mod.chiselsandbits.recipe.modificationtable.ModificationTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ModificationRecipesUpdated extends ModPacket
{
    public static final Identifier                                           ID   = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "modification_recipes_updated");
    public static final CustomPacketPayload.Type<ModificationRecipesUpdated> TYPE = new CustomPacketPayload.Type<>(ID);

    private final List<RecipeHolder<ModificationTableRecipe>> recipes;

    public ModificationRecipesUpdated(final List<RecipeHolder<ModificationTableRecipe>> recipes) {
        this.recipes = recipes;
    }

    public ModificationRecipesUpdated(final RegistryFriendlyByteBuf buf) {
        this.recipes = new ArrayList<>();
        readPayload(buf);
    }

    @Override
    public void writePayload(final RegistryFriendlyByteBuf buffer)
    {
        buffer.writeVarInt(
            this.recipes.size()
        );

        for (final RecipeHolder<ModificationTableRecipe> recipe : this.recipes)
        {
            RecipeHolder.STREAM_CODEC.encode(buffer, recipe);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readPayload(final RegistryFriendlyByteBuf buffer)
    {
        this.recipes.clear();

        final int count = buffer.readVarInt();
        for (int i = 0; i < count; i++)
        {
            this.recipes.add((RecipeHolder<ModificationTableRecipe>) RecipeHolder.STREAM_CODEC.decode(buffer));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @Override
    public void client()
    {
        DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> {
                if (Minecraft.getInstance().player != null &&
                    Minecraft.getInstance().player.containerMenu instanceof ModificationTableContainer modificationTableContainer) {
                    modificationTableContainer.setRecipes(this.recipes);
                }
            }
        );
    }
}
