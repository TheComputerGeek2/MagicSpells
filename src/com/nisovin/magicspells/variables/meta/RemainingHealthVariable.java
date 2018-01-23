package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.MetaVariable;

public class RemainingHealthVariable extends MetaVariable {

    @Override
    public double getValue(String player) {
        Player p = PlayerNameUtils.getPlayerExact(player);
        if (p != null) return p.getHealth();
        return 0;

    }
    @Override
    public void set(String player, double amount) {
        Player p = PlayerNameUtils.getPlayerExact(player);
        if (p != null) p.setHealth((int) amount);
    }

    @Override
    public boolean modify(String player, double amount) {
        Player p = PlayerNameUtils.getPlayerExact(player);
        if (p != null) {
            p.setHealth(p.getHealth() + (int) amount);
            return true;
        }
        return false;
    }

}
