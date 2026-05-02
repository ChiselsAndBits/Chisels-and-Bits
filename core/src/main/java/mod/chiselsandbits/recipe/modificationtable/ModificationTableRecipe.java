package mod.chiselsandbits.recipe.modificationtable;

import com.communi.suggestu.scena.core.registries.ICustomRegistryEntry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.registrars.ModRecipeSerializers;
import mod.chiselsandbits.registrars.ModRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record ModificationTableRecipe(IModificationOperation operation) implements Recipe<CraftingInput>
{
    public static final MapCodec<ModificationTableRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Identifier.CODEC.xmap(
                resourceLocation -> IModificationOperation.getRegistry().get(resourceLocation).orElseThrow(),
                ICustomRegistryEntry::getRegistryName
            ).fieldOf(NbtConstants.OPERATION).forGetter(ModificationTableRecipe::operation)
        ).apply(instance, ModificationTableRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ModificationTableRecipe> STREAM_CODEC = StreamCodec.composite(
        IModificationOperation.getRegistry().byNameStreamCodec(),
        ModificationTableRecipe::operation,
        ModificationTableRecipe::new
    );

    @Override
    public boolean matches(final CraftingInput inv, final @NotNull Level worldIn)
    {
        return inv.getItem(0).getItem() instanceof IPatternItem && !(inv.getItem(0).getItem() instanceof IMultiUsePatternItem);
    }

    @Override
    public @NonNull ItemStack assemble(final CraftingInput input)
    {
        return getAppliedSnapshot(input).toItemStack().toPatternStack();
    }

    @Override
    public boolean showNotification()
    {
        return false;
    }

    @Override
    public @NonNull String group()
    {
        return "";
    }

    @Override
    public @NonNull RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer()
    {
        return ModRecipeSerializers.MODIFICATION_TABLE.get();
    }

    @Override
    public @NonNull RecipeType<? extends Recipe<CraftingInput>> getType()
    {
        return ModRecipeTypes.MODIFICATION_TABLE.get();
    }

    @Override
    public @NonNull PlacementInfo placementInfo()
    {
        return PlacementInfo.NOT_PLACEABLE;
    }

    public @NotNull ItemStack getCraftingBlockResult(final CraftingInput inv)
    {
        return getAppliedSnapshot(inv).toItemStack().toBlockStack();
    }

    public @NotNull IMultiStateSnapshot getAppliedSnapshot(final CraftingInput inv)
    {
        final ItemStack multiStateStack = inv.getItem(0);
        if (multiStateStack.isEmpty())
        {
            return EmptySnapshot.INSTANCE;
        }

        if (!(multiStateStack.getItem() instanceof final IMultiStateItem item))
        {
            return EmptySnapshot.INSTANCE;
        }

        final IMultiStateItemStack multiStateItemStack = item.createItemStack(multiStateStack);
        final IMultiStateSnapshot snapshot = multiStateItemStack.createSnapshot().clone();

        operation().apply(snapshot);

        return snapshot;
    }

    public Component getDisplayName()
    {
        return Component.translatable(
            Objects.requireNonNull(this.operation().getRegistryName()).getNamespace() + ".recipes.chisel.pattern.modification." + this.operation().getRegistryName().getPath());
    }

    @Override
    public @NonNull RecipeBookCategory recipeBookCategory()
    {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
