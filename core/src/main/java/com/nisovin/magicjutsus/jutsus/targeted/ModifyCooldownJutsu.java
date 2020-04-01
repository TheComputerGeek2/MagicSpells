package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class ModifyCooldownJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private List<Jutsu> jutsus;
	private List<String> jutsuNames;
	
	private float seconds;
	private float multiplier;
	
	public ModifyCooldownJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		jutsuNames = getConfigStringList("jutsus", null);

		seconds = getConfigFloat("seconds", 1F);
		multiplier = getConfigFloat("multiplier", 0F);
	}
	
	@Override
	public void initialize() {
		jutsus = new ArrayList<>();

		if (jutsuNames == null) {
			MagicJutsus.error("ModifyCooldownJutsu '" + internalName + "' has no jutsus defined!");
			return;
		}

		for (String jutsuName : jutsuNames) {
			Jutsu jutsu = MagicJutsus.getJutsuByInternalName(jutsuName);
			if (jutsu == null) {
				MagicJutsus.error("ModifyCooldownJutsu '" + internalName + "' has an invalid jutsu defined '" + jutsuName + '\'');
				continue;
			}
			jutsus.add(jutsu);
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			modifyCooldowns(target.getTarget(), target.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		modifyCooldowns(target, power);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		modifyCooldowns(target, power);
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}

	private void modifyCooldowns(LivingEntity target, float power) {
		float sec = seconds * power;
		float mult = multiplier * (1F / power);

		for (Jutsu jutsu : jutsus) {
			float cd = jutsu.getCooldown(target);
			if (cd <= 0) continue;

			cd -= sec;
			if (mult > 0) cd *= mult;
			if (cd < 0) cd = 0;
			jutsu.setCooldown(target, cd, false);
		}
	}

}
