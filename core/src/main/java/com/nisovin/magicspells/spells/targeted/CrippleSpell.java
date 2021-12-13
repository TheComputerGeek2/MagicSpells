package com.nisovin.magicspells.spells.targeted;

import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CrippleSpell extends TargetedSpell implements TargetedEntitySpell {

	private int strength;
	private int duration;
	private int portalCooldown;

	private boolean useSlownessEffect;
	private boolean applyPortalCooldown;

	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
		portalCooldown = getConfigInt("portal-cooldown-ticks", 100);

		useSlownessEffect = getConfigBoolean("use-slowness-effect", true);
		applyPortalCooldown = getConfigBoolean("apply-portal-cooldown", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);

			cripple(caster, target.getTarget(), power, args);
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		cripple(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		cripple(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void cripple(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (target == null) return;

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
		
		if (useSlownessEffect) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration * power), strength));
		if (applyPortalCooldown && target.getPortalCooldown() < (int) (portalCooldown * power)) target.setPortalCooldown((int) (portalCooldown * power));
	}

}
