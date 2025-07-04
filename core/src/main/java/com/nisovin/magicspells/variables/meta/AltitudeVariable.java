package com.nisovin.magicspells.variables.meta;

import org.bukkit.*;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AltitudeVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = Bukkit.getPlayerExact(player);
		if (p == null) return 0;

		World world = p.getWorld();
		int x = Location.locToBlock(p.getX());
		int y = Location.locToBlock(p.getY()) - 1;
		int z = Location.locToBlock(p.getZ());

		int highestPoint = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
		if (highestPoint <= y) return y - highestPoint;

		for (int i = y, min = world.getMinHeight(); i >= min; i--) {
			// Matches the check for HeightMap.MOTION_BLOCKING
			if (!world.getBlockAt(x, i, z).isSolid() && world.getFluidData(x, i, z).getFluidType() == Fluid.EMPTY)
				continue;

			return y - i;
		}

		return 0;
	}

}
