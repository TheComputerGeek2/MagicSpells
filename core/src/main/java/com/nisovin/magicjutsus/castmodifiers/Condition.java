package com.nisovin.magicjutsus.castmodifiers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.castmodifiers.conditions.*;

public abstract class Condition {

	public abstract boolean setVar(String var);

	public abstract boolean check(LivingEntity livingEntity);

	public abstract boolean check(LivingEntity livingEntity, LivingEntity target);

	public abstract boolean check(LivingEntity livingEntity, Location location);

	private static Map<String, Class<? extends Condition>> conditions = new HashMap<>();

	public static void addCondition(String name, Class<? extends Condition> condition) {
		conditions.put(name.toLowerCase(), condition);
	}

	static Condition getConditionByName(String name) {
		Class<? extends Condition> clazz = conditions.get(name.toLowerCase());
		if (clazz == null) {
			if (name.toLowerCase().startsWith("addon")) {
				// If it starts with addon, then load it as an addon provided condition
				return new ProxyCondition(name.replaceFirst("addon:", ""));
			}
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
		conditions.put("displayname", DisplayNameCondition.class);
		conditions.put("day", DayCondition.class);
		conditions.put("night", NightCondition.class);
		conditions.put("time", TimeCondition.class);
		conditions.put("storm", StormCondition.class);
		conditions.put("moonphase", MoonPhaseCondition.class);
		conditions.put("lightlevel", LightLevelCondition.class);
		conditions.put("onblock", OnBlockCondition.class);
		conditions.put("inblock", InBlockCondition.class);
		conditions.put("onground", OnGroundCondition.class);
		conditions.put("underblock", UnderBlockCondition.class);
		conditions.put("overblock", OverBlockCondition.class);
		conditions.put("inregion", InRegionCondition.class);
		conditions.put("incuboid", InCuboidCondition.class);
		conditions.put("innomagiczone", InNoMagicZoneCondition.class);
		conditions.put("outside", OutsideCondition.class);
		conditions.put("roof", RoofCondition.class);
		conditions.put("elevation", ElevationCondition.class);
		conditions.put("biome", BiomeCondition.class);
		conditions.put("sneaking", SneakingCondition.class);
		conditions.put("swimming", SwimmingCondition.class);
		conditions.put("sprinting", SprintingCondition.class);
		conditions.put("flying", FlyingCondition.class);
		conditions.put("falling", FallingCondition.class);
		conditions.put("blocking", BlockingCondition.class);
		conditions.put("riding", RidingCondition.class);
		conditions.put("wearing", WearingCondition.class);
		conditions.put("wearinginslot", WearingInSlotCondition.class);
		conditions.put("holding", HoldingCondition.class);
		conditions.put("offhand", OffhandCondition.class);
		conditions.put("durability", DurabilityCondition.class);
		conditions.put("hasitem", HasItemCondition.class);
		conditions.put("hasitemamount", HasItemAmountCondition.class);
		conditions.put("openslots", OpenSlotsCondition.class);
		conditions.put("onteam", OnTeamCondition.class);
		conditions.put("onsameteam", OnSameTeamCondition.class);
		conditions.put("health", HealthCondition.class);
		conditions.put("absorption", AbsorptionCondition.class);
		conditions.put("mana", ManaCondition.class);
		conditions.put("maxmana", MaxManaCondition.class);
		conditions.put("food", FoodCondition.class);
		conditions.put("gamemode", GameModeCondition.class);
		conditions.put("level", LevelCondition.class);
		conditions.put("magicxpabove", MagicXpAboveCondition.class);
		conditions.put("magicxpbelow", MagicXpBelowCondition.class);
		conditions.put("pitch", PitchCondition.class);
		conditions.put("rotation", RotationCondition.class);
		conditions.put("facing", FacingCondition.class);
		conditions.put("potioneffect", PotionEffectCondition.class);
		conditions.put("onfire", OnFireCondition.class);
		conditions.put("buffactive", BuffActiveCondition.class);
		conditions.put("lastdamagetype", LastDamageTypeCondition.class);
		conditions.put("world", InWorldCondition.class);
		conditions.put("isnpc", IsNPCCondition.class);
		conditions.put("permission", PermissionCondition.class);
		conditions.put("playeronline", PlayerOnlineCondition.class);
		conditions.put("chance", ChanceCondition.class);
		conditions.put("entitytype", EntityTypeCondition.class);
		conditions.put("distance", DistanceCondition.class);
		conditions.put("name", NameCondition.class);
		conditions.put("namepattern", NamePatternCondition.class);
		conditions.put("uptime", UpTimeCondition.class);
		conditions.put("variable", VariableCondition.class);
		conditions.put("variablecompare", VariableCompareCondition.class);
		conditions.put("variablematches", VariableMatchesCondition.class);
		conditions.put("variablestringequals", VariableStringEqualsCondition.class);
		conditions.put("alive", AliveCondition.class);
		conditions.put("lastlife", LastLifeCondition.class);
		conditions.put("testforblock", TestForBlockCondition.class);
		conditions.put("richerthan", RicherThanCondition.class);
		conditions.put("lookingatblock", LookingAtBlockCondition.class);
		conditions.put("oncooldown", OnCooldownCondition.class);
		conditions.put("hasmark", HasMarkCondition.class);
		conditions.put("hastarget", HasTargetCondition.class);
		conditions.put("playercount", PlayerCountCondition.class);
		conditions.put("targetmaxhealth", TargetMaxHealthCondition.class);
		conditions.put("worldguardmembership", WorldGuardRegionMembershipCondition.class);
		conditions.put("worldguardbooleanflag", WorldGuardBooleanFlagCondition.class);
		conditions.put("worldguardstateflag", WorldGuardStateFlagCondition.class);
		conditions.put("oxygen", OxygenCondition.class);
		conditions.put("yaw", YawCondition.class);
		conditions.put("saturation", SaturationCondition.class);
		conditions.put("money", MoneyCondition.class);
		conditions.put("collection", MultiCondition.class);
		conditions.put("age", AgeCondition.class);
		conditions.put("targeting", TargetingCondition.class);
		conditions.put("power", PowerCondition.class);
		conditions.put("jutsutag", JutsuTagCondition.class);
		conditions.put("beneficial", JutsuBeneficialCondition.class);
		conditions.put("customname", CustomNameCondition.class);
		conditions.put("customnamevisible", CustomNameVisibleCondition.class);
		conditions.put("canpickupitems", CanPickupItemsCondition.class);
		conditions.put("gliding", GlidingCondition.class);
		conditions.put("jutsucaststate", JutsuCastStateCondition.class);
		conditions.put("pluginenabled", PluginEnabledCondition.class);
		conditions.put("leaping", LeapingCondition.class);
		conditions.put("hasitemprecise", HasItemPreciseCondition.class);
		conditions.put("wearingprecise", WearingPreciseCondition.class);
		conditions.put("holdingprecise", HoldingPreciseCondition.class);
		conditions.put("receivingredstone", ReceivingRedstoneCondition.class);
		conditions.put("behindtarget", BehindTargetCondition.class);
		conditions.put("thundering", ThunderingCondition.class);
		conditions.put("raining", RainingCondition.class);
		conditions.put("onleash", OnLeashCondition.class);
		conditions.put("griefpreventionisowner", GriefPreventionIsOwnerCondition.class);
		conditions.put("slotselected", SlotSelectedCondition.class);
	}

}
