package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Map;
import java.util.HashMap;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;
import com.nisovin.magicspells.castmodifiers.conditions.util.AbstractWorldGuardCondition;

@DependsOn(plugin = "WorldGuard")
public class WorldGuardStateFlagCondition extends AbstractWorldGuardCondition {

	protected static Map<String, StateFlag> flags = new HashMap<>()
			;
	static {
		for (Flag<?> flag : WorldGuard.getInstance().getFlagRegistry().getAll()) {
			if (!(flag instanceof StateFlag stateFlag)) continue;
			flags.put(flag.getName(), stateFlag);
		}
	}

	private StateFlag flag = null;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		flag = flags.get(var.toLowerCase());
		return flag != null;
	}

	@Override
	protected boolean check(ProtectedRegion region, LocalPlayer player) {
		return region.getFlag(flag) == StateFlag.State.ALLOW;
	}

}
