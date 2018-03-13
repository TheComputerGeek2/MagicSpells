package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.DataStore;

public class GriefPreventionIsOwnerCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(Player player) {
		// Does the claim recognize the player as the owner?
		Claim currentClaim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
		if (currentClaim == null) return false;
		return (player.getUniqueId().equals(currentClaim.ownerID));
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		// Does the claim recognize the target as the owner?
		Claim currentClaim = GriefPrevention.instance.dataStore.getClaimAt(target.getLocation(), false, null);
		if (currentClaim == null) return false;
		return (target.getUniqueId().equals(currentClaim.ownerID));
	}

	@Override
	public boolean check(Player player, Location location) {
		// Locations, coming soon.
		return false;
	}

}
