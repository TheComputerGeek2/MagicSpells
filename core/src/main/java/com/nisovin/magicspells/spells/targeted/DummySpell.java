package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class DummySpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, TargetedEntityFromLocationSpell {

	public DummySpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target == null) return noTarget(caster);

			playSpellEffects(caster, target.getTarget());
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		playSpellEffects(from, target);
		return true;
	}
	
	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		playSpellEffects(from, target);
		return true;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return true;
	}
	
}
