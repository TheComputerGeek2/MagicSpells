package com.nisovin.magicjutsus.jutsueffects;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;

public class SmokeTrailEffect extends JutsuEffect {

	private int interval;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		interval = config.getInt("interval", 0);
	}

	@Override
	public Runnable playEffect(Location location1, Location location2) {		
		SmokeStreamEffect effect = new SmokeStreamEffect(location1, location2);
		if (interval > 0) effect.start(interval);
		else effect.showNoAnimation();
		return null;
	}
	
	// Thanks to DrBowe for sharing the code
	private class SmokeStreamEffect implements Runnable {
		
		private Location startLoc;
		private Location endLoc;
		private List<Location> locationsForProjection;
		private World world;

		private int i;
		private int id;
		
		SmokeStreamEffect(Location loc1, Location loc2) {
			this.startLoc = loc1;
			this.endLoc = loc2;
			this.world = startLoc.getWorld();
			this.locationsForProjection = calculateLocsForProjection();
			this.i = 0;
		}
		
		public void start(int interval) {
			this.id = MagicJutsus.scheduleRepeatingTask(this, interval, interval);
		}

		void showNoAnimation() {
			while (this.i < locationsForProjection.size()) {
				run();
			}
		}
		
		@Override
		public void run() {
			if (i > locationsForProjection.size() - 1) {
				MagicJutsus.cancelTask(id);
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
