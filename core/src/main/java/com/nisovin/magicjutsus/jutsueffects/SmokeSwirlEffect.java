package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;

public class SmokeSwirlEffect extends JutsuEffect {

	private int interval;
	private int duration = TimeUtil.TICKS_PER_SECOND;

	private int[] x = {1, 1, 0, -1, -1, -1, 0, 1};
	private int[] z = {0, 1, 1, 1, 0, -1, -1, -1};
	private int[] v = {7, 6, 3, 0, 1, 2, 5, 8};

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		interval = config.getInt("interval", 1);
		duration = config.getInt("duration", duration);
	}

	@Override
	public Runnable playEffectLocation(Location location) {		
		new Animator(location, interval, duration);
		return null;
	}
	
	@Override
	public Runnable playEffectEntity(Entity entity) {
		new Animator(entity, interval, duration);
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
			this.animatorTaskId = MagicJutsus.scheduleRepeatingTask(this, 0, interval);
		}
		
		@Override
		public void run() {
			if (iteration * interval > animatorDuration) {
				MagicJutsus.cancelTask(animatorTaskId);
				return;
			}

			int i = iteration % 8;
			Location loc;
			if (location != null) loc = location;
			else loc = entity.getLocation();

			loc.getWorld().playEffect(loc.clone().add(x[i], 0, z[i]), Effect.SMOKE, v[i]);
			iteration++;
		}
		
	}
	
}
