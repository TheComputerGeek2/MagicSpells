package com.nisovin.magicjutsus.variables;

import java.util.Map;
import java.util.HashMap;

public enum VariableType {
	
	PLAYER("player") {
		
		@Override
		public Variable newInstance() {
			return new PlayerVariable();
		}
		
	},
	
	GLOBAL("global") {
		
		@Override
		public Variable newInstance() {
			return new GlobalVariable();
		}
		
	},
	
	DISTANCE_TO("distancetolocation") {
		
		@Override
		public Variable newInstance() {
			return new DistanceToVariable();
		}
		
	},
	
	
	DISTANCE_TO_SQUARED("squareddistancetolocation") {
		
		@Override
		public Variable newInstance() {
			return new DistanceToSquaredVariable();
		}
		
	},
	
	PLAYER_STRING("playerstring") {

		@Override
		public Variable newInstance() {
			return new PlayerStringVariable();
		}
		
	}
	;
	
	private String[] names;
	
	VariableType(String... names) {
		this.names = names;
	}
	
	public abstract Variable newInstance();
	
	private static Map<String, VariableType> nameMap;
	private static boolean initialized = false;
	
	public static void initialize() {
		if (initialized) return;
		nameMap = new HashMap<>();
		for (VariableType type : VariableType.values()) {
			for (String name: type.names) {
				nameMap.put(name.toLowerCase(), type);
			}
		}
		initialized = true;
	}
	
	public static VariableType getType(String name) {
		if (!initialized) initialize();
		
		VariableType ret = nameMap.get(name.toLowerCase());
		if (ret == null) ret = VariableType.GLOBAL;
		
		return ret;
	}
	
}
