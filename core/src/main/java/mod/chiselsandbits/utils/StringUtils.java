package mod.chiselsandbits.utils;

import java.util.Iterator;

public class StringUtils {
	private StringUtils() {
		throw new IllegalStateException("Can not instantiate an instance of: StringUtils.This is a utility class");
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

    public static String pad(String s, int i, char c) {
        if (s.length() >= i) {
			return s;
		}

		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < i) {
			sb.append(c);
		}
		return sb.toString();
    }
}
