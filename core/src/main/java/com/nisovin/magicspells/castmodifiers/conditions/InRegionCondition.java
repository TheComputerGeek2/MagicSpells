package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.util.compat.CompatBasics;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class InRegionCondition extends Condition {

	WorldGuardPlugin worldGuard;
	String worldName;
	String regionName;
	ProtectedRegion region;

	@Override
	public boolean setVar(String var) {
		if (var == null) return false;

		worldGuard = (WorldGuardPlugin)CompatBasics.getPlugin("WorldGuard");
		if (worldGuard == null || !worldGuard.isEnabled()) return false;

		String[] split = var.split(":");
		if (split.length == 2) {
			worldName = split[0];
			regionName = split[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		if (region == null) {
			org.bukkit.World world = Bukkit.getWorld(worldName);

			if (world == null) return false;
			if (!world.equals(location.getWorld())) return false;

			World aWorld = BukkitAdapter.adapt(world);

			RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionManager regionManager = regionContainer.get(aWorld);

			if (regionManager == null) return false;
			region = regionManager.getRegion(regionName);
		}
		if (region == null) return false;
		return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

}
