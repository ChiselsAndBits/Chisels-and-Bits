package mod.chiselsandbits.utils;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;

public class CommandUtils
{
	private CommandUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: CommandUtils. This is a utility class");
	}

	public static boolean hasArgument(final CommandContext<CommandSourceStack> context, final String name) {
		try {
			return context.getArgument(name, Object.class) != null;
		} catch (IllegalArgumentException iae) {
			return false;
		}
	}
}
