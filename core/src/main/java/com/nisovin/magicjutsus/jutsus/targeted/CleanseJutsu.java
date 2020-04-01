package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

// TODO setup a system for registering "CleanseProvider"s
public class CleanseJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private ValidTargetChecker checker;

	private List<String> toCleanse;
	private List<DotJutsu> dotJutsus;
	private List<StunJutsu> stunJutsus;
	private List<BuffJutsu> buffJutsus;
	private List<SilenceJutsu> silenceJutsus;
	private List<LevitateJutsu> levitateJutsus;
	private List<PotionEffectType> potionEffectTypes;

	private boolean fire;
	
	public CleanseJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		toCleanse = getConfigStringList("remove", Arrays.asList("fire", "hunger", "poison", "wither"));
		dotJutsus = new ArrayList<>();
		stunJutsus = new ArrayList<>();
		buffJutsus = new ArrayList<>();
		silenceJutsus = new ArrayList<>();
		levitateJutsus = new ArrayList<>();
		potionEffectTypes = new ArrayList<>();
		fire = false;
	}

	@Override
	public void initialize() {
		super.initialize();

		for (String s : toCleanse) {
			if (s.equalsIgnoreCase("fire")) {
				fire = true;
				continue;
			}

			if (s.startsWith("buff:")) {
				if (s.replace("buff:", "").equalsIgnoreCase("*")) {
					for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
						if (jutsu instanceof BuffJutsu) buffJutsus.add((BuffJutsu) jutsu);
					}
					continue;
				}
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s.replace("buff:", ""));
				if (jutsu instanceof BuffJutsu) buffJutsus.add((BuffJutsu) jutsu);
				continue;
			}

			if (s.startsWith("dot:")) {
				if (s.replace("dot:", "").equalsIgnoreCase("*")) {
					for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
						if (jutsu instanceof DotJutsu) dotJutsus.add((DotJutsu) jutsu);
					}
					continue;
				}
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s.replace("dot:", ""));
				if (jutsu instanceof DotJutsu) dotJutsus.add((DotJutsu) jutsu);
				continue;
			}

			if (s.startsWith("stun:")) {
				if (s.replace("stun:", "").equalsIgnoreCase("*")) {
					for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
						if (jutsu instanceof StunJutsu) stunJutsus.add((StunJutsu) jutsu);
					}
					continue;
				}
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s.replace("stun:", ""));
				if (jutsu instanceof StunJutsu) stunJutsus.add((StunJutsu) jutsu);
				continue;
			}

			if (s.startsWith("silence:")) {
				if (s.replace("silence:", "").equalsIgnoreCase("*")) {
					for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
						if (jutsu instanceof SilenceJutsu) silenceJutsus.add((SilenceJutsu) jutsu);
					}
					continue;
				}
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s.replace("silence:", ""));
				if (jutsu instanceof SilenceJutsu) silenceJutsus.add((SilenceJutsu) jutsu);
				continue;
			}

			if (s.startsWith("levitate:")) {
				if (s.replace("levitate:", "").equalsIgnoreCase("*")) {
					for (Jutsu jutsu : MagicJutsus.getJutsusOrdered()) {
						if (jutsu instanceof LevitateJutsu) levitateJutsus.add((LevitateJutsu) jutsu);
					}
					continue;
				}
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(s.replace("levitate:", ""));
				if (jutsu instanceof LevitateJutsu) levitateJutsus.add((LevitateJutsu) jutsu);
				continue;
			}

			PotionEffectType type = Util.getPotionEffectType(s);
			if (type != null) potionEffectTypes.add(type);
		}

		checker = entity -> {
			if (fire && entity.getFireTicks() > 0) return true;

			for (PotionEffectType type : potionEffectTypes) {
				if (entity.hasPotionEffect(type)) return true;
			}

			for (BuffJutsu jutsu : buffJutsus) {
				if (jutsu.isActive(entity)) return true;
			}

			for (DotJutsu jutsu : dotJutsus) {
				if (jutsu.isActive(entity)) return true;
			}

			for (StunJutsu jutsu : stunJutsus) {
				if (jutsu.isStunned(entity)) return true;
			}

			for (SilenceJutsu jutsu : silenceJutsus) {
				if (jutsu.isSilenced(entity)) return true;
			}

			for (LevitateJutsu jutsu : levitateJutsus) {
				if (jutsu.isBeingLevitated(entity)) return true;
			}

			return false;
		};
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, checker);
			if (target == null) return noTarget(livingEntity);
			
			cleanse(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		cleanse(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		cleanse(null, target);
		return true;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}
	
	private void cleanse(LivingEntity caster, LivingEntity target) {
		if (fire) target.setFireTicks(0);

		for (PotionEffectType type : potionEffectTypes) {
			target.addPotionEffect(new PotionEffect(type, 0, 0, true), true);
			target.removePotionEffect(type);
		}

		buffJutsus.forEach(jutsu -> jutsu.turnOff(target));
		dotJutsus.forEach(jutsu -> jutsu.cancelDot(target));
		stunJutsus.forEach(jutsu -> jutsu.removeStun(target));
		silenceJutsus.forEach(jutsu -> jutsu.removeSilence(target));
		levitateJutsus.forEach(jutsu -> jutsu.removeLevitate(target));

		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);
	}

}
