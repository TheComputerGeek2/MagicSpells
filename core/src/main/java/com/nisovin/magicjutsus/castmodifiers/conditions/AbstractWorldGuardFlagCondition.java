package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public abstract class AbstractWorldGuardFlagCondition extends AbstractWorldGuardCondition {
	
	@Override
	public boolean setVar(String var) {
		if (!worldGuardEnabled()) return false;
		return parseVar(var);
	}
	
	protected abstract boolean parseVar(String var);

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(livingEntity, target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		if (!(livingEntity instanceof Player)) return false;
		ProtectedRegion region = getTopPriorityRegion(location);
		LocalPlayer localPlayer = new BukkitPlayer(worldGuard, (Player) livingEntity);
		return check(region, localPlayer);
	}
	
	protected abstract boolean check(ProtectedRegion region, LocalPlayer player);

}
