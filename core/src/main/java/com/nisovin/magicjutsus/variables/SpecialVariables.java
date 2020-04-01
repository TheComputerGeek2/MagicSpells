package com.nisovin.magicjutsus.variables;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import com.nisovin.magicjutsus.variables.meta.*;

// TODO look into GENERIC_FLYING_SPEED attribute
// TODO look into GENERIC_MOVEMENT_SPEED attribute
public class SpecialVariables {

	private static Map<String, Variable> specialVariables;
	
	static {
		specialVariables = new HashMap<>();
		specialVariables.put("meta_location_x", new CoordXVariable());
		specialVariables.put("meta_location_y", new CoordYVariable());
		specialVariables.put("meta_location_z", new CoordZVariable());
		specialVariables.put("meta_location_pitch", new CoordPitchVariable());
		specialVariables.put("meta_location_yaw", new CoordYawVariable());
		specialVariables.put("meta_saturation", new SaturationVariable());
		specialVariables.put("meta_experience_level", new ExperienceLevelVariable());
		specialVariables.put("meta_experience_points", new ExperienceVariable());
		specialVariables.put("meta_remaining_air", new RemainingAirVariable());
		specialVariables.put("meta_max_air", new MaximumAirVariable());
		specialVariables.put("meta_fly_speed", new FlySpeedVariable());
		specialVariables.put("meta_walk_speed", new WalkSpeedVariable());
		specialVariables.put("meta_food_level", new FoodLevelVariable());
		specialVariables.put("meta_entity_id", new EntityIDVariable());
		specialVariables.put("meta_fire_ticks", new FireTicksVariable());
		specialVariables.put("meta_fall_distance", new FallDistanceVariable());
		specialVariables.put("meta_players_online", new PlayersOnlineVariable());
		specialVariables.put("meta_max_health", new MaxHealthVariable());
		specialVariables.put("meta_health_scale", new HealthScaleVariable());
		specialVariables.put("meta_compass_target_x", new CompassTargetXVariable());
		specialVariables.put("meta_compass_target_y", new CompassTargetYVariable());
		specialVariables.put("meta_compass_target_z", new CompassTargetZVariable());
		specialVariables.put("meta_velocity_x", new VelocityXVariable());
		specialVariables.put("meta_velocity_y", new VelocityYVariable());
		specialVariables.put("meta_velocity_z", new VelocityZVariable());
		specialVariables.put("meta_no_damage_ticks", new NoDamageTicksVariable());
		specialVariables.put("meta_max_no_damage_ticks", new MaximumNoDamageTicksVariable());
		specialVariables.put("meta_last_damage", new LastDamageVariable());
		specialVariables.put("meta_sleep_ticks", new SleepTicksVariable());
		specialVariables.put("meta_bed_location_x", new BedCoordXVariable());
		specialVariables.put("meta_bed_location_y", new BedCoordYVariable());
		specialVariables.put("meta_bed_location_z", new BedCoordZVariable());
		specialVariables.put("meta_altitude", new AltitudeVariable());

		specialVariables.put("meta_attribute_generic_armor_base", new AttributeBaseValueVariable("GENERIC_ARMOR"));
		specialVariables.put("meta_attribute_generic_armor_toughness_base", new AttributeBaseValueVariable("GENERIC_ARMOR_TOUGHNESS"));
		specialVariables.put("meta_attribute_generic_attack_damage_base", new AttributeBaseValueVariable("GENERIC_ATTACK_DAMAGE"));
		specialVariables.put("meta_attribute_generic_attack_speed_base", new AttributeBaseValueVariable("GENERIC_ATTACK_SPEED"));
		specialVariables.put("meta_attribute_generic_knockback_resistance_base", new AttributeBaseValueVariable("GENERIC_KNOCKBACK_RESISTANCE"));
		specialVariables.put("meta_attribute_generic_luck_base", new AttributeBaseValueVariable("GENERIC_LUCK"));
		specialVariables.put("meta_attribute_generic_max_health_base", new AttributeBaseValueVariable("GENERIC_MAX_HEALTH"));
	}
	
	public static Map<String, Variable> getSpecialVariables() {
		return Collections.unmodifiableMap(specialVariables);
	}
	
}
