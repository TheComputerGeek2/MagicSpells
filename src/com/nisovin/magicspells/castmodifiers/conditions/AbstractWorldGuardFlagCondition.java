package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		ProtectedRegion region = getTopPriorityRegion(location);
		LocalPlayer localPlayer = new BukkitPlayer(worldGuard, player);
		return check(region, localPlayer);
	}
	
	protected abstract boolean check(ProtectedRegion region, LocalPlayer player);

}
