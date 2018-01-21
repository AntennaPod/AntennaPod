package de.danoeh.antennapod.core.util;

import android.support.v4.util.ArrayMap;

import java.nio.charset.Charset;

public class LangUtils {
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final ArrayMap<String, String> languages;
	static {
		languages = new ArrayMap<>();
		languages.put("af", "Afrikaans");
		languages.put("sq", "Albanian");
		languages.put("sq", "Albanian");
		languages.put("eu", "Basque");
		languages.put("be", "Belarusian");
		languages.put("bg", "Bulgarian");
		languages.put("ca", "Catalan");
		languages.put("Chinese (Simplified)", "zh-cn");
		languages.put("Chinese (Traditional)", "zh-tw");
		languages.put("hr", "Croatian");
		languages.put("cs", "Czech");
		languages.put("da", "Danish");
		languages.put("nl", "Dutch");
		languages.put("nl-be", "Dutch (Belgium)");
		languages.put("nl-nl", "Dutch (Netherlands)");
		languages.put("en", "English");
		languages.put("en-au", "English (Australia)");
		languages.put("en-bz", "English (Belize)");
		languages.put("en-ca", "English (Canada)");
		languages.put("en-ie", "English (Ireland)");
		languages.put("en-jm", "English (Jamaica)");
		languages.put("en-nz", "English (New Zealand)");
		languages.put("en-ph", "English (Phillipines)");
		languages.put("en-za", "English (South Africa)");
		languages.put("en-tt", "English (Trinidad)");
		languages.put("en-gb", "English (United Kingdom)");
		languages.put("en-us", "English (United States)");
		languages.put("en-zw", "English (Zimbabwe)");
		languages.put("et", "Estonian");
		languages.put("fo", "Faeroese");
		languages.put("fi", "Finnish");
		languages.put("fr", "French");
		languages.put("fr-be", "French (Belgium)");
		languages.put("fr-ca", "French (Canada)");
		languages.put("fr-fr", "French (France)");
		languages.put("fr-lu", "French (Luxembourg)");
		languages.put("fr-mc", "French (Monaco)");
		languages.put("fr-ch", "French (Switzerland)");
		languages.put("gl", "Galician");
		languages.put("gd", "Gaelic");
		languages.put("de", "German");
		languages.put("de-at", "German (Austria)");
		languages.put("de-de", "German (Germany)");
		languages.put("de-li", "German (Liechtenstein)");
		languages.put("de-lu", "German (Luxembourg)");
		languages.put("de-ch", "German (Switzerland)");
		languages.put("el", "Greek");
		languages.put("haw", "Hawaiian");
		languages.put("hu", "Hungarian");
		languages.put("is", "Icelandic");
		languages.put("in", "Indonesian");
		languages.put("ga", "Irish");
		languages.put("it", "Italian");
		languages.put("it-it", "Italian (Italy)");
		languages.put("it-ch", "Italian (Switzerland)");
		languages.put("ja", "Japanese");
		languages.put("ko", "Korean");
		languages.put("mk", "Macedonian");
		languages.put("no", "Norwegian");
		languages.put("pl", "Polish");
		languages.put("pt", "Portugese");
		languages.put("pt-br", "Portugese (Brazil)");
		languages.put("pt-pt", "Portugese (Portugal");
		languages.put("ro", "Romanian");
		languages.put("ro-mo", "Romanian (Moldova)");
		languages.put("ro-ro", "Romanian (Romania");
		languages.put("ru", "Russian");
		languages.put("ru-mo", "Russian (Moldova)");
		languages.put("ru-ru", "Russian (Russia)");
		languages.put("sr", "Serbian");
		languages.put("sk", "Slovak");
		languages.put("sl", "Slovenian");
		languages.put("es", "Spanish");
		languages.put("es-ar", "Spanish (Argentinia)");
		languages.put("es=bo", "Spanish (Bolivia)");
		languages.put("es-cl", "Spanish (Chile)");
		languages.put("es-co", "Spanish (Colombia)");
		languages.put("es-cr", "Spanish (Costa Rica)");
		languages.put("es-do", "Spanish (Dominican Republic)");
		languages.put("es-ec", "Spanish (Ecuador)");
		languages.put("es-sv", "Spanish (El Salvador)");
		languages.put("es-gt", "Spanish (Guatemala)");
		languages.put("es-hn", "Spanish (Honduras)");
		languages.put("es-mx", "Spanish (Mexico)");
		languages.put("es-ni", "Spanish (Nicaragua)");
		languages.put("es-pa", "Spanish (Panama)");
		languages.put("es-py", "Spanish (Paraguay)");
		languages.put("es-pe", "Spanish (Peru)");
		languages.put("es-pr", "Spanish (Puerto Rico)");
		languages.put("es-es", "Spanish (Spain)");
		languages.put("es-uy", "Spanish (Uruguay)");
		languages.put("es-ve", "Spanish (Venezuela)");
		languages.put("sv", "Swedish");
		languages.put("sv-fi", "Swedish (Finland)");
		languages.put("sv-se", "Swedish (Sweden)");
		languages.put("tr", "Turkish");
		languages.put("uk", "Ukranian");
	}
	
	/** Finds language string for key or returns the language key if it can't be found. */
	public static String getLanguageString(String key) {
		String language = languages.get(key);
		if (language != null) {
			return language;
		} else {
			return key;
		}
	}
}
