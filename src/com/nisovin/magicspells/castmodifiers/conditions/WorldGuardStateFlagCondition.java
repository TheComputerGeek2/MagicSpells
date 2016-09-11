package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.HashMap;
import java.util.Map;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardStateFlagCondition extends AbstractWorldGuardFlagCondition {

	StateFlag flag = null;
	
	static Map<String, StateFlag> nameMap;
	
	static {
		nameMap = new HashMap<String, StateFlag>();
		for (Flag<?> f: DefaultFlag.getFlags()) {
			if (f instanceof StateFlag) {
				nameMap.put(f.getName(), (StateFlag)f);
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
		return region.getFlag(flag) == StateFlag.State.ALLOW;
	}

}
