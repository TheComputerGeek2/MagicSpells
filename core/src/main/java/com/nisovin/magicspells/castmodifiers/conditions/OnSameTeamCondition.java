package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.ScoreboardManager;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnSameTeamCondition extends Condition {
	
	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (target instanceof Player && livingEntity instanceof Player) {
			ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
			if (scoreboardManager == null) return false;
			Team team1 = scoreboardManager.getMainScoreboard().getEntryTeam(livingEntity.getName());
			Team team2 = scoreboardManager.getMainScoreboard().getEntryTeam(target.getName());
			return team1 != null && team1.equals(team2);
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
}
