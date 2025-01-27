package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.UUID;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.managers.BossBarManager.Bar;

@Name("bossbar")
public class BossBarEffect extends SpellEffect {

	private static final Object2ObjectMap<TaskData, ScheduledTask> tasks;

	static {
		tasks = new Object2ObjectOpenHashMap<>();
		tasks.defaultReturnValue(null);
	}

	private ConfigData<String> namespaceKey;

	private String title;

	private ConfigData<Boolean> remove;
	private ConfigData<Boolean> visible;
	private ConfigData<Boolean> broadcast;
	private ConfigData<Boolean> useViewerAsTarget;
	private ConfigData<Boolean> useViewerAsDefault;

	private ConfigData<Integer> duration;

	private ConfigData<Double> progress;
	private ConfigData<Double> maxValue;

	private ConfigData<BarColor> barColor;
	private ConfigData<BarStyle> barStyle;

	private ConfigData<String> variable;
	private ConfigData<String> maxVariable;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		namespaceKey = ConfigDataUtil.getString(config, "namespace-key", null);

		title = config.getString("title", "");

		remove = ConfigDataUtil.getBoolean(config, "remove", false);
		visible = ConfigDataUtil.getBoolean(config, "visible", true);
		broadcast = ConfigDataUtil.getBoolean(config, "broadcast", false);
		useViewerAsTarget = ConfigDataUtil.getBoolean(config, "use-viewer-as-target", false);
		useViewerAsDefault = ConfigDataUtil.getBoolean(config, "use-viewer-as-default", true);

		duration = ConfigDataUtil.getInteger(config, "duration", 60);

		progress = ConfigDataUtil.getDouble(config, "progress", 1);
		maxValue = ConfigDataUtil.getDouble(config, "max-value", 100);

		barColor = ConfigDataUtil.getEnum(config, "color", BarColor.class, BarColor.RED);
		barStyle = ConfigDataUtil.getEnum(config, "style", BarStyle.class, BarStyle.SOLID);

		variable = ConfigDataUtil.getString(config, "variable", null);
		maxVariable = ConfigDataUtil.getString(config, "max-variable", null);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		boolean useViewerAsTarget = this.useViewerAsTarget.get(data);
		boolean useViewerAsDefault = this.useViewerAsDefault.get(data);

		if (broadcast.get(data)) {
			Util.forEachPlayerOnline(player -> {
				SpellData subData = data;
				if (useViewerAsTarget) subData = subData.target(player);
				if (useViewerAsDefault) subData = subData.recipient(player);

				updateBar(player, subData);
			});

			return null;
		}

		if (entity instanceof Player player) {
			SpellData subData = data;
			if (useViewerAsTarget) subData = subData.target(player);
			if (useViewerAsDefault) subData = subData.recipient(player);

			updateBar(player, subData);
		}

		return null;
	}

	@Override
	public void turnOff() {
		for (ScheduledTask t : tasks.values()) MagicSpells.cancelTask(t);
		tasks.clear();
	}

	private void updateBar(Player player, SpellData data) {
		boolean remove = this.remove.get(data);

		String namespaceKey = this.namespaceKey.get(data);
		if (namespaceKey != null && !MagicSpells.getBossBarManager().isNamespaceKey(namespaceKey)) return;

		Bar bar = MagicSpells.getBossBarManager().getBar(player, namespaceKey, !remove);
		if (remove) {
			if (bar != null) bar.remove();
			return;
		}

		double maxValue;
		String maxVariable = this.maxVariable.get(data);
		if (maxVariable != null) {
			Variable maxVar = MagicSpells.getVariableManager().getVariable(maxVariable);

			if (maxVar != null) maxValue = maxVar.getValue(player);
			else maxValue = this.maxValue.get(data);
		} else maxValue = this.maxValue.get(data);

		double progress;
		String variable = this.variable.get(data);
		if (variable != null) {
			Variable var = MagicSpells.getVariableManager().getVariable(variable);

			if (var != null) progress = var.getValue(player) / maxValue;
			else progress = this.progress.get(data);
		} else progress = this.progress.get(data);

		progress = Math.min(Math.max(progress, 0), 1);

		String title = Util.getLegacyFromComponent(Util.getMiniMessage(this.title, player, data));
		bar.set(title, progress, barStyle.get(data), barColor.get(data), visible.get(data));

		int duration = this.duration.get(data);
		if (duration > 0) {
			TaskData taskData = new TaskData(bar.getNamespaceKey(), player.getUniqueId());

			ScheduledTask newTask = MagicSpells.scheduleDelayedTask(() -> {
				tasks.remove(taskData);
				bar.remove();
			}, duration, player);

			ScheduledTask oldTask = tasks.put(taskData, newTask);
			if (oldTask != null) MagicSpells.cancelTask(oldTask);
		}
	}

	private record TaskData(String key, UUID uuid) {
	}

}
