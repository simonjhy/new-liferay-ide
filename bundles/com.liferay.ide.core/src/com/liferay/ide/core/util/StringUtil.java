/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Kuo Zhang
 * @author Terry Jia
 */
public class StringUtil {

	public static boolean contains(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}

		return s1.contains(s2);
	}

	public static boolean containsAny(String object, String... anyArray) {
		if (object == null) {
			return false;
		}

		return Stream.of(
			anyArray
		).anyMatch(
			anyString -> object.contains(anyString)
		);
	}

	public static boolean endsWith(Object o, String s2) {
		if ((o == null) || (s2 == null)) {
			return false;
		}

		String s1 = o.toString();

		return s1.endsWith(s2);
	}

	public static boolean endsWith(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}

		return s1.endsWith(s2);
	}

	public static boolean equals(String s1, Object o) {
		if ((s1 == null) || (o == null)) {
			return false;
		}

		return s1.equals(o.toString());
	}

	public static boolean equals(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}

		return s1.equals(s2);
	}

	public static boolean equalsIgnoreCase(String s1, Object o) {
		if ((s1 == null) || (o == null)) {
			return false;
		}

		return s1.equalsIgnoreCase(o.toString());
	}

	public static boolean equalsIgnoreCase(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}

		return s1.equalsIgnoreCase(s2);
	}

	public static byte[] getBytes(String s) {
		if (s == null) {
			return new byte[0];
		}

		return s.getBytes();
	}

	public static boolean isQuoted(String string) {
		if ((string == null) || (string.length() < 2)) {
			return false;
		}

		int lastIndex = string.length() - 1;

		char firstChar = string.charAt(0);

		char lastChar = string.charAt(lastIndex);

		if (((firstChar == StringPool.SINGLE_QUOTE_CHAR) && (lastChar == StringPool.SINGLE_QUOTE_CHAR)) ||
			((firstChar == StringPool.DOUBLE_QUOTE_CHAR) && (lastChar == StringPool.DOUBLE_QUOTE_CHAR))) {

			return true;
		}

		return false;
	}

	public static int length(String s) {
		if (s == null) {
			return 0;
		}

		return s.length();
	}

	public static String merge(String[] array, String delimiter) {
		if (array == null) {
			return null;
		}

		if (array.length == 0) {
			return StringPool.BLANK;
		}

		StringBuilder sb = new StringBuilder(2 * array.length - 1);

		for (int i = 0; i < array.length; i++) {
			if (i != 0) {
				sb.append(delimiter);
			}

			sb.append(array[i]);
		}

		return sb.toString();
	}

	public static String replace(String content, String source, String target) {
		if (content == null) {
			return null;
		}

		int length = content.length();
		int position = 0;
		int previous = 0;
		int spacer = source.length();

		StringBuffer sb = new StringBuffer();

		while (((position + spacer - 1) < length) && (content.indexOf(source, position) > -1)) {
			position = content.indexOf(source, previous);

			sb.append(content.substring(previous, position));

			sb.append(target);

			position += spacer;
			previous = position;
		}

		sb.append(content.substring(position, content.length()));

		return sb.toString();
	}

	public static boolean startsWith(String s1, String s2) {
		if ((s1 == null) || (s2 == null)) {
			return false;
		}

		return s1.startsWith(s2);
	}

	public static String[] stringToArray(String input, String delimiter) {
		Stream<String> inputStream = Arrays.stream(input.split(delimiter));

		return inputStream.map(
			String::trim
		).toArray(
			String[]::new
		);
	}

	public static List<String> stringToList(String input, String delimiter) {
		Stream<String> inputStream = Arrays.stream(input.split(Pattern.quote(delimiter)));

		return inputStream.map(
			String::trim
		).collect(
			Collectors.toList()
		);
	}

	public static String toLowerCase(String s) {
		if (s == null) {
			return "";
		}

		return s.toLowerCase();
	}

	public static String toLowerCase(StringBuilder sb) {
		if (sb == null) {
			return "";
		}

		String string = sb.toString();

		return string.toLowerCase();
	}

	public static String toString(Object o) {
		if (o != null) {
			return o.toString();
		}

		return "";
	}

	public static String toUpperCase(String s) {
		if (s == null) {
			return "";
		}

		return s.toUpperCase();
	}

	public static String trim(String string) {
		if (string == null) {
			return "";
		}

		return string.trim();
	}

	public static String trim(StringBuffer sb) {
		if (sb == null) {
			return "";
		}

		String string = sb.toString();

		return string.trim();
	}

	public static String trim(StringBuilder sb) {
		if (sb == null) {
			return "";
		}

		String string = sb.toString();

		return string.trim();
	}

}