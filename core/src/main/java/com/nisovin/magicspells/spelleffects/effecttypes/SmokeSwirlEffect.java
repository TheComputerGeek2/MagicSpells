package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class SmokeSwirlEffect extends SpellEffect {

	private static final int[] X = {1, 1, 0, -1, -1, -1, 0, 1};
	private static final int[] Z = {0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] V = {7, 6, 3, 0, 1, 2, 5, 8};

	private ConfigData<Integer> duration;
	private ConfigData<Integer> interval;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		duration = ConfigDataUtil.getInteger(config, "duration", TimeUtil.TICKS_PER_SECOND);
		interval = ConfigDataUtil.getInteger(config, "interval", 1);
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		new Animator(location, interval.get(data), duration.get(data));
		return null;
	}

	@Override
	public Runnable playEffectEntity(Entity entity, SpellData data) {
		new Animator(entity, interval.get(data), duration.get(data));
		return null;
	}

	private class Animator implements Runnable {

		private Entity entity;
		private Location location;
		private int interval;
		private int animatorDuration;
		private int iteration;
		private int animatorTaskId;

		Animator(Location location, int interval, int duration) {
			this(interval, duration);
			this.location = location;
		}

		Animator(Entity entity, int interval, int duration) {
			this(interval, duration);
			this.entity = entity;
		}

		Animator(int interval, int duration) {
			this.interval = interval;
			this.animatorDuration = duration;
			this.iteration = 0;
			this.animatorTaskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			if (iteration * interval > animatorDuration) {
				MagicSpells.cancelTask(animatorTaskId);
				return;
			}

			int i = iteration % 8;
			Location loc;
			if (location != null) loc = location;
			else loc = entity.getLocation();

			loc.getWorld().playEffect(loc.clone().add(X[i], 0, Z[i]), Effect.SMOKE, V[i]);
			iteration++;
		}

	}

}
