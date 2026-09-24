package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ai.CustomGoal;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class MobGoalEditSpell extends TargetedSpell implements TargetedEntitySpell {

	private final List<GoalData> add = new ArrayList<>();
	private final List<GoalKey<@NotNull Mob>> remove = new ArrayList<>();
	private final EnumSet<GoalType> removeTypes = EnumSet.noneOf(GoalType.class);
	private final List<GoalKey<?>> removeVanilla = new ArrayList<>();

	private final ConfigData<Boolean> removeAll;

	public MobGoalEditSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		removeAll = getConfigDataBoolean("remove-all", false);

		List<String> removeTypeStrings = getConfigStringList("remove-types", null);
		if (removeTypeStrings != null) {
			for (String string : removeTypeStrings) {
				GoalType type = Util.enumValueSafe(GoalType.class, string);
				if (type == null) {
					MagicSpells.error("MobGoalEditSpell '" + internalName + "' lists an invalid value in 'remove-types': " + string);
					continue;
				}
				removeTypes.add(type);
			}
		}

		List<String> removeStrings = getConfigStringList("remove", null);
		if (removeStrings != null) {
			for (String string : removeStrings) {
				NamespacedKey key = null;
				try {
					key = NamespacedKey.fromString(string, MagicSpells.getInstance());
				} catch (IllegalArgumentException ignored) {}
				if (key == null) {
					MagicSpells.error("MobGoalEditSpell '" + internalName + "' lists an invalid key in 'remove': " + string);
					continue;
				}
				remove.add(GoalKey.of(Mob.class, key));
			}
		}
	}

	@Override
	protected void initialize() {
		super.initialize();

		List<String> removeVanillaStrings = getConfigStringList("remove-vanilla", null);
		if (removeVanillaStrings != null) {
			for (String string : removeVanillaStrings) {
				GoalKey<?> vanillaGoal = MagicSpells.getCustomGoalsManager().getVanillaGoal(string);
				if (vanillaGoal == null) {
					MagicSpells.error("MobGoalEditSpell '" + internalName + "' lists an invalid vanilla goal in 'remove-vanilla': " + string);
					continue;
				}
				removeVanilla.add(vanillaGoal);
			}
		}

		List<?> addList = getConfigList("add", null);
		if (addList != null) {
			for (Object object : addList) {
				if (!(object instanceof Map<?, ?> map)) continue;
				ConfigurationSection section = ConfigReaderUtil.mapToSection(map);

				int priority = section.getInt("priority", 0);
				String goalName = section.getString("goal", "").toLowerCase();
				ConfigurationSection goalSection = section.getConfigurationSection("data");

				if (!MagicSpells.getCustomGoalsManager().getGoals().containsKey(goalName)) {
					MagicSpells.error("MobGoalEditSpell '" + internalName + "' lists an invalid goal name: '" + goalName + "'");
					continue;
				}

				add.add(new GoalData(goalName, priority, goalSection));
			}
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Mob mob)) return noTarget(data);

		if (removeAll.get(data)) Bukkit.getMobGoals().removeAllGoals(mob);

		for (GoalType type : removeTypes) {
			Bukkit.getMobGoals().removeAllGoals(mob, type);
		}

		for (GoalKey<@NotNull Mob> key : remove) {
			Bukkit.getMobGoals().removeGoal(mob, key);
		}

		for (GoalKey<?> key : removeVanilla) {
			// We have to loop through because casting to parameter types is tricky.
			// It loops through on each MobGoals#removeGoal call anyway.
			for (Goal<@NotNull Mob> goal : Bukkit.getMobGoals().getAllGoals(mob)) {
				if (!goal.getKey().equals(key)) continue;
				Bukkit.getMobGoals().removeGoal(mob, goal);
			}
		}

		for (GoalData goalData : add) {
			CustomGoal goal = MagicSpells.getCustomGoalsManager().getGoal(goalData.goalName(), mob, data);
			if (goal == null) {
				MagicSpells.error("MobGoalEditSpell '" + internalName + "' lists an invalid goal name on 'add': '" + goalData.goalName() + "'");
				continue;
			}
			boolean success = goal.initialize(goalData.section());
			if (!success) {
				MagicSpells.error("MobGoalEditSpell '" + internalName +"' is missing a 'data' option under 'add' for goal: " + goalData.goalName());
				continue;
			}
			Bukkit.getMobGoals().addGoal(mob, goalData.priority(), goal);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private record GoalData(String goalName, int priority, ConfigurationSection section) {}

}
