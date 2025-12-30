package com.github.kilianB;

public class StringUtil {
	public static boolean isEscaped(String s) {
		return s.contains("&amp;") || s.contains("&lt;") || s.contains("&gt;") || s.contains("&quot;")
				|| s.contains("&apos;");
	}
}
