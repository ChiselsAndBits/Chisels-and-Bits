package mod.chiselsandbits.api.item.withmode;

import java.util.Optional;

import mod.chiselsandbits.api.item.withmode.group.IToolModeGroup;
import mod.chiselsandbits.api.util.IWithDisplayName;

/**
 * A mode of a given tool.
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
