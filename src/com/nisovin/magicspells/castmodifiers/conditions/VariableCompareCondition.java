package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

import java.util.Objects;

public class VariableCompareCondition extends Condition {

    String variable;
    String firstVariable = null;
    String secondVariable = null;

    @Override
    public boolean setVar(String var) {
        try {
            String[] split = var.split(":",2);
            firstVariable = split[0]; //The variable that is being checked
            secondVariable = split[1]; //The string that the variable is being checked against
            return true;
        } catch (ArrayIndexOutOfBoundsException missingColon) {
            // Someone didn't read the GitHub commit
            MagicSpells.error("You must use a colon to separate the two variables");
            return false;
        }
    }

    @Override
    public boolean check(Player player) {
        // Get variable values
        String value = MagicSpells.getVariableManager().getStringValue(firstVariable, player);
        String valueSecond = MagicSpells.getVariableManager().getStringValue(secondVariable, player);

        // Do comparison
        return (value.equals(valueSecond));
    }

    @Override
    public boolean check(Player player, LivingEntity target) {
        // Someone didn't read the GitHub commit x2
        MagicSpells.error("VariableCompare cannot be used in target-modifiers, use VariableMatches");
        return false;
    }

    @Override
    public boolean check(Player player, Location location) {
        // Against defaults (only possible comparison here)
        return check(player);
    }
}
