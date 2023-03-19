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
	public boolean check(LivingEntity caster) {
		return onTeam(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return onTeam(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean onTeam(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(pl.getName());
		return team != null && team.getName().equals(teamName);
	}

}
