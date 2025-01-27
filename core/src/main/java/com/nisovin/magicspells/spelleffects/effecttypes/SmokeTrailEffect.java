package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.List;
import java.util.ArrayList;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("smoketrail")
public class SmokeTrailEffect extends SpellEffect {

	private ConfigData<Integer> interval;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		interval = ConfigDataUtil.getInteger(config, "interval", 0);
	}

	@Override
	public Runnable playEffect(Location startLoc, Location endLoc, SpellData data) {
		SmokeStreamEffect effect = new SmokeStreamEffect(startLoc, endLoc);

		int interval = this.interval.get(data);
		if (interval > 0) effect.start(interval);
		else effect.showNoAnimation();

		return null;
	}

	// Thanks to DrBowe for sharing the code
	private static class SmokeStreamEffect implements Runnable {

		private final Location startLoc;
		private final Location endLoc;
		private final List<Location> locationsForProjection;
		private final World world;

		private int i;
		private ScheduledTask task;

		SmokeStreamEffect(Location loc1, Location loc2) {
			this.startLoc = loc1;
			this.endLoc = loc2;
			this.world = startLoc.getWorld();
			this.locationsForProjection = calculateLocsForProjection();
			this.i = 0;
		}

		public void start(int interval) {
			this.task = MagicSpells.scheduleRepeatingTask(this, interval, interval);
		}

		void showNoAnimation() {
			while (this.i < locationsForProjection.size()) {
				run();
			}
		}

		@Override
		public void run() {
			if (i > locationsForProjection.size() - 1) {
				MagicSpells.cancelTask(task);
				return;
			}
			Location loc = locationsForProjection.get(i);
			for (int j = 0; j <= 8; j += 2) {
				world.playEffect(loc, Effect.SMOKE, j);
			}
			i++;
		}

		private List<Location> calculateLocsForProjection() {
			double x1;
			double y1;
			double z1;
			double x2;
			double y2;
			double z2;
			double xVect;
			double yVect;
			double zVect;
			x1 = endLoc.getX();
			y1 = endLoc.getY();
			z1 = endLoc.getZ();
			x2 = startLoc.getX();
			y2 = startLoc.getY();
			z2 = startLoc.getZ();
			xVect = x2 - x1;
			yVect = y2 - y1;
			zVect = z2 - z1;
			double distance = startLoc.distance(endLoc);
			List<Location> tmp = new ArrayList<>((int) Math.floor(distance));

			for (double t = 0; t <= 1; t += 1 / distance) {
				tmp.add(new Location(world, x2 - (xVect * t), y2 - (yVect * t) + 1, z2 - (zVect * t)));
			}
			return tmp;
		}

	}

}
