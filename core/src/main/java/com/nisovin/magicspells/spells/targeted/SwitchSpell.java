package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SwitchSpell extends TargetedSpell implements TargetedEntitySpell {

	private int switchBack;
	
	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		switchBack = getConfigInt("switch-back", 0);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			
			playSpellEffects(caster, target.getTarget());
			switchPlaces(caster, target.getTarget(), target.getPower(), args);
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		switchPlaces(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void switchPlaces(LivingEntity player, final LivingEntity target, float power, String[] args) {
		Location targetLoc = target.getLocation();
		Location casterLoc = player.getLocation();
		player.teleport(targetLoc);
		target.teleport(casterLoc);

		if (switchBack <= 0) return;

		MagicSpells.scheduleDelayedTask(() -> {
			if (player.isDead() || target.isDead()) return;
			Location targetLoc1 = target.getLocation();
			Location casterLoc1 = player.getLocation();
			player.teleport(targetLoc1);
			target.teleport(casterLoc1);
		}, switchBack);
	}

}
