package de.podfetcher.syndication.util;

import java.util.HashMap;

/** Unescapes HTML */
public class HtmlUnescaper {
	private static HashMap<String, String> symbols;
	
	static {
		symbols.put("&nbsp", " ");
		symbols.put("&quot", "\"");
		symbols.put("&amp", "&");
		symbols.put("&lt", "<");
		symbols.put("&gt", ">");
		
	}
	
	public static String unescape(final String source) {
		String result = source;
		for (String key : symbols.keySet()) {
			result = result.replaceAll(key, symbols.get(key));
		}
		return result;
	}
}
