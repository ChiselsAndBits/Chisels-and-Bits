package mod.chiselsandbits.api.item.withmode;

import java.util.Optional;

import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.util.IWithDisplayName;

/**
 * A mode of a given tool, parameterized by the type of {@link IToolModeGroup} it belongs to.
 *
 * @param <G> The type of tool mode group that this mode is part of.
 */
public interface IToolMode<G extends IToolModeGroup> extends IWithDisplayName, IRenderableMode {
	/**
	 * An optional which indicates the group this tool mode
	 * is part of.
	 *
	 * @return The optional tool mode group.
	 */
	Optional<G> getGroup();
}
