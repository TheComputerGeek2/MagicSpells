package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.util.DependsOn;

@DependsOn(plugin = "WorldGuard")
public class InRegionCondition extends Condition {

	private String worldName;
	private String regionName;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;
		String[] split = var.split(":");
		if (split.length == 2) {
			worldName = split[0];
			regionName = split[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return checkRegion(caster.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return checkRegion(target.getLocation());
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return checkRegion(location);
	}

	private boolean checkRegion(Location location) {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return false;
		if (world != location.getWorld()) return false;

		RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) return false;

		ProtectedRegion region = regionManager.getRegion(regionName);
		return region != null && region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

}
