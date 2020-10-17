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

        Location location = p.getLocation();
        World world = location.getWorld();
        if (world != null) return location.getY() - world.getHighestBlockYAt(location);
        return 0;
    }

}
