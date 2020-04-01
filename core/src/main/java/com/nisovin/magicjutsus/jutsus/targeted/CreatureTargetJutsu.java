package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class CreatureTargetJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private String targetJutsuName;
	private Ninjutsu targetJutsu;

	public CreatureTargetJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		targetJutsuName = getConfigString("jutsu", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		targetJutsu = new Ninjutsu(targetJutsuName);
		if (!targetJutsu.process()) {
			targetJutsu = null;
			if (!targetJutsuName.isEmpty()) MagicJutsus.error("CreatureTargetJutsu '" + internalName + "' has an invalid jutsu defined!");
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			castJutsus(livingEntity, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		castJutsus(caster, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		castJutsus(caster, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		playJutsuEffects(from, target);
		return true;
	}

	private void castJutsus(LivingEntity livingEntity, float power) {
		if (!(livingEntity instanceof Creature)) return;
		Creature caster = (Creature) livingEntity;
		LivingEntity target = caster.getTarget();
		if (target == null || !target.isValid()) return;

		playJutsuEffects(caster, target);
		if (targetJutsu == null) return;
		if (targetJutsu.isTargetedEntityFromLocationJutsu()) targetJutsu.castAtEntityFromLocation(caster, caster.getLocation(), target, power);
		else if (targetJutsu.isTargetedLocationJutsu()) targetJutsu.castAtLocation(caster, target.getLocation(), power);
		else if (targetJutsu.isTargetedEntityJutsu()) targetJutsu.castAtEntity(caster, target, power);
	}

}
