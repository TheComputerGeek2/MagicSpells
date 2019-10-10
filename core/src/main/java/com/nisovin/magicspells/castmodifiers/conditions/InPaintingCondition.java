package com.nisovin.magicspells.castmodifiers.conditions;

import com.nisovin.magicspells.castmodifiers.Condition;
import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.entity.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InPaintingCondition extends Condition {

    Set<Art> arts;
    Art art;

    @Override
    public boolean setVar(String var) {
        if (var.contains(",")) {
            arts = new HashSet<>();
            String[] split = var.split(",");
            for (String s : split) {
                Art a = Art.getByName(s.toUpperCase());
                if (a == null) return false;
                arts.add(a);
            }
            return true;
        }

        art = Art.getByName(var);
        if (art == null) return false;
        return true;
    }

    @Override
    public boolean check(Player player) {
        return check(player, player);
    }

    @Override
    public boolean check(Player player, LivingEntity target) {
        List<Entity> entities = target.getNearbyEntities(0, 0, 0);

        for (Entity entity : entities) {
            if (!(entity.getType() == EntityType.PAINTING)) continue;
            Painting painting = (Painting) entity;

            if (art != null) return painting.getArt() == art;
            if (!arts.contains(painting.getArt())) return false;

            for (Art art : arts) {
                if (art.equals(painting.getArt())) return true;
            }

            return false;
        }

        return false;
    }

    @Override
    public boolean check(Player player, Location location) {
        return false;
    }
}
