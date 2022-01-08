package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Objective;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;

public class ScoreboardDataSpell extends TargetedSpell implements TargetedEntitySpell {

	private String variableName;
	private String objectiveName;
	private Objective objective;

	public ScoreboardDataSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		variableName = getConfigString("variable-name", "");
		objectiveName = getConfigString("objective-name", "");
	}

	@Override
	public void initialize() {
		if (objectiveName == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an invalid objective name defined for objective-name!");
			return;
		}

		objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an objective name defined for objective-name that could not be resolved as an existing objective!");
			objectiveName = null;
		}
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();

		if (variableName.isEmpty() || MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("ScoreboardDataSpell '" + internalName + "' has an invalid variable-name defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player player) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power, args);
			if (targetInfo == null) return noTarget(player);

			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(player);

			setScore(player, target);

			playSpellEffects(player, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(caster instanceof Player player)) return false;

		setScore(player, target);

		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void setScore(Player caster, LivingEntity target) {
		if (objective == null) return;

		Variable variable = MagicSpells.getVariableManager().getVariable(variableName);
		if (variable == null) return;

		int score = objective.getScoreFor(target).getScore();

		if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable)
			variable.parseAndSet(caster, String.valueOf(score));
		else variable.set(caster, score);
	}

}