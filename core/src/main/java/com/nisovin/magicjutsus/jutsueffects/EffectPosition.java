package com.nisovin.magicjutsus.jutsueffects;

import java.util.Map;
import java.util.HashMap;

public enum EffectPosition {

	START_CAST(0, "start", "startcast"),
	CASTER(1, "pos1", "position1", "caster", "actor"),
	TARGET(2, "pos2", "position2", "target"),
	TRAIL(3, "line", "trail"),
	DISABLED(4, "disabled"),
	DELAYED(5, "delayed"),
	SPECIAL(6, "special"),
	BUFF(7, "buff", "active"),
	ORBIT(8, "orbit"),
	REVERSE_LINE(9, "reverse_line", "reverseline", "rline"),
	PROJECTILE(10, "projectile"),
	DYNAMIC_CASTER_PROJECTILE_LINE(11, "casterprojectile", "casterprojectileline"),
	BLOCK_DESTRUCTION(12, "blockdestroy", "blockdestruction"),
	COOLDOWN(13, "cooldown"),
	MISSING_REAGENTS(14, "missingreagents"),
	CHARGE_USE(15, "chargeuse")

	;

	private int id;
	private String[] names;
	
	private static Map<String, EffectPosition> nameMap = new HashMap<>();
	private static boolean initialized = false;
	
	EffectPosition(int num, String... names) {
		this.id = num;
		this.names = names;
	}
	
	public int getId() {
		return id;
	}
	
	private static void initializeNameMap() {
		if (nameMap == null) nameMap = new HashMap<>();
		nameMap.clear();
		for (EffectPosition pos: EffectPosition.values()) {
			// Make sure the number id can be mapped
			nameMap.put(pos.id + "", pos);
			
			// For all of the names
			for (String name: pos.names) {
				nameMap.put(name.toLowerCase(), pos);
			}
		}
		initialized = true;
	}
	
	public static EffectPosition getPositionFromString(String pos) {
		if (!initialized) initializeNameMap();
		return nameMap.get(pos.toLowerCase());
	}
	
}
