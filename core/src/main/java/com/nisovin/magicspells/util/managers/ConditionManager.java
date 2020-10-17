package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.castmodifiers.conditions.*;

public class ConditionManager {

	private static final Map<String, Class<? extends Condition>> conditions = new HashMap<>();

	public ConditionManager() {
		initialize();
	}

	public Map<String, Class<? extends Condition>> getConditions() {
		return conditions;
	}

	public void addCondition(String name, Class<? extends Condition> condition) {
		conditions.put(name.toLowerCase(), condition);
	}

	public void addCondition(Class<? extends Condition> condition, String name) {
		conditions.put(name.toLowerCase(), condition);
	}

	public void addCondition(Class<? extends Condition> condition, String... names) {
		for (String name : names) {
			conditions.put(name.toLowerCase(), condition);
		}
	}

	public Condition getConditionByName(String name) {
		Class<? extends Condition> clazz = conditions.get(name.toLowerCase());
		if (clazz == null) return null;

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	private void initialize() {
		addCondition("advancement", AdvancementCondition.class);
		addCondition("displayname", DisplayNameCondition.class);
		addCondition("hoveringwith", HoveringWithCondition.class);
		addCondition("day", DayCondition.class);
		addCondition("night", NightCondition.class);
		addCondition("time", TimeCondition.class);
		addCondition("storm", StormCondition.class);
		addCondition("moonphase", MoonPhaseCondition.class);
		addCondition("lightlevel", LightLevelCondition.class);
		addCondition("onblock", OnBlockCondition.class);
		addCondition("inblock", InBlockCondition.class);
		addCondition("onground", OnGroundCondition.class);
		addCondition("underblock", UnderBlockCondition.class);
		addCondition("overblock", OverBlockCondition.class);
		addCondition("inregion", InRegionCondition.class);
		addCondition("incuboid", InCuboidCondition.class);
		addCondition("innomagiczone", InNoMagicZoneCondition.class);
		addCondition("outside", OutsideCondition.class);
		addCondition("roof", RoofCondition.class);
		addCondition("elevation", ElevationCondition.class);
		addCondition("biome", BiomeCondition.class);
		addCondition("sneaking", SneakingCondition.class);
		addCondition("swimming", SwimmingCondition.class);
		addCondition("sprinting", SprintingCondition.class);
		addCondition("flying", FlyingCondition.class);
		addCondition("falling", FallingCondition.class);
		addCondition("blocking", BlockingCondition.class);
		addCondition("riding", RidingCondition.class);
		addCondition("wearing", WearingCondition.class);
		addCondition("wearinginslot", WearingInSlotCondition.class);
		addCondition("holding", HoldingCondition.class);
		addCondition("offhand", OffhandCondition.class);
		addCondition("durability", DurabilityCondition.class);
		addCondition("hasitem", HasItemCondition.class);
		addCondition("hasitemamount", HasItemAmountCondition.class);
		addCondition("openslots", OpenSlotsCondition.class);
		addCondition("onteam", OnTeamCondition.class);
		addCondition("onsameteam", OnSameTeamCondition.class);
		addCondition("health", HealthCondition.class);
		addCondition("absorption", AbsorptionCondition.class);
		addCondition("mana", ManaCondition.class);
		addCondition("maxmana", MaxManaCondition.class);
		addCondition("food", FoodCondition.class);
		addCondition("gamemode", GameModeCondition.class);
		addCondition("level", LevelCondition.class);
		addCondition("magicxpabove", MagicXpAboveCondition.class);
		addCondition("magicxpbelow", MagicXpBelowCondition.class);
		addCondition("pitch", PitchCondition.class);
		addCondition("rotation", RotationCondition.class);
		addCondition("facing", FacingCondition.class);
		addCondition("potioneffect", PotionEffectCondition.class);
		addCondition("onfire", OnFireCondition.class);
		addCondition("buffactive", BuffActiveCondition.class);
		addCondition("ownedbuffactive", OwnedBuffActiveCondition.class);
		addCondition("lastdamagetype", LastDamageTypeCondition.class);
		addCondition("world", InWorldCondition.class);
		addCondition("isnpc", IsNPCCondition.class);
		addCondition("permission", PermissionCondition.class);
		addCondition("playeronline", PlayerOnlineCondition.class);
		addCondition("chance", ChanceCondition.class);
		addCondition("chestcontains", ChestContainsCondition.class);
		addCondition("entitytype", EntityTypeCondition.class);
		addCondition("distance", DistanceCondition.class);
		addCondition("name", NameCondition.class);
		addCondition("namepattern", NamePatternCondition.class);
		addCondition("uptime", UpTimeCondition.class);
		addCondition("variable", VariableCondition.class);
		addCondition("variablematches", VariableMatchesCondition.class);
		addCondition("variablestringequals", VariableStringEqualsCondition.class);
		addCondition("alive", AliveCondition.class);
		addCondition("lastlife", LastLifeCondition.class);
		addCondition("testforblock", TestForBlockCondition.class);
		addCondition("richerthan", RicherThanCondition.class);
		addCondition("lookingatblock", LookingAtBlockCondition.class);
		addCondition("oncooldown", OnCooldownCondition.class);
		addCondition("hasmark", HasMarkCondition.class);
		addCondition("hastarget", HasTargetCondition.class);
		addCondition("playercount", PlayerCountCondition.class);
		addCondition("targetmaxhealth", TargetMaxHealthCondition.class);
		addCondition("worldguardmembership", WorldGuardRegionMembershipCondition.class);
		addCondition("worldguardbooleanflag", WorldGuardBooleanFlagCondition.class);
		addCondition("worldguardstateflag", WorldGuardStateFlagCondition.class);
		addCondition("oxygen", OxygenCondition.class);
		addCondition("yaw", YawCondition.class);
		addCondition("saturation", SaturationCondition.class);
		addCondition("signtext", SignTextCondition.class);
		addCondition("money", MoneyCondition.class);
		addCondition("collection", MultiCondition.class);
		addCondition("age", AgeCondition.class);
		addCondition("targeting", TargetingCondition.class);
		addCondition("power", PowerCondition.class);
		addCondition("spelltag", SpellTagCondition.class);
		addCondition("beneficial", SpellBeneficialCondition.class);
		addCondition("customname", CustomNameCondition.class);
		addCondition("customnamevisible", CustomNameVisibleCondition.class);
		addCondition("canpickupitems", CanPickupItemsCondition.class);
		addCondition("gliding", GlidingCondition.class);
		addCondition("spellcaststate", SpellCastStateCondition.class);
		addCondition("pluginenabled", PluginEnabledCondition.class);
		addCondition("leaping", LeapingCondition.class);
		addCondition("hasitemprecise", HasItemPreciseCondition.class);
		addCondition("wearingprecise", WearingPreciseCondition.class);
		addCondition("holdingprecise", HoldingPreciseCondition.class);
		addCondition("receivingredstone", ReceivingRedstoneCondition.class);
		addCondition("angle", AngleCondition.class);
		addCondition("thundering", ThunderingCondition.class);
		addCondition("raining", RainingCondition.class);
		addCondition("onleash", OnLeashCondition.class);
		addCondition("griefpreventionisowner", GriefPreventionIsOwnerCondition.class);
		addCondition("slotselected", SlotSelectedCondition.class);
		addCondition("hasscoreboardtag", HasScoreboardTagCondition.class);
	}

}
