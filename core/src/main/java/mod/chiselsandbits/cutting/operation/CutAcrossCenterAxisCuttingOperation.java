package mod.chiselsandbits.cutting.operation;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import com.google.common.collect.ImmutableList;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperation;
import mod.chiselsandbits.api.cutting.operation.ICuttingOperationGroup;
import mod.chiselsandbits.api.multistate.accessor.IAreaAccessor;
import mod.chiselsandbits.api.multistate.accessor.IStateEntryInfo;
import mod.chiselsandbits.api.multistate.mutator.IGenerallyModifiableAreaMutator;
import mod.chiselsandbits.api.multistate.mutator.IMutableStateEntryInfo;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.multistate.mutator.MutatorFactory;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CutAcrossCenterAxisCuttingOperation extends AbstractCustomRegistryEntry implements ICuttingOperation
{

    private final Direction.Axis axis;

    public CutAcrossCenterAxisCuttingOperation(final Direction.Axis axis) {this.axis = axis;}

    @Override
    public Collection<IAreaAccessor> apply(final IAreaAccessor source)
    {
        final AABB box = source.getBoundingBox();
        final Vec3 size = new Vec3(box.getXsize(), box.getYsize(), box.getZsize());

        final IGenerallyModifiableAreaMutator preAxisMutator = MutatorFactory.getInstance().clonedFromAccessor(source);
        final IGenerallyModifiableAreaMutator postAxisMutator = MutatorFactory.getInstance().clonedFromAccessor(source);

        final Function<Vec3, Double> axisValueSelectorFunction = vec3 -> vec3.get(axis);
        final double sizeAxisSeparator = size.get(axis);

        final Predicate<IStateEntryInfo> preAxisPredicate = (info) -> axisValueSelectorFunction.apply(info.getStartPoint()) < sizeAxisSeparator;
        final Predicate<IStateEntryInfo> postAxisPredicate = (info) -> axisValueSelectorFunction.apply(info.getEndPoint()) >= sizeAxisSeparator;

        preAxisMutator
          .mutableStream()
          .filter(preAxisPredicate)
          .forEach(IMutableStateEntryInfo::clear);
        postAxisMutator
          .mutableStream()
          .filter(postAxisPredicate)
          .forEach(IMutableStateEntryInfo::clear);

        return ImmutableList.of(preAxisMutator, postAxisMutator);
    }

    @Override
    public @NotNull Optional<ICuttingOperationGroup> getGroup()
    {
        return Optional.empty();
    }

    @Override
    public Component getDisplayName()
    {
        return switch (axis)
                 {
                     case X -> LocalStrings.PatternCuttingAcrossXAxis.getText();
                     case Y -> LocalStrings.PatternCuttingAcrossYAxis.getText();
                     case Z -> LocalStrings.PatternCuttingAcrossZAxis.getText();
                 };
    }

    @Override
    public ResourceLocation getIcon()
    {
        return null;
    }
}
