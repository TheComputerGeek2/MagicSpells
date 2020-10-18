package com.nisovin.magicspells.variables.meta;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AltitudeVariable extends MetaVariable {

    @Override
    public double getValue(String player) {
        Player p = PlayerNameUtils.getPlayerExact(player);
        if (p == null) return 0;
        World world = p.getWorld();
        Location location = p.getLocation().clone().subtract(0,1,0);
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Try to avoid looping through block below the player.
        int highestPoint = world.getHighestBlockYAt(location);
        if (highestPoint < y) return y - highestPoint;

        for (int i = y; i > 0; i--) {
            if (world.getBlockAt(x, i, z).getType().isAir()) continue;
            return y - i;
        }
        return 0;
    }

}
