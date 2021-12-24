package com.nisovin.magicspells;

import java.util.List;
import java.util.Random;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.CastUtil.CastMode;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class Subspell {

	private static final Random random = ThreadLocalRandom.current();

	private Spell spell;
	private String spellName;
	private CastMode mode = CastMode.PARTIAL;

	private int delay = -1;
	private float subPower = 1F;
	private double chance = -1D;
	private String[] args = null;

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

			String[] castArguments = split[1].split(";");
			for (String castArgument : castArguments) {
				String[] keyValue = castArgument.split("=");
				if (keyValue.length != 2) {
					MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + data + "'.");
					continue;
				}

				switch (keyValue[0].toLowerCase().trim()) {
					case "mode" -> mode = Util.getCastMode(keyValue[1].trim());
					case "args" -> {
						try {
							JsonElement element = JsonParser.parseString(keyValue[1].trim());
							JsonArray array = element.getAsJsonArray();

							List<String> arguments = new ArrayList<>();
							array.forEach(e -> arguments.add(e.getAsString()));

							args = arguments.toArray(new String[0]);
						} catch (IllegalArgumentException e) {
							MagicSpells.error("Invalid spell arguments '" + keyValue[1] + "' on subspell '" + data + "'.");
							DebugHandler.debugIllegalArgumentException(e);
						} catch (ClassCastException | JsonSyntaxException e) {
							MagicSpells.error("Invalid spell arguments '" + keyValue[1] + "' on subspell '" + data + "'.");
							DebugHandler.debug(e);
						}
					}
					case "power" -> {
						try {
							subPower = Float.parseFloat(keyValue[1].trim());
						} catch (NumberFormatException e) {
							MagicSpells.error("Invalid power '" + keyValue[1] + "' on subspell '" + data + "'.");
							DebugHandler.debugNumberFormat(e);
						}
					}
					case "delay" -> {
						try {
							delay = Integer.parseInt(keyValue[1].trim());
						} catch (NumberFormatException e) {
							MagicSpells.error("Invalid delay '" + keyValue[1] + "' on subspell '" + data + "'.");
							DebugHandler.debugNumberFormat(e);
						}
					}
					case "chance" -> {
						try {
							chance = Double.parseDouble(keyValue[1].trim()) / 100D;
						} catch (NumberFormatException e) {
							MagicSpells.error("Invalid chance '" + keyValue[1] + "' on subspell '" + data + "'.");
							DebugHandler.debugNumberFormat(e);
						}
					}
					default -> MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + data + "'.");
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

	public PostCastAction cast(final LivingEntity caster, final float power) {
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return PostCastAction.ALREADY_HANDLED;
		if (delay < 0) return castReal(caster, power);
		MagicSpells.scheduleDelayedTask(() -> castReal(caster, power), delay);
		return PostCastAction.HANDLE_NORMALLY;
	}

	private PostCastAction castReal(LivingEntity caster, float basePower) {
		float power = basePower * subPower;

		return switch (mode) {
			case HARD, FULL -> spell.cast(caster, power, args).action;
			case DIRECT -> spell.castSpell(caster, SpellCastState.NORMAL, power, args);
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, caster, SpellCastState.NORMAL, power * subPower, args, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield PostCastAction.ALREADY_HANDLED;

				power = castEvent.getPower();

				PostCastAction action = spell.castSpell(caster, SpellCastState.NORMAL, power, args);
				new SpellCastedEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null, action);

				yield action;
			}
		};
	}

	public boolean castAtEntity(final LivingEntity caster, final LivingEntity target, final float power) {
		return castAtEntity(caster, target, power, true);
	}

	public boolean castAtEntity(final LivingEntity caster, final LivingEntity target, final float power, final boolean passTargeting) {
		if (delay < 0) return castAtEntityReal(caster, target, power, passTargeting);
		MagicSpells.scheduleDelayedTask(() -> castAtEntityReal(caster, target, power, passTargeting), delay);
		return true;
	}

	private boolean castAtEntityReal(LivingEntity caster, LivingEntity target, float basePower, boolean passTargeting) {
		if (!isTargetedEntity) return isTargetedLocation && castAtLocationReal(caster, target.getLocation(), basePower);

		return switch (mode) {
			case HARD -> {
				SpellCastResult result = spell.cast(caster, basePower * subPower, args);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY || result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				float power = basePower * subPower;

				if (passTargeting) yield passTargetingEntity(caster, target, power, args);
				else {
					TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
					yield caster != null ? targetedSpell.castAtEntity(caster, target, power, args) : targetedSpell.castAtEntity(target, power, args);
				}
			}
			case PARTIAL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = new SpellCastEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, caster, target, castEvent.getPower(), args);
				if (!targetEvent.callEvent()) yield false;

				target = targetEvent.getTarget();
				power = targetEvent.getPower();

				boolean success;
				if (passTargeting) success = passTargetingEntity(caster, target, power, args);
				else {
					TargetedEntitySpell targetedEntitySpell = (TargetedEntitySpell) spell;
					success = caster != null ? targetedEntitySpell.castAtEntity(caster, target, power, args) :
						targetedEntitySpell.castAtEntity(target, power, args);
				}

				if (success)
					new SpellCastedEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = spell.preCast(caster, power, args);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					SpellTargetEvent targetEvent = new SpellTargetEvent(spell, caster, target, castEvent.getPower(), args);

					target = targetEvent.getTarget();
					power = targetEvent.getPower();

					if (targetEvent.callEvent()) {
						if (passTargeting) success = passTargetingEntity(caster, target, power, args);
						else {
							TargetedEntitySpell targetedEntitySpell = (TargetedEntitySpell) spell;
							success = caster != null ? targetedEntitySpell.castAtEntity(caster, target, power, args) :
								targetedEntitySpell.castAtEntity(target, power, args);
						}
					}
				}

				if (success) {
					if (spell instanceof TargetedSpell targetedSpell) {
						action = PostCastAction.NO_MESSAGES;
						targetedSpell.sendMessages(caster, target, args);
					}
				} else action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean passTargetingEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		ValidTargetList list = spell.getValidTargetList();
		ValidTargetList originalList = list.clone();
		if (Objects.equals(caster, target) && !list.canTargetSelf()) list.setTargetCaster(true);
		if (!list.canTargetEntity(target)) {
			list.addEntityTarget(target);
			spell.setValidTargetList(list);
		}

		boolean success = caster != null ? ((TargetedEntitySpell) spell).castAtEntity(caster, target, power, args) : ((TargetedEntitySpell) spell).castAtEntity(target, power, args);
		spell.setValidTargetList(originalList);
		return success;
	}

	public boolean castAtLocation(final LivingEntity caster, final Location target, final float power) {
		if (delay < 0) return castAtLocationReal(caster, target, power);
		MagicSpells.scheduleDelayedTask(() -> castAtLocationReal(caster, target, power), delay);
		return true;
	}

	private boolean castAtLocationReal(LivingEntity caster, Location target, float basePower) {
		if (!isTargetedLocation) return false;

		return switch (mode) {
			case HARD -> {
				SpellCastResult result = spell.cast(caster, basePower * subPower, args);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY ||
					result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				yield caster != null ? targetedSpell.castAtLocation(caster, target, basePower * subPower, args)
					: targetedSpell.castAtLocation(target, basePower * subPower, args);
			}
			case PARTIAL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = new SpellCastEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, caster, target, castEvent.getPower(), args);
				if (!targetEvent.callEvent()) yield false;

				target = targetEvent.getTargetLocation();
				power = targetEvent.getPower();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				boolean success = caster != null ? targetedSpell.castAtLocation(caster, target, power, args) :
					targetedSpell.castAtLocation(target, power, args);

				if (success)
					new SpellCastedEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = spell.preCast(caster, power, args);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, caster, target, castEvent.getPower(), args);

					target = targetEvent.getTargetLocation();
					power = targetEvent.getPower();

					if (targetEvent.callEvent()) {
						TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
						success = caster != null ? targetedSpell.castAtLocation(caster, target, power, args) :
							targetedSpell.castAtLocation(target, power, args);
					}
				}

				if (!success) action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean castAtEntityFromLocation(final LivingEntity caster, final Location from, final LivingEntity target, final float power) {
		return castAtEntityFromLocation(caster, from, target, power, true);
	}

	public boolean castAtEntityFromLocation(final LivingEntity caster, final Location from, final LivingEntity target, final float power, final boolean passTargeting) {
		if (delay < 0) return castAtEntityFromLocationReal(caster, from, target, power, passTargeting);
		MagicSpells.scheduleDelayedTask(() -> castAtEntityFromLocationReal(caster, from, target, power, passTargeting), delay);
		return true;
	}

	private boolean castAtEntityFromLocationReal(LivingEntity caster, Location from, LivingEntity target, float basePower, boolean passTargeting) {
		if (!isTargetedEntityFromLocation) return false;

		return switch (mode) {
			case HARD -> {
				SpellCastResult result = spell.cast(caster, basePower * subPower, args);
				yield result.state == SpellCastState.NORMAL && (result.action == PostCastAction.HANDLE_NORMALLY ||
					result.action == PostCastAction.NO_MESSAGES);
			}
			case DIRECT -> {
				if (passTargeting)
					yield passTargetingEntityFromLocation(caster, from, target, basePower * subPower, args);
				else {
					TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
					yield caster != null ? targetedSpell.castAtEntityFromLocation(caster, from, target, basePower * subPower, args) :
						targetedSpell.castAtEntityFromLocation(from, target, basePower * subPower, args);
				}
			}
			case PARTIAL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = new SpellCastEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null, 0);
				if (!castEvent.callEvent() || castEvent.getSpellCastState() != SpellCastState.NORMAL) yield false;

				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, caster, target, castEvent.getPower(), args);
				if (!targetEntityEvent.callEvent()) yield false;

				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, caster, from, targetEntityEvent.getPower(), args);
				if (!targetLocationEvent.callEvent()) yield false;

				target = targetEntityEvent.getTarget();
				power = targetLocationEvent.getPower();
				from = targetLocationEvent.getTargetLocation();

				boolean success;
				if (passTargeting) success = passTargetingEntityFromLocation(caster, from, target, power, args);
				else {
					TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
					success = caster != null ? targetedSpell.castAtEntityFromLocation(caster, from, target, power, args)
						: targetedSpell.castAtEntityFromLocation(from, target, power, args);
				}

				if (success)
					new SpellCastedEvent(spell, caster, SpellCastState.NORMAL, power, args, 0, null,
						PostCastAction.HANDLE_NORMALLY).callEvent();

				yield success;
			}
			case FULL -> {
				float power = basePower * subPower;

				SpellCastEvent castEvent = spell.preCast(caster, power, args);
				if (castEvent == null) yield false;

				PostCastAction action = PostCastAction.HANDLE_NORMALLY;
				boolean success = false;
				if (castEvent.getSpellCastState() == SpellCastState.NORMAL) {
					SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, caster, target, castEvent.getPower(), args);
					targetEntityEvent.callEvent();

					target = targetEntityEvent.getTarget();
					power = targetEntityEvent.getPower();

					if (!targetEntityEvent.isCancelled()) {
						SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, caster, from, power, args);
						targetLocationEvent.callEvent();

						power = targetLocationEvent.getPower();
						from = targetLocationEvent.getTargetLocation();

						if (!targetLocationEvent.isCancelled()) {
							if (passTargeting)
								success = passTargetingEntityFromLocation(caster, from, target, power, args);
							else {
								TargetedEntityFromLocationSpell targetedEntitySpell = (TargetedEntityFromLocationSpell) spell;
								success = caster != null ? targetedEntitySpell.castAtEntityFromLocation(caster, from, target, power, args) :
									targetedEntitySpell.castAtEntityFromLocation(from, target, power, args);
							}
						}
					}
				}

				if (success) {
					if (spell instanceof TargetedSpell targetedSpell) {
						action = PostCastAction.NO_MESSAGES;
						targetedSpell.sendMessages(caster, target, args);
					}
				} else action = PostCastAction.ALREADY_HANDLED;

				spell.postCast(castEvent, action);

				yield success;
			}
		};
	}

	public boolean passTargetingEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		ValidTargetList list = spell.getValidTargetList();
		ValidTargetList originalList = list.clone();
		if (Objects.equals(caster, target) && !list.canTargetSelf()) list.setTargetCaster(true);
		if (!list.canTargetEntity(target)) {
			list.addEntityTarget(target);
			spell.setValidTargetList(list);
		}

		boolean success = caster != null ? ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(caster, from, target, power, args) : ((TargetedEntityFromLocationSpell) spell).castAtEntityFromLocation(from, target, power, args);
		spell.setValidTargetList(originalList);
		return success;
	}

}
