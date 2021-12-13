package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class CloseInventorySpell extends TargetedSpell implements TargetedEntitySpell {

	private final int delay;

	public CloseInventorySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		delay = getConfigInt("delay", 0);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power);
			if (targetInfo == null) return noTarget(caster);

			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(caster);

			close(caster, target, target, power, args);
			playSpellEffects(caster, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!(target instanceof Player player)) return false;
		close(caster, target, player, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		return castAtEntity(null, target, power, args);

	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power, null);
	}

	private void close(LivingEntity caster, LivingEntity target, Player playerTarget, float power, String[] args) {
		if (delay > 0) MagicSpells.scheduleDelayedTask(playerTarget::closeInventory, delay);
		else playerTarget.closeInventory();
	}

}
