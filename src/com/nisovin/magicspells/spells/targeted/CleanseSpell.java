package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CleanseSpell extends TargetedSpell implements TargetedEntitySpell {

	boolean targetPlayers;
	boolean targetNonPlayers;
	
	List<PotionEffectType> potionEffectTypes;

	List<BuffSpell> buffSpells;
	List<StunSpell> stunSpells;
	List<DotSpell> dotSpells;
	List<LevitateSpell> levitateSpells;

	List<String> toCleanse;
	boolean fire;
	
	private ValidTargetChecker checker;
	
	public CleanseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", false);

		buffSpells = new ArrayList<>();
		stunSpells = new ArrayList<>();
		dotSpells = new ArrayList<>();
		levitateSpells = new ArrayList<>();

		fire = false;
		potionEffectTypes = new ArrayList<>();
		toCleanse = getConfigStringList("remove", Arrays.asList("fire", "17", "19", "20"));

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
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("buff:", ""));
				if (spell instanceof BuffSpell) {
					buffSpells.add((BuffSpell)spell);
				}
				continue;
			}

			if (s.startsWith("stun:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("stun:", ""));
				if (spell instanceof StunSpell) {
					stunSpells.add((StunSpell)spell);
				}
				continue;
			}

			if (s.startsWith("dot:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("dot:", ""));
				if (spell instanceof DotSpell) {
					dotSpells.add((DotSpell)spell);
				}
				continue;
			}

			if (s.startsWith("levitate:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("levitate:", ""));
				if (spell instanceof LevitateSpell) {
					levitateSpells.add((LevitateSpell)spell);
				}
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

			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (buffSpells.stream().anyMatch(spell -> spell.isActive(player))) return true;
				if (stunSpells.stream().anyMatch(spell -> spell.isStunned(player))) return true;
				if (dotSpells.stream().anyMatch(spell -> spell.isActive(player))) return true;
				if (levitateSpells.stream().anyMatch(spell -> spell.isActive(player))) return true;
			}

			return false;
		};

	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power, checker);
			if (target == null) return noTarget(player);
			
			cleanse(player, target.getTarget());
			
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void cleanse(Player caster, LivingEntity target) {
		if (fire) target.setFireTicks(0);
		for (PotionEffectType type : potionEffectTypes) {
			target.addPotionEffect(new PotionEffect(type, 0, 0, true), true);
			target.removePotionEffect(type);
		}
		if (target instanceof Player) {
			Player player = (Player) target;
			buffSpells.forEach(buffSpell -> buffSpell.turnOff(player));
			stunSpells.forEach(stunSpell -> stunSpell.cancelStun(player));
			dotSpells.forEach(dotSpell -> dotSpell.cancelDot(player));
			levitateSpells.forEach(levitateSpell -> levitateSpell.cancelLevitation(player));
		}
		if (caster != null) {
			playSpellEffects(caster, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
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
	public boolean isBeneficialDefault() {
		return true;
	}
	
	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}

}
