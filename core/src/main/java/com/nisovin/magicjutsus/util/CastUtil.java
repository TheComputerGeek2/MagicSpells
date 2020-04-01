package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.HashMap;

public class CastUtil {

	public enum CastMode {

		HARD("hard", "h"),
		FULL("full", "f"),
		PARTIAL("partial", "p"),
		DIRECT("direct", "d");

		private static Map<String, CastMode> nameMap = new HashMap<>();

		private final String[] names;

		CastMode(String... names) {
			this.names = names;
		}

		public static CastMode getFromString(String label) {
			return nameMap.get(label.toLowerCase());
		}

		static {
			for (CastMode mode : CastMode.values()) {
				nameMap.put(mode.name().toLowerCase(), mode);
				for (String s : mode.names) {
					nameMap.put(s.toLowerCase(), mode);
				}
			}
		}

	}

}
