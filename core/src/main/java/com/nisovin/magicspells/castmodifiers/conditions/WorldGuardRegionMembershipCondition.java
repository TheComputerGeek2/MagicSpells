package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.nisovin.magicspells.castmodifiers.conditions.util.AbstractWorldGuardCondition;

public class WorldGuardRegionMembershipCondition extends AbstractWorldGuardCondition {
	
	private boolean ownerRequired = false;
	/* the condition var may be set to owner
	to require the player to own the region, otherwise, the condition will pass if they are just a member
	this condition will check the highest priority region that the player is standing in. */
	
	@Override
	public boolean initialize(String var) {
		if (!worldGuardEnabled()) return false;
		var = var.toLowerCase();
		ownerRequired = var.contains("owner");
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return check(getTopPriorityRegion(location), livingEntity);
	}
	
	private boolean check(ProtectedRegion region, LivingEntity livingEntity) {
		if (region == null || livingEntity == null) return false;
		if (!(livingEntity instanceof Player)) return false;
		LocalPlayer localPlayer = new BukkitPlayer(worldGuard, (Player) livingEntity);
		return ownerRequired ? region.isOwner(localPlayer) : region.isMember(localPlayer);
	}
	
}
