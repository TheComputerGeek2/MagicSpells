package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.HashMap;
import java.util.Map;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardBooleanFlagCondition extends AbstractWorldGuardFlagCondition {

	BooleanFlag flag = null;
	
	static Map<String, BooleanFlag> nameMap;
	static {
		nameMap = new HashMap<String, BooleanFlag>();
		for (Flag<?> f: DefaultFlag.getFlags()) {
			if (f instanceof BooleanFlag) {
				nameMap.put(f.getName().toLowerCase(), (BooleanFlag)f);
			}
		}
	}
	
	@Override
	protected boolean parseVar(String var) {
		flag = nameMap.get(var.toLowerCase());
		return flag != null;
	}

	@Override
	protected boolean check(ProtectedRegion region, LocalPlayer player) {
		return region.getFlag(flag).booleanValue();
	}

}
