package mod.chiselsandbits.recipe.modificationtable;

import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import com.communi.suggestu.scena.core.registries.SimpleCustomRegistryEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ModificationTableRecipeSerializer extends SimpleCustomRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ModificationTableRecipe>
{

    private static final MapCodec<ModificationTableRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
        ResourceLocation.CODEC.xmap(
                resourceLocation -> IModificationOperation.getRegistry().get(resourceLocation).orElseThrow(),
                ICustomRegistryEntry::getRegistryName
        ).fieldOf(NbtConstants.OPERATION).forGetter(ModificationTableRecipe::getOperation)
      ).apply(instance, ModificationTableRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ModificationTableRecipe> STREAM_CODEC = StreamCodec.composite(
            IModificationOperation.getRegistry().byNameStreamCodec(),
            ModificationTableRecipe::getOperation,
            ModificationTableRecipe::new
    );

    public static final StreamCodec<? super FriendlyByteBuf, ModificationTableRecipe> NETWORK_CODEC = StreamCodec.composite(
        IModificationOperation.getRegistry().byNameStreamCodec(),
        ModificationTableRecipe::getOperation,
        ModificationTableRecipe::new
    );

    @Override
    public @NotNull MapCodec<ModificationTableRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ModificationTableRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
