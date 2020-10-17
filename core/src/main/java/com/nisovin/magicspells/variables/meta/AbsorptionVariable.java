package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AbsorptionVariable extends MetaVariable {

    @Override
    public double getValue(String p) {
        Player player = PlayerNameUtils.getPlayerExact(p);
        if (player == null) return 0D;
        return MagicSpells.getVolatileCodeHandler().getAbsorptionHearts(player);
    }

    @Override
    public void set(String p, double amount) {
        Player player = PlayerNameUtils.getPlayerExact(p);
        if (player != null) MagicSpells.getVolatileCodeHandler().setAbsorptionHearts(player, amount);
    }

}
