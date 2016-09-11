package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardRegionMembershipCondition extends AbstractWorldGuardCondition {
	
	boolean ownerRequired = false;
	// the condition var may be set to
	//    owner
	// to require the player to own the region, otherwise, the condition will pass if they are just a member
	// this condition will check the highest priority region that the player is standing in.
	
	@Override
	public boolean setVar(String var) {
		if (!worldGuardEnabled()) return false;
		var = var.toLowerCase();
		ownerRequired = (var.contains("owner"));
		return true;
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
		return check(getTopPriorityRegion(location), player);
	}
	
	private boolean check(ProtectedRegion region, Player player) {
		if (region == null || player == null) return false;
		LocalPlayer localPlayer = new BukkitPlayer(worldGuard, player);
		return ownerRequired ? region.isOwner(localPlayer) : region.isMember(localPlayer);
	}
}
