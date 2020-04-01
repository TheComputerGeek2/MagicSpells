package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class CrippleJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int strength;
	private int duration;
	private int portalCooldown;

	private boolean useSlownessEffect;
	private boolean applyPortalCooldown;

	public CrippleJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
		portalCooldown = getConfigInt("portal-cooldown-ticks", 100);

		useSlownessEffect = getConfigBoolean("use-slowness-effect", true);
		applyPortalCooldown = getConfigBoolean("apply-portal-cooldown", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			cripple(livingEntity, target.getTarget(), power);
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		cripple(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		cripple(null, target, power);
		return true;
	}
	
	private void cripple(LivingEntity caster, LivingEntity target, float power) {
		if (target == null) return;

		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);
		
		if (useSlownessEffect) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration * power), strength), true);
		if (applyPortalCooldown && target.getPortalCooldown() < (int) (portalCooldown * power)) target.setPortalCooldown((int) (portalCooldown * power));
	}

}
