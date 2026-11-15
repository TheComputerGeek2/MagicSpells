package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.World;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("cloud")
public class CloudEffect extends SpellEffect {

	private ConfigData<Integer> radius;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		radius = ConfigDataUtil.getInteger(config, "radius", 3);
	}
	
	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		World w = location.getWorld();
		int cx = location.getBlockX();
		int cy = location.getBlockY();
		int cz = location.getBlockZ();
		
		Block b;
		int radius = this.radius.get(data);
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				if (!inRange(x, z, cx, cz, radius)) continue;
				b = w.getBlockAt(x, cy, z);
				if (b.isPassable()) {
					smoke(w, b, radius);
					continue;
				}
				b = b.getRelative(0, -1, 0);
				if (b.isPassable()) {
					smoke(w, b, radius);
					continue;
				}
				b = b.getRelative(0, 2, 0);
				if (b.isPassable()) smoke(w, b, radius);
			}
		}
		return null;
	}
	
	private void smoke(World w, Block b, int r) {
		Location loc = b.getLocation();
		if (r <= 5) {
			for (int i = 0; i <= 8; i += 2) w.playEffect(loc, Effect.SMOKE_SHOOT, i);
		} else if (r <= 8) {
			w.playEffect(loc, Effect.SMOKE_SHOOT, random.nextInt(9));
			w.playEffect(loc, Effect.SMOKE_SHOOT, random.nextInt(9));
		} else {
			w.playEffect(loc, Effect.SMOKE_SHOOT, random.nextInt(9));
		}
	}
	
	private boolean inRange(int x1, int z1, int x2, int z2, int r) {
		return sq(x1 - x2) + sq(z1 - z2) < sq(r);
	}

	private int sq(int v) {
		return v * v;
	}

}
