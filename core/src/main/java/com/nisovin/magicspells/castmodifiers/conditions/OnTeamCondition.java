package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnTeamCondition extends Condition {

	private String teamName;
	
	@Override
	public boolean initialize(String var) {
		teamName = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (target instanceof Player) {
			Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(target.getName());
			return team != null && team.getName().equals(teamName);
		}
		return false;

	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
