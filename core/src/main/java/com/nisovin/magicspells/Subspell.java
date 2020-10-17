package com.nisovin.magicspells;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.CastUtil.CastMode;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class Subspell {

	private static final Random random = new Random();
	
	private Spell spell;
	private String spellName;
	private CastMode mode = CastMode.PARTIAL;

	private int delay = 0;
	private float subPower = 1F;
	private double chance = -1D;
	
	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;

	// spellName(mode=hard|h|full|f|partial|p|direct|d;power=[subpower];delay=[delay];chance=[chance])
	public Subspell(String data) {
		String[] split = data.split("\\(", 2);
		
		spellName = split[0].trim();
		
		if (split.length > 1) {
			split[1] = split[1].trim();
			if (split[1].endsWith(")")) split[1] = split[1].substring(0, split[1].length() - 1);
			String[] args = Util.splitParams(split[1]);

			for (String arg : args) {
				if (!arg.contains("=")) continue;

				String[] castArguments = arg.split(";");
				for (String castArgument : castArguments) {
					String[] keyValue = castArgument.split("=");
					switch(keyValue[0].toLowerCase()) {
						case "mode":
							mode = Util.getCastMode(keyValue[1]);
							break;
						case "power":
							try {
								subPower = Float.parseFloat(keyValue[1]);
							} catch (NumberFormatException e) {
								DebugHandler.debugNumberFormat(e);
							}
							break;
						case "delay":
							try {
								delay = Integer.parseInt(keyValue[1]);
							} catch (NumberFormatException e) {
								DebugHandler.debugNumberFormat(e);
							}
							break;
						case "chance":
							try {
								chance = Double.parseDouble(keyValue[1]) / 100D;
							} catch (NumberFormatException e) {
								DebugHandler.debugNumberFormat(e);
							}
							break;
					}
				}

			}
		}
	}
	
	public boolean process() {
		spell = MagicSpells.getSpellByInternalName(spellName);
		if (spell != null) {
			isTargetedEntity = spell instanceof TargetedEntitySpell;
			isTargetedLocation = spell instanceof TargetedLocationSpell;
			isTargetedEntityFromLocation = spell instanceof TargetedEntityFromLocationSpell;
		}
		return spell != null;
	}
	
	public Spell getSpell() {
		return spell;
	}
	
	public boolean isTargetedEntitySpell() {
		return isTargetedEntity;
	}
	
	public boolean isTargetedLocationSpell() {
		return isTargetedLocation;
	}
	
	public boolean isTargetedEntityFromLocationSpell() {
		return isTargetedEntityFromLocation;
	}
	
	public PostCastAction cast(final LivingEntity livingEntity, final float power) {
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return PostCastAction.ALREADY_HANDLED;
		if (delay <= 0) return castReal(livingEntity, power);
		MagicSpells.scheduleDelayedTask(() -> castReal(livingEntity, power), delay);
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private PostCastAction castReal(LivingEntity livingEntity, float power) {
		if ((mode == CastMode.HARD || mode == CastMode.FULL) && livingEntity != null) {
			return spell.cast(livingEntity, power * subPower, null).action;
		}
		
		if (mode == CastMode.PARTIAL) {
			SpellCastEvent event = new SpellCastEvent(spell, livingEntity, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
			EventUtil.call(event);
			if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				PostCastAction act = spell.castSpell(livingEntity, SpellCastState.NORMAL, event.getPower(), null);
				EventUtil.call(new SpellCastedEvent(spell, livingEntity, SpellCastState.NORMAL, event.getPower(), null, 0, null, act));
				return act;
			}
			return PostCastAction.ALREADY_HANDLED;
		}
		
		return spell.castSpell(livingEntity, SpellCastState.NORMAL, power * subPower, null);
	}
	
	public boolean castAtEntity(final LivingEntity livingEntity, final LivingEntity target, final float power) {
		if (delay <= 0) return castAtEntityReal(livingEntity, target, power);
		MagicSpells.scheduleDelayedTask(() -> castAtEntityReal(livingEntity, target, power), delay);
		return true;
	}
	
	private boolean castAtEntityReal(LivingEntity livingEntity, LivingEntity target, float power) {
		boolean ret = false;

		if (!isTargetedEntity) {
			if (isTargetedLocation) castAtLocationReal(livingEntity, target.getLocation(), power);
			return ret;
		}

		if (mode == CastMode.HARD && livingEntity != null) {
			SpellCastResult result = spell.cast(livingEntity, power, null);
			return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			SpellCastEvent spellCast = spell.preCast(livingEntity, power * subPower, null);
			SpellTargetEvent spellTarget = new SpellTargetEvent(spell, livingEntity, target, power);
			EventUtil.call(spellTarget);
			if (!spellTarget.isCancelled() && spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
				success = ((TargetedEntitySpell) spell).castAtEntity(livingEntity, target, spellCast.getPower());
				spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			SpellCastEvent event = new SpellCastEvent(spell, livingEntity, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
			SpellTargetEvent spellTarget = new SpellTargetEvent(spell, livingEntity, target, power);
			EventUtil.call(spellTarget);
			EventUtil.call(event);
			if (!spellTarget.isCancelled() && !event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedEntitySpell) spell).castAtEntity(livingEntity, target, event.getPower());
				else ret = ((TargetedEntitySpell) spell).castAtEntity(target, event.getPower());
				if (ret) EventUtil.call(new SpellCastedEvent(spell, livingEntity, SpellCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedEntitySpell) spell).castAtEntity(livingEntity, target, power * subPower);
			else ret = ((TargetedEntitySpell) spell).castAtEntity(target, power * subPower);
		}

		return ret;
	}
	
	public boolean castAtLocation(final LivingEntity livingEntity, final Location target, final float power) {
		if (delay <= 0) return castAtLocationReal(livingEntity, target, power);
		MagicSpells.scheduleDelayedTask(() -> castAtLocationReal(livingEntity, target, power), delay);
		return true;
	}
	
	private boolean castAtLocationReal(LivingEntity livingEntity, Location target, float power) {
		boolean ret = false;

		if (!isTargetedLocation) return ret;

		if (mode == CastMode.HARD && livingEntity != null) {
			SpellCastResult result = spell.cast(livingEntity, power, null);
			return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			SpellCastEvent spellCast = spell.preCast(livingEntity, power * subPower, null);
			SpellTargetLocationEvent spellLocation = new SpellTargetLocationEvent(spell, livingEntity, target, power);
			EventUtil.call(spellLocation);
			if (!spellLocation.isCancelled() && spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
				success = ((TargetedLocationSpell) spell).castAtLocation(livingEntity, target, spellCast.getPower());
				spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			SpellCastEvent event = new SpellCastEvent(spell, livingEntity, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
			SpellTargetLocationEvent spellLocation = new SpellTargetLocationEvent(spell, livingEntity, target, power);
			EventUtil.call(spellLocation);
			EventUtil.call(event);
			if (!spellLocation.isCancelled() && !event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedLocationSpell) spell).castAtLocation(livingEntity, target, event.getPower());
				else ret = ((TargetedLocationSpell) spell).castAtLocation(target, event.getPower());
				if (ret) EventUtil.call(new SpellCastedEvent(spell, livingEntity, SpellCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedLocationSpell) spell).castAtLocation(livingEntity, target, power * subPower);
			else ret = ((TargetedLocationSpell) spell).castAtLocation(target, power * subPower);
		}

		return ret;
	}
	
	public boolean castAtEntityFromLocation(final LivingEntity livingEntity, final Location from, final LivingEntity target, final float power) {
		if (delay <= 0) return castAtEntityFromLocationReal(livingEntity, from, target, power);
		MagicSpells.scheduleDelayedTask(() -> castAtEntityFromLocationReal(livingEntity, from, target, power), delay);
		return true;
	}
	
	private boolean castAtEntityFromLocationReal(LivingEntity livingEntity, Location from, LivingEntity target, float power) {
		boolean ret = false;

		if (!isTargetedEntityFromLocation) return ret;

		if (mode == CastMode.HARD && livingEntity != null) {
			SpellCastResult result = spell.cast(livingEntity, power, MagicSpells.NULL_ARGS);
			return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			SpellCastEvent spellCast = spell.preCast(livingEntity, power * subPower, MagicSpells.NULL_ARGS);
			SpellTargetEvent spellTarget = new SpellTargetEvent(spell, livingEntity, target, power);
			SpellTargetLocationEvent spellLocation = new SpellTargetLocationEvent(spell, livingEntity, from, power);
			EventUtil.call(spellLocation);
			EventUtil.call(spellTarget);
			if (!spellLocation.isCancelled() && !spellTarget.isCancelled() && spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
				success = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(livingEntity, from, target, spellCast.getPower());
				spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			SpellCastEvent event = new SpellCastEvent(spell, livingEntity, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
			SpellTargetEvent spellTarget = new SpellTargetEvent(spell, livingEntity, target, power);
			SpellTargetLocationEvent spellLocation = new SpellTargetLocationEvent(spell, livingEntity, from, power);
			EventUtil.call(spellLocation);
			EventUtil.call(spellTarget);
			EventUtil.call(event);
			if (!spellLocation.isCancelled() && !spellTarget.isCancelled() && !event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(livingEntity, from, target, event.getPower());
				else ret = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(from, target, event.getPower());
				if (ret) EventUtil.call(new SpellCastedEvent(spell, livingEntity, SpellCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(livingEntity, from, target, power * subPower);
			else ret = ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(from, target, power * subPower);
		}

		return ret;
	}
	
}
