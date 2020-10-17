package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class GriefPreventionIsOwnerCondition extends Condition {

	@Override
	public boolean initialize(String var) {
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
		if (livingEntity == null) return false;
		Claim currentClaim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
		if (currentClaim == null) return false;
		return (livingEntity.getUniqueId().equals(currentClaim.ownerID));
	}

}
