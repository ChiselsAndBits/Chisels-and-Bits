package mod.chiselsandbits.utils;

import java.util.Iterator;

public class StringUtils
{
	private StringUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: StringUtils. This is a utility class");
	}

	public static String join(final String delimiter, final Iterator<String> source) {
		final StringBuilder sb = new StringBuilder();

		while (source.hasNext()) {
			sb.append(source.next());

			if (source.hasNext()) {
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}
}
