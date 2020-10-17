package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.managers.BossBarManager.Bar;

public class BossBarEffect extends SpellEffect {

	private String namespaceKey;
	private String title;
	private String color;
	private String style;

	private String strVar;
	private Variable variable;
	private double maxValue;

	private BarColor barColor;
	private BarStyle barStyle;

	private int duration;
	private double progress;

	private boolean broadcast;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		namespaceKey = config.getString("namespace-key");
		title = config.getString("title", "");
		color = config.getString("color", "red");
		style = config.getString("style", "solid");
		strVar = config.getString("variable", "");
		maxValue = config.getDouble("max-value", 100);

		if (namespaceKey != null && !MagicSpells.getBossBarManager().isNameSpaceKey(namespaceKey)) {
			MagicSpells.error("Wrong namespace-key defined! '" + namespaceKey + "'");
		}

		try {
			barColor = BarColor.valueOf(color.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			barColor = BarColor.WHITE;
			MagicSpells.error("Wrong bar color defined! '" + color + "'");
		}

		try {
			barStyle = BarStyle.valueOf(style.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			barStyle = BarStyle.SOLID;
			MagicSpells.error("Wrong bar style defined! '" + style + "'");
		}

		duration = config.getInt("duration", 60);
		progress = config.getDouble("progress", 1);
		if (progress > 1) progress = 1;
		if (progress < 0) progress = 0;

		broadcast = config.getBoolean("broadcast", false);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();
		
		variable = MagicSpells.getVariableManager().getVariable(strVar);
		if (variable == null && !strVar.isEmpty()) {
			MagicSpells.error("Wrong variable defined! '" + strVar + "'");
		}
	}

	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (barStyle == null || barColor == null) return null;
		if (broadcast) Util.forEachPlayerOnline(this::createBar);
		else if (entity instanceof Player) createBar((Player) entity);
		return null;
	}

	private void createBar(Player player) {
		Bar bar = MagicSpells.getBossBarManager().getBar(player, namespaceKey);
		String newTitle = Util.doVarReplacementAndColorize(player, title);
		if (variable == null) bar.set(newTitle, progress, barStyle, barColor);
		else {
			double diff = variable.getValue(player) / maxValue;
			if (diff > 0 && diff < 1) bar.set(newTitle, diff, barStyle, barColor);
		}
		if (duration > 0) MagicSpells.scheduleDelayedTask(bar::remove, duration);
	}

}
