package com.github.kilianB.sonos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Utility functions to extract information returned from UPnP events
 * @author Kilian
 * @author vmichalak
 */
public class ParserHelper {

	// Hide the implicit public constructor.
	private ParserHelper() {
	}

	/**
	 * Return the first find occurrence of a regex match.
	 * 
	 * @param regex pattern regex
	 * @param content data
	 * @return an empty string if it doesn't found pattern
	 */
	public static String findOne(String regex, String content) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		boolean haveResult = matcher.find();
		if (!haveResult) {
			return "";
		}
		return matcher.group(1);
	}

	/**
	 * Return all occurrences of a regex match.
	 * 
	 * @param regex pattern regex
	 * @param content data
	 * @return an empty list if it doesn't found pattern
	 */
	public static List<String> findAll(String regex, String content) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		List<String> r = new ArrayList<String>();
		while (matcher.find()) {
			r.add(matcher.group(1));
		}
		return Collections.unmodifiableList(r);
	}
	
	 /**
     * Converts the sonos upnp timestamp HH:MM:SS to 
     * a duration in seconds
     * @param durationAsString the duration 
     * @return timestamp as seconds
     */
	public static int formatedTimestampToSeconds(String durationAsString) {

		if(!durationAsString.isEmpty()) {
			String[] parts = durationAsString.split(":");
			return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
		}else {
			return 0;
		}
		
	}

	/**
	 * Converts seconds to a sonos upnp duration timestamp in the format
	 * of HH:MM:SS
	 * @param seconds	the seconds
	 * @return	converted timestamp in HH:MM:SS
	 */
	public static String secondsToFormatedTimestamp(int seconds) {
		int hours =  seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		return String.format("%02d:%02d:%02d", hours,minutes,secs);
	}
	
	public static Element unwrapSonosEvent(Element e,Namespace namespace) {
		return e.getChild("Event",namespace).getChild("InstanceID",namespace);
	}
	
	public static String extractEventValue(Element e, String childName) {
		return e.getChild(childName).getAttributeValue("val");
	}
	
	public static String extractEventValue(Element e,String childName, Namespace namespace) {
		return e.getChild(childName,namespace).getAttributeValue("val");
	}
}
