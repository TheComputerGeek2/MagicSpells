package com.nisovin.magicspells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.Util;

public class Subspell {

	private static Random random = new Random();
	
	private String spellName;
	private Spell spell;
	private CastMode mode = CastMode.PARTIAL;
	private float subPower = 1F;
	private int delay = 0;
	private double chance = -1;
	
	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;
	
	// spell-name (mode=hard|full|partial|direct,power=[subpower],delay=[delay],chance=[chance])|(hard)|(full)|(partial)|(direct)
	public Subspell(String data) {
		String[] split = data.split("\\(", 2);
		
		spellName = split[0].trim();
		
		if (split.length > 1) {
			split[1] = split[1].trim();
			if (split[1].endsWith(")")) split[1] = split[1].substring(0, split[1].length() - 1);
			String[] args = Util.splitParams(split[1]);
			for (String arg : args) {
				if (arg.contains("=")) {
					String[] keyval = arg.split("=");
					if (keyval[0].equalsIgnoreCase("mode")) {
						if (keyval[1].equalsIgnoreCase("hard")) {
							mode = CastMode.HARD;
						} else if (keyval[1].equalsIgnoreCase("full")) {
							mode = CastMode.FULL;
						} else if (keyval[1].equalsIgnoreCase("partial")) {
							mode = CastMode.PARTIAL;
						} else if (keyval[1].equalsIgnoreCase("direct")) {
							mode = CastMode.DIRECT;
						}
					} else if (keyval[0].equalsIgnoreCase("power")) {
						try {
							subPower = Float.parseFloat(keyval[1]);
						} catch (NumberFormatException e) {
							DebugHandler.debugNumberFormat(e);
						}
					} else if (keyval[0].equalsIgnoreCase("delay")) {
						try {
							delay = Integer.parseInt(keyval[1]);
						} catch (NumberFormatException e) {
							DebugHandler.debugNumberFormat(e);
						}
					} else if (keyval[0].equalsIgnoreCase("chance")) {
						try {
							chance = Double.parseDouble(keyval[1]) / 100;
						} catch (NumberFormatException e) {
							DebugHandler.debugNumberFormat(e);
						}
					}
				} else if (arg.equalsIgnoreCase("hard")) {
					mode = CastMode.HARD;
				} else if (arg.equalsIgnoreCase("full")) {
					mode = CastMode.FULL;
				} else if (arg.equalsIgnoreCase("partial")) {
					mode = CastMode.PARTIAL;
				} else if (arg.equalsIgnoreCase("direct")) {
					mode = CastMode.DIRECT;
				} else if (RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, arg)) {  //TODO include this part in the comments
					delay = Integer.parseInt(arg);
				} else if (arg.matches("^[0-9]+\\.[0-9]+$")) { //TODO include this part in the comments
					subPower = Float.parseFloat(arg);
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
	
	public PostCastAction cast(final Player player, final float power) {
		if (chance > 0 && chance < 1) {
			if (random.nextDouble() > chance) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (delay <= 0) {
			return castReal(player, power);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					castReal(player, power);
				}
			}, delay);
			return PostCastAction.HANDLE_NORMALLY;
		}
	}
	
	PostCastAction castReal(Player player, float power) {
		if ((mode == CastMode.HARD || mode == CastMode.FULL) && player != null) {
			return spell.cast(player, power * subPower, null).action;
		} else if (mode == CastMode.PARTIAL) {
			SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				PostCastAction act = spell.castSpell(player, SpellCastState.NORMAL, event.getPower(), null);
				Bukkit.getPluginManager().callEvent(new SpellCastedEvent(spell, player, SpellCastState.NORMAL, event.getPower(), null, 0, null, act));
				return act;
			}
			return PostCastAction.ALREADY_HANDLED;
		} else {
			return spell.castSpell(player, SpellCastState.NORMAL, power * subPower, null);
		}
	}
	
	public boolean castAtEntity(final Player player, final LivingEntity target, final float power) {
		if (delay <= 0) {
			return castAtEntityReal(player, target, power);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					castAtEntityReal(player, target, power);
				}
			}, delay);
			return true;
		}
	}
	
	boolean castAtEntityReal(Player player, LivingEntity target, float power) {
		boolean ret = false;
		if (isTargetedEntity) {
			if (mode == CastMode.HARD && player != null) {
				SpellCastResult result = spell.cast(player, power, null); // the null is just no args
				return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
			} else if (mode == CastMode.FULL && player != null) {
				boolean success = false;
				SpellCastEvent spellCast = spell.preCast(player, power * subPower, null); // the null is just no args
				if (spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
					success = ((TargetedEntitySpell)spell).castAtEntity(player, target, spellCast.getPower());
					spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
				}
				return success;
			} else if (mode == CastMode.PARTIAL) {
				SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power * subPower /*power*/, null /*args*/, 0, null /*reagents*/, 0);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
					if (player != null) {
						ret = ((TargetedEntitySpell)spell).castAtEntity(player, target, event.getPower());
					} else {
						ret = ((TargetedEntitySpell)spell).castAtEntity(target, event.getPower());
					}
					if (ret) {
						Bukkit.getPluginManager().callEvent(new SpellCastedEvent(spell, player, SpellCastState.NORMAL, event.getPower(), null /*args*/, 0, null /*reagents*/, PostCastAction.HANDLE_NORMALLY));
					}
				}
			} else {
				if (player != null) {
					ret = ((TargetedEntitySpell)spell).castAtEntity(player, target, power * subPower);
				} else {
					ret = ((TargetedEntitySpell)spell).castAtEntity(target, power * subPower);
				}
			}
		} else if (isTargetedLocation) {
			castAtLocationReal(player, target.getLocation(), power);
		}
		return ret;
	}
	
	public boolean castAtLocation(final Player player, final Location target, final float power) {
		if (delay <= 0) {
			return castAtLocationReal(player, target, power);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					castAtLocationReal(player, target, power);
				}
			}, delay);
			return true;
		}
	}
	
	boolean castAtLocationReal(Player player, Location target, float power) {
		boolean ret = false;
		if (isTargetedLocation) {
			if (mode == CastMode.HARD && player != null) {
				SpellCastResult result = spell.cast(player, power, null);
				return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
			} else if (mode == CastMode.FULL && player != null) {
				boolean success = false;
				SpellCastEvent spellCast = spell.preCast(player, power * subPower, null);
				if (spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
					success = ((TargetedLocationSpell)spell).castAtLocation(player, target, spellCast.getPower());
					spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
				}
				return success;
			} else if (mode == CastMode.PARTIAL) {
				SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
					if (player != null) {
						ret = ((TargetedLocationSpell)spell).castAtLocation(player, target, event.getPower());
					} else {
						ret = ((TargetedLocationSpell)spell).castAtLocation(target, event.getPower());
					}
					if (ret) {
						Bukkit.getPluginManager().callEvent(new SpellCastedEvent(spell, player, SpellCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
					}
				}
			} else {
				if (player != null) {
					ret = ((TargetedLocationSpell)spell).castAtLocation(player, target, power * subPower);
				} else {
					ret = ((TargetedLocationSpell)spell).castAtLocation(target, power * subPower);
				}
			}
		}
		return ret;
	}
	
	public boolean castAtEntityFromLocation(final Player player, final Location from, final LivingEntity target, final float power) {
		if (delay <= 0) {
			return castAtEntityFromLocationReal(player, from, target, power);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				@Override
				public void run() {
					castAtEntityFromLocationReal(player, from, target, power);
				}
			}, delay);
			return true;
		}
	}
	
	boolean castAtEntityFromLocationReal(Player player, Location from, LivingEntity target, float power) {
		boolean ret = false;
		if (isTargetedEntityFromLocation) {
			if (mode == CastMode.HARD && player != null) {
				SpellCastResult result = spell.cast(player, power, null);
				return result.state == SpellCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
			} else if (mode == CastMode.FULL && player != null) {
				boolean success = false;
				SpellCastEvent spellCast = spell.preCast(player, power * subPower, null);
				if (spellCast != null && spellCast.getSpellCastState() == SpellCastState.NORMAL) {
					success = ((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(player, from, target, spellCast.getPower());
					spell.postCast(spellCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
				}
				return success;
			} else if (mode == CastMode.PARTIAL) {
				SpellCastEvent event = new SpellCastEvent(spell, player, SpellCastState.NORMAL, power * subPower, null, 0, null, 0);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
					if (player != null) {
						ret = ((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(player, from, target, event.getPower());
					} else {
						ret = ((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(from, target, event.getPower());
					}
					if (ret) {
						Bukkit.getPluginManager().callEvent(new SpellCastedEvent(spell, player, SpellCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
					}
				}
			} else {
				if (player != null) {
					ret = ((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(player, from, target, power * subPower);
				} else {
					ret = ((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(from, target, power * subPower);
				}
			}
		}
		return ret;
	}
	
	public enum CastMode {
		HARD, FULL, PARTIAL, DIRECT
	}
}
