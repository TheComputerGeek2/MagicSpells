package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public abstract class AbstractWorldGuardCondition extends Condition {

	protected WorldGuardPlugin worldGuard;
	
	protected boolean worldGuardEnabled() {
		worldGuard = (WorldGuardPlugin)Bukkit.getPluginManager().getPlugin("WorldGuard");
		return !(worldGuard == null || !worldGuard.isEnabled());
	}
	
	protected RegionManager getRegionManager(World world) {
		return worldGuard.getRegionManager(world);
	}
	
	protected ApplicableRegionSet getRegion(Location loc) {
		return getRegionManager(loc.getWorld()).getApplicableRegions(new Vector(loc.getX(), loc.getY(), loc.getZ()));
	}
	
	protected ProtectedRegion getTopPriorityRegion(Location loc) {
		ApplicableRegionSet regions = getRegion(loc);
		ProtectedRegion topRegion = null;
		int topPriority = Integer.MIN_VALUE;
		for (ProtectedRegion region: regions) {
			if (region.getPriority() > topPriority) {
				topRegion = region;
				topPriority = region.getPriority();
			}
		}
		return topRegion;
	}
}
