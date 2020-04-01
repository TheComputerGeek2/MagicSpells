package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.HashMap;

public enum TargetBooleanState {
	
	ON("on", "yes", "true", "enable", "enabled") {
	
		@Override
		public boolean getBooleanState(boolean current) {
			return true;
		}
		
	},
	
	OFF("off", "no", "false", "disable", "disabled") {
		
		@Override
		public boolean getBooleanState(boolean current) {
			return false;
		}
		
	},
	
	TOGGLE("toggle", "switch") {
		
		@Override
		public boolean getBooleanState(boolean current) {
			return !current;
		}
		
	};
	
	private final String[] names;
	
	TargetBooleanState(String... names) {
		this.names = names;
	}
	
	public abstract boolean getBooleanState(boolean current);
	
	private static Map<String, TargetBooleanState> nameToState = new HashMap<>();
	
	static {
		for (TargetBooleanState value : TargetBooleanState.values()) {
			for (String name : value.names) {
				nameToState.put(name, value);
			}
		}
	}
	
	public static TargetBooleanState getFromName(String name) {
		TargetBooleanState ret = nameToState.get(name.toLowerCase());
		if (ret == null) ret = TargetBooleanState.TOGGLE;
		return ret;
	}
	
}
