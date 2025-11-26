package mod.chiselsandbits.modification.operation;

import com.communi.suggestu.scena.core.registries.AbstractCustomRegistryEntry;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.modification.operation.IModificationOperationGroup;
import mod.chiselsandbits.api.multistate.mutator.IGenerallyModifiableAreaMutator;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.registrars.ModModificationOperationGroups;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RotateAroundAxisModificationOperation extends AbstractCustomRegistryEntry implements IModificationOperation
{
    private final Direction.Axis axis;

    private RotateAroundAxisModificationOperation(final Direction.Axis axis) {this.axis = axis;}

    @Override
    public void apply(final IGenerallyModifiableAreaMutator source)
    {
        source.rotate(axis);
    }

    @Override
    public Identifier getIcon()
    {
        return ModModificationOperationGroups.ROTATE.getIcon();
    }

    @Override
    public @NotNull Optional<IModificationOperationGroup> getGroup()
    {
        return Optional.of(ModModificationOperationGroups.ROTATE);
    }

    @Override
    public Component getDisplayName()
    {
        return switch (axis)
                 {
                     case X -> LocalStrings.PatternModificationAcrossXAxis.getText();
                     case Y -> LocalStrings.PatternModificationAcrossYAxis.getText();
                     case Z -> LocalStrings.PatternModificationAcrossZAxis.getText();
                 };
    }

    public static final class Builder
    {
        private Direction.Axis axis;

        private Builder() {}

        public static Builder create() { return new Builder(); }

        public Builder withAxis(Direction.Axis axis)
        {
            this.axis = axis;
            return this;
        }

        public RotateAroundAxisModificationOperation build() { return new RotateAroundAxisModificationOperation(axis); }
    }
}
