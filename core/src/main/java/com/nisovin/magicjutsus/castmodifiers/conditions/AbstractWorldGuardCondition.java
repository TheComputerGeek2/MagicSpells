package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public abstract class AbstractWorldGuardCondition extends Condition {

	protected WorldGuardPlugin worldGuard;

	protected boolean worldGuardEnabled() {
		worldGuard = (WorldGuardPlugin)Bukkit.getPluginManager().getPlugin("WorldGuard");
		return !(worldGuard == null || !worldGuard.isEnabled());
	}

	protected RegionManager getRegionManager(World world) {
		return WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
		//return worldGuard.getRegionManager(world);
	}

	protected ApplicableRegionSet getRegion(Location loc) {
		RegionManager manager = getRegionManager(loc.getWorld());
		return manager.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
		//return getRegionManager(loc.getWorld()).getApplicableRegions(new Vector(loc.getX(), loc.getY(), loc.getZ()));
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
