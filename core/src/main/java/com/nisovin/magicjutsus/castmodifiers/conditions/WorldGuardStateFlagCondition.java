package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.Map;
import java.util.HashMap;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
//import com.sk89q.worldguard.protection.flags.DefaultFlag;
//import com.sk89q.worldguard.protection.flags.Flag;

public class WorldGuardStateFlagCondition extends AbstractWorldGuardFlagCondition {

	private StateFlag flag = null;
	
	private static Map<String, StateFlag> nameMap;
	
	static {
		nameMap = new HashMap<>();
		/*for (Flag<?> f: DefaultFlag.getFlags()) {
			if (f instanceof StateFlag) {
				nameMap.put(f.getName(), (StateFlag)f);
			}
		}*/
		// FIXME get this setup
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
