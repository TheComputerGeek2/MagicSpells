package com.nisovin.magicspells.util.ai.goals;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.entity.ai.GoalType;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ai.CustomGoal;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("path_to")
public class PathToGoal extends CustomGoal {

	private ConfigData<Double> speed;
	private ConfigData<Vector> position;
	private ConfigData<Double> distanceAllowed;

	private Location location;
	private int retryTicks;

	public PathToGoal(Mob mob, SpellData data) {
		super(mob, data);
	}

	@Override
	public boolean initialize(@Nullable ConfigurationSection config) {
		if (config == null) return false;
		speed = ConfigDataUtil.getDouble(config, "speed", 1);
		position = ConfigDataUtil.getVector(config, "position", new Vector());
		distanceAllowed = ConfigDataUtil.getDouble(config, "distance-allowed", 1);
		return true;
	}

	private void setLocation() {
		Vector position = PathToGoal.this.position.get(data);
		if (position == null) return;

		location = new Location(mob.getWorld(), position.getX(), position.getY(), position.getZ());
	}

	@Override
	public boolean shouldActivate() {
		setLocation();
		return location != null &&
				location.isChunkLoaded() &&
				location.getWorld().equals(mob.getWorld()) &&
				mob.getLocation().distanceSquared(location) > distanceAllowed.get(data);
	}

	@Override
	public void stop() {
		mob.getPathfinder().stopPathfinding();
	}

	@Override
	public void tick() {
		if (retryTicks > 0) {
			retryTicks--;
			return;
		}
		mob.getPathfinder().moveTo(location, speed.get(data));
		retryTicks = 20;
	}

	@NotNull
	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.MOVE, GoalType.JUMP);
	}

}
