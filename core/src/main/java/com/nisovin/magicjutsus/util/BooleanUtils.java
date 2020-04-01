package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.HashSet;

public class BooleanUtils {

	private static Set<String> yesStrings;
	private static Set<String> noStrings;
	static {
		yesStrings = new HashSet<>();
		noStrings = new HashSet<>();

		yesStrings.add("yes");
		yesStrings.add("true");

		noStrings.add("no");
		noStrings.add("false");
	}
	
	public static boolean isYes(String toCheck) {
		return yesStrings.contains(toCheck.trim().toLowerCase());
	}

	public static boolean isNo(String toCheck) {
		return noStrings.contains(toCheck.trim().toLowerCase());
	}
	
}
