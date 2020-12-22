package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Scoreboard;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;

public class GlowSpell extends BuffSpell {

	private final Set<LivingEntity> glowing;

	private Team team;
	private boolean ownsTeam;

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		glowing = new HashSet<>();

		String colorName = getConfigString("color", "");
		if (colorName == null || colorName.isEmpty()) colorName = "white";
		ChatColor color;
		try {
			color = ChatColor.valueOf(colorName.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			DebugHandler.debugBadEnumValue(ChatColor.class, colorName);
			return;
		}

		// For white color we don't need a team.
		if (color == ChatColor.WHITE) return;

		// Maybe if was registered by another spell effect?
		String teamName = "MS_" + color.name().toUpperCase();
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		team = scoreboard.getTeam(teamName);
		if (team != null) return;

		team = scoreboard.registerNewTeam(teamName);
		team.setColor(color);
		ownsTeam = true;
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		glowing.add(entity);
		entity.setGlowing(true);
		if (team != null) {
			String entry = entity instanceof Player ? entity.getName() : entity.getUniqueId().toString();
			team.addEntry(entry);
		}
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return glowing.contains(entity);
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		glowing.remove(entity);
		entity.setGlowing(false);
		if (team != null) {
			String entry = entity instanceof Player ? entity.getName() : entity.getUniqueId().toString();
			team.removeEntry(entry);
		}
	}

	@Override
	protected void turnOff() {
		for (LivingEntity entity : new HashSet<>(glowing)) {
			if (entity == null) continue;
			turnOff(entity);
		}

		if (!ownsTeam) return;
		try {
			team.unregister();
		} catch (IllegalStateException ignored) {}
	}

}
