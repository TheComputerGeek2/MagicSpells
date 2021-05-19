package com.nisovin.magicspells.util.ai;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import com.nisovin.magicspells.MagicSpells;

public class PathToGoal implements Goal<Mob> {

    private final Mob mob;

    private LivingEntity target;
    private double speed;

    public PathToGoal(@NotNull Mob mob, @NotNull LivingEntity target, double speed) {
        this.target = target;
        this.speed = speed;
        this.mob = mob;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public boolean shouldActivate() {
        return target != null && target.isValid() && !target.isDead();
    }

    @Override
    public boolean shouldStayActive() {
        if (target == null || !target.isValid() || target.isDead()) return false;

        Location mobLocation = mob.getLocation();
        Location targetLocation = target.getLocation();
        return mobLocation.getWorld().equals(targetLocation.getWorld()) && mobLocation.distanceSquared(targetLocation) > 4;
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void tick() {
        mob.getPathfinder().moveTo(target, speed);
    }

    @NotNull
    @Override
    public GoalKey<Mob> getKey() {
        return GoalKey.of(Mob.class, new NamespacedKey(MagicSpells.getInstance(), "pathto"));
    }

    @NotNull
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

}