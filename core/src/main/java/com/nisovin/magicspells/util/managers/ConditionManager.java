package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.DependsOn;
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

	/**
	 * @param condition must be annotated with {@link Name}.
	 */
	public void addCondition(Class<? extends Condition> condition) {
		Name name = condition.getAnnotation(Name.class);
		if (name == null) throw new IllegalStateException("Missing 'Name' annotation on Condition class: " + condition.getName());
		conditions.put(name.value(), condition);
	}

	/**
	 * @deprecated Use {@link ConditionManager#addCondition(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addCondition(String name, Class<? extends Condition> condition) {
		conditions.put(name.toLowerCase(), condition);
	}

	/**
	 * @deprecated Use {@link ConditionManager#addCondition(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addCondition(Class<? extends Condition> condition, String name) {
		conditions.put(name.toLowerCase(), condition);
	}

	/**
	 * @deprecated Use {@link ConditionManager#addCondition(Class)}
	 */
	@Deprecated(forRemoval = true)
	public void addCondition(Class<? extends Condition> condition, String... names) {
		for (String name : names) {
			conditions.put(name.toLowerCase(), condition);
		}
	}

	public Condition getConditionByName(String name) {
		Class<? extends Condition> clazz = conditions.get(name.toLowerCase());
		if (clazz == null) return null;

		DependsOn dependsOn = clazz.getAnnotation(DependsOn.class);
		if (dependsOn != null && !Util.checkPluginsEnabled(dependsOn.value())) return null;

		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	private void initialize() {
		addCondition(AdvancementCondition.class);
		addCondition(DataCondition.class);
		addCondition(DisplayNameCondition.class);
		addCondition(HoveringWithCondition.class);
		addCondition(DayCondition.class);
		addCondition(NightCondition.class);
		addCondition(TimeCondition.class);
		addCondition(StormCondition.class);
		addCondition(MoonPhaseCondition.class);
		addCondition(LightLevelCondition.class);
		addCondition(LineOfSightCondition.class);
		addCondition(OnBlockCondition.class);
		addCondition(InBlockCondition.class);
		addCondition(OnGroundCondition.class);
		addCondition(OverGroundCondition.class);
		addCondition(UnderBlockCondition.class);
		addCondition(OverBlockCondition.class);
		addCondition(InRegionCondition.class);
		addCondition(InCuboidCondition.class);
		addCondition(InNoMagicZoneCondition.class);
		addCondition(OutsideCondition.class);
		addCondition(RoofCondition.class);
		addCondition(ElevationCondition.class);
		addCondition(BiomeCondition.class);
		addCondition(SneakingCondition.class);
		addCondition(SwimmingCondition.class);
		addCondition(SprintingCondition.class);
		addCondition(FlyingCondition.class);
		addCondition(FallingCondition.class);
		addCondition(BlockingCondition.class);
		addCondition(RidingCondition.class);
		addCondition(RiptidingCondition.class);
		addCondition(WearingCondition.class);
		addCondition(WearingInSlotCondition.class);
		addCondition(HoldingCondition.class);
		addCondition(OffhandCondition.class);
		addCondition(OffHandPreciseCondition.class);
		addCondition(DurabilityCondition.class);
		addCondition(HasItemCondition.class);
		addCondition(HasItemAmountCondition.class);
		addCondition(OpenSlotsCondition.class);
		addCondition(OnTeamCondition.class);
		addCondition(OnSameTeamCondition.class);
		addCondition(HealthCondition.class);
		addCondition(AbsorptionCondition.class);
		addCondition(ManaCondition.class);
		addCondition(MaxManaCondition.class);
		addCondition(FoodCondition.class);
		addCondition(GameModeCondition.class);
		addCondition(LevelCondition.class);
		addCondition(MagicXpAboveCondition.class);
		addCondition(MagicXpBelowCondition.class);
		addCondition(PitchCondition.class);
		addCondition(RotationCondition.class);
		addCondition(FacingCondition.class);
		addCondition(PotionEffectCondition.class);
		addCondition(OnFireCondition.class);
		addCondition(BuffActiveCondition.class);
		addCondition(OwnedBuffActiveCondition.class);
		addCondition(LastDamageTypeCondition.class);
		addCondition(InWorldCondition.class);
		addCondition(IsNPCCondition.class);
		addCondition(PermissionCondition.class);
		addCondition(PlayerOnlineCondition.class);
		addCondition(ChanceCondition.class);
		addCondition(ChestContainsCondition.class);
		addCondition(EntityTypeCondition.class);
		addCondition(DistanceCondition.class);
		addCondition(NameCondition.class);
		addCondition(NamePatternCondition.class);
		addCondition(UpTimeCondition.class);
		addCondition(VariableCondition.class);
		addCondition(VariableMatchesCondition.class);
		addCondition(VariableStringEqualsCondition.class);
		addCondition(AliveCondition.class);
		addCondition(LastLifeCondition.class);
		addCondition(TestForBlockCondition.class);
		addCondition(RicherThanCondition.class);
		addCondition(LookingAtBlockCondition.class);
		addCondition(OnCooldownCondition.class);
		addCondition(HasMarkCondition.class);
		addCondition(HasTargetCondition.class);
		addCondition(PlayerCountCondition.class);
		addCondition(TargetMaxHealthCondition.class);
		addCondition(WorldGuardRegionMembershipCondition.class);
		addCondition(WorldGuardBooleanFlagCondition.class);
		addCondition(WorldGuardStateFlagCondition.class);
		addCondition(OxygenCondition.class);
		addCondition(YawCondition.class);
		addCondition(SaturationCondition.class);
		addCondition(SignTextCondition.class);
		addCondition(MoneyCondition.class);
		addCondition(MultiCondition.class);
		addCondition(AgeCondition.class);
		addCondition(TargetingCondition.class);
		addCondition(PowerCondition.class);
		addCondition(SpellTagCondition.class);
		addCondition(SpellSelectedCondition.class);
		addCondition(SpellBeneficialCondition.class);
		addCondition(CustomNameCondition.class);
		addCondition(CustomNameVisibleCondition.class);
		addCondition(CanPickupItemsCondition.class);
		addCondition(GlidingCondition.class);
		addCondition(SpellCastStateCondition.class);
		addCondition(PluginEnabledCondition.class);
		addCondition(LeapingCondition.class);
		addCondition(HasItemPreciseCondition.class);
		addCondition(WearingPreciseCondition.class);
		addCondition(HoldingPreciseCondition.class);
		addCondition(ReceivingRedstoneCondition.class);
		addCondition(AngleCondition.class);
		addCondition(ThunderingCondition.class);
		addCondition(RainingCondition.class);
		addCondition(OnLeashCondition.class);
		addCondition(GriefPreventionIsOwnerCondition.class);
		addCondition(SlotSelectedCondition.class);
		addCondition(HasScoreboardTagCondition.class);
		addCondition(HasSpellCondition.class);
		addCondition(LoopActiveCondition.class);
		addCondition(OwnedLoopActiveCondition.class);
		addCondition(AlwaysCondition.class);
		addCondition(VelocityActiveCondition.class);
		addCondition(BuildableCondition.class);
		addCondition(BurnableCondition.class);
		addCondition(CollidableCondition.class);
		addCondition(PassableCondition.class);
		addCondition(ReplaceableCondition.class);
		addCondition(SolidCondition.class);
		addCondition(PoseCondition.class);
		addCondition(FixedPoseCondition.class);
		addCondition(SilentCondition.class);
		addCondition(ClientBrandNameCondition.class);
		addCondition(AttributeCondition.class);
		addCondition(AttributeBaseCondition.class);
		addCondition(AttributeDefaultCondition.class);
		addCondition(PulserActiveCondition.class);
		addCondition(TotemActiveCondition.class);
		addCondition(BlockTagCondition.class);
		addCondition(CastCondition.class);
		addCondition(ClimbingCondition.class);
		addCondition(EntityTypeTagCondition.class);
		addCondition(LeashedCondition.class);
		addCondition(SleepingCondition.class);
		addCondition(UnderWaterCondition.class);
		addCondition(FixedTimeCondition.class);
	}

}
