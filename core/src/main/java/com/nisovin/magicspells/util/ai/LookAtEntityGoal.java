package com.nisovin.magicspells.util.ai;

import org.bukkit.entity.Mob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

public class LookAtEntityGoal implements Goal<Mob> {
    private final Mob mob;
    private final Class<? extends LivingEntity> targetType;
    private final float range;
    private final float chance;

    private Entity target;
    private int lookTime;

    public LookAtEntityGoal(Mob mob, Class<? extends LivingEntity> targetType, float range, float chance) {
        this.mob = mob;
        this.targetType = targetType;
        this.range = range;
        this.chance = chance;
    }

    @Override
    public boolean shouldActivate() {
        if (ThreadLocalRandom.current().nextFloat() >= this.chance) {
            return false;
        }
        if (this.mob.getTarget() != null) {
            this.target = this.mob.getTarget();
        }
        if (this.targetType.isAssignableFrom(HumanEntity.class)) {
            this.target = Util.getNearestEntity(this.mob, range, entity -> this.rides(entity).and(this.targetType::isInstance).test(entity));
        } else {
            this.target = Util.getNearestEntity(this.mob, range, this.targetType::isInstance);
        }
        return this.target != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (this.target.isDead()) {
            return false;
        }
        if (this.mob.getLocation().distanceSquared(this.target.getLocation()) > (this.range * this.range)) {
            return false;
        }
        return this.lookTime > 0;
    }

    @Override
    public void start() {
        this.lookTime = ThreadLocalRandom.current().nextInt(40, 80);
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        this.mob.teleport(this.mob.getLocation().setDirection(this.target.getLocation().subtract(this.mob.getLocation()).toVector()));
        this.lookTime--;
    }

    @NotNull
    @Override
    public GoalKey<Mob> getKey() {
        return GoalKey.of(Mob.class, new NamespacedKey(MagicSpells.getInstance(), "lookatentity"));
    }

    @NotNull
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }

    private Predicate<Entity> rides(Entity vehicle) {
        return passenger -> {
            while (passenger.getVehicle() != null) {
                passenger = passenger.getVehicle();
                if (passenger == vehicle) {
                    return false;
                }
            }
            return true;
        };
    }
}
