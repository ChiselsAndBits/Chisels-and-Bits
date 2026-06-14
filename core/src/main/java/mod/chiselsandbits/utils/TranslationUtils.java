package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.Constants;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TranslationUtils
{
	private TranslationUtils()
	{
		throw new IllegalStateException("Can not instantiate an instance of: TranslationUtils. This is a utility class");
	}

	public static MutableComponent build(final String keySuffix, final Object... args) {
		return Component.translatable(String.format("mod.%s.%s", Constants.MOD_ID, keySuffix), args);
	}

	public static MutableComponent build(final LocalStrings chiselSupportTagBlackListed, final Object... args)
	{
		return Component.translatable(chiselSupportTagBlackListed.toString(), args);
	}
}
