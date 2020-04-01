package com.nisovin.magicjutsus.util;

import org.apache.commons.lang.NullArgumentException;

// Just some methods useful for checking safety of operations
public class SafetyCheckUtils {

	public static boolean areAnyNull(Object... objects) {
		if (objects == null) throw new NullArgumentException("No, you do not get to pass a null collection of items to check for nullness -.-");
		for (Object o: objects) {
			if (o == null) return true;
		}
		return false;
	}
	
}
