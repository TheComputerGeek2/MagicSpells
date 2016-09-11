package com.nisovin.magicspells.castmodifiers;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.*;

public abstract class Condition {
	
	public abstract boolean setVar(String var);

	public abstract boolean check(Player player);
	
	public abstract boolean check(Player player, LivingEntity target);
	
	public abstract boolean check(Player player, Location location);
	
	private static HashMap<String, Class<? extends Condition>> conditions = new HashMap<String, Class<? extends Condition>>();
	
	public static void addCondition(String name, Class<? extends Condition> condition) {
		conditions.put(name.toLowerCase(), condition);
	}
	
	static Condition getConditionByName(String name) {
		Class<? extends Condition> clazz = conditions.get(name.toLowerCase());
		if (clazz == null) {
			return null;
		}
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}
	
	static {
		conditions.put("disguised", DisguisedCondition.class);
		conditions.put("day", DayCondition.class);
		conditions.put("night", NightCondition.class);
		conditions.put("time", TimeCondition.class);
		conditions.put("storm", StormCondition.class);
		conditions.put("moonphase", MoonPhaseCondition.class);
		conditions.put("lightlevelabove", LightLevelAboveCondition.class);
		conditions.put("lightlevelbelow", LightLevelBelowCondition.class);
		conditions.put("onblock", OnBlockCondition.class);
		conditions.put("inblock", InBlockCondition.class);
		conditions.put("onground", OnGroundCondition.class);
		conditions.put("inregion", InRegionCondition.class);
		conditions.put("incuboid", InCuboidCondition.class);
		conditions.put("innomagiczone", InNoMagicZoneCondition.class);
		conditions.put("outside", OutsideCondition.class);
		conditions.put("roof", RoofCondition.class);
		conditions.put("elevationabove", ElevationAboveCondition.class);
		conditions.put("elevationbelow", ElevationBelowCondition.class);
		conditions.put("biome", BiomeCondition.class);
		conditions.put("sneaking", SneakingCondition.class);
		conditions.put("sprinting", SprintingCondition.class);
		conditions.put("flying", FlyingCondition.class);
		conditions.put("falling", FallingCondition.class);
		conditions.put("blocking", BlockingCondition.class);
		conditions.put("riding", RidingCondition.class);
		conditions.put("wearing", WearingCondition.class);
		conditions.put("wearinginslot", WearingInSlotCondition.class);
		conditions.put("holding", HoldingCondition.class);
		conditions.put("offhand", OffhandCondition.class);
		conditions.put("durabilitylessthan", DurabilityLessThanCondition.class);
		conditions.put("hasitem", HasItemCondition.class);
		conditions.put("hasitemlessthan", HasItemLessThanCondition.class);
		conditions.put("hasitemmorethan", HasItemMoreThanCondition.class);
		conditions.put("openslotslessthan", OpenSlotsLessThanCondition.class);
		conditions.put("openslotsmorethan", OpenSlotsMoreThanCondition.class);
		conditions.put("onteam", OnTeamCondition.class);
		conditions.put("onsameteam", OnSameTeamCondition.class);
		conditions.put("healthabove", HealthAboveCondition.class);
		conditions.put("healthbelow", HealthBelowCondition.class);
		conditions.put("absorptionlessthan", AbsorptionLessThanCondition.class);
		conditions.put("absorptionmorethan", AbsorptionMoreThanCondition.class);
		conditions.put("manaabove", ManaAboveCondition.class);
		conditions.put("manabelow", ManaBelowCondition.class);
		conditions.put("foodabove", FoodAboveCondition.class);
		conditions.put("foodbelow", FoodBelowCondition.class);
		conditions.put("levelabove", LevelAboveCondition.class);
		conditions.put("levelbelow", LevelBelowCondition.class);
		conditions.put("magicxpabove", MagicXpAboveCondition.class);
		conditions.put("magicxpbelow", MagicXpBelowCondition.class);
		conditions.put("pitchabove", PitchAboveCondition.class);
		conditions.put("pitchbelow", PitchBelowCondition.class);
		conditions.put("rotationabove", RotationAboveCondition.class);
		conditions.put("rotationbelow", RotationBelowCondition.class);
		conditions.put("facing", FacingCondition.class);
		conditions.put("potioneffect", PotionEffectCondition.class);
		conditions.put("onfire", OnFireCondition.class);
		conditions.put("buffactive", BuffActiveCondition.class);
		conditions.put("lastdamagetype", LastDamageTypeCondition.class);
		conditions.put("world", InWorldCondition.class);
		conditions.put("permission", PermissionCondition.class);
		conditions.put("playeronline", PlayerOnlineCondition.class);
		conditions.put("chance", ChanceCondition.class);
		conditions.put("entitytype", EntityTypeCondition.class);
		conditions.put("distancemorethan", DistanceMoreThan.class);
		conditions.put("distancelessthan", DistanceLessThan.class);
		conditions.put("name", NameCondition.class);
		conditions.put("namepattern", NamePatternCondition.class);
		conditions.put("uptime", UpTimeCondition.class);
		conditions.put("variablemorethan", VariableMoreThanCondition.class);
		conditions.put("variablelessthan", VariableLessThanCondition.class);
		conditions.put("variableequals", VariableEqualsCondition.class);
		conditions.put("alivelessthan", AliveLessThan.class);
		conditions.put("alivemorethan", AliveMoreThan.class);
		conditions.put("lastlifelongerthan", LastLifeLongerThan.class);
		conditions.put("lastlifeshorterthan", LastLifeShorterThan.class);
		conditions.put("testforblock", TestForBlockCondition.class);
		conditions.put("richerthan", RicherThanCondition.class);
		conditions.put("lookingatblock", LookingAtBlockCondition.class);
		conditions.put("oncooldown", OnCooldownCondition.class);
		conditions.put("hasmark", HasMarkCondition.class);
		conditions.put("playercountabove", PlayerCountAbove.class);
		conditions.put("targetmaxhealthgreaterthan", TargetMaxHealthGreaterThanCondition.class);
		conditions.put("targetmaxhealthlessthan", TargetMaxHealthLessThanCondition.class);
<<<<<<< HEAD
<<<<<<< HEAD
=======
		conditions.put("worldguardmembership", WorldGuardRegionMembershipCondition.class);
		conditions.put("worldguardbooleanflag", WorldGuardBooleanFlagCondition.class);
		conditions.put("worldguardstateflag", WorldGuardStateFlagCondition.class);
>>>>>>> refs/remotes/TheComputerGeek2/master
		conditions.put("oxygenabove", OxygenAboveCondition.class);
		conditions.put("oxygenbelow", OxygenBelowCondition.class);
		conditions.put("oxygenequals", OxygenEqualsCondition.class);
		conditions.put("yawabove", YawAboveCondition.class);
		conditions.put("yawbelow", YawBelowCondition.class);
<<<<<<< HEAD
=======
		conditions.put("worldguardmembership", WorldGuardRegionMembershipCondition.class);
		conditions.put("worldguardbooleanflag", WorldGuardBooleanFlagCondition.class);
		conditions.put("worldguardstateflag", WorldGuardStateFlagCondition.class);
>>>>>>> refs/remotes/TheComputerGeek2/master
=======
		conditions.put("saturationabove", SaturationAboveCondition.class);
		conditions.put("saturationbelow", SaturationBelowCondition.class);
		conditions.put("moneymorethan", MoneyMoreThanCondition.class);
		conditions.put("moneylessthan", MoneyLessThanCondition.class);
>>>>>>> refs/remotes/TheComputerGeek2/master
	}
	
}
