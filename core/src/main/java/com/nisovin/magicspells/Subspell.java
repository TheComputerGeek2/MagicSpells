package com.nisovin.magicspells;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastResult;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.FunctionData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class Subspell {

	private static final Random random = ThreadLocalRandom.current();

	private Spell spell;
	private final String spellName;
	private CastMode mode = CastMode.PARTIAL;
	private CastTargeting targeting = CastTargeting.NORMAL;

	private boolean invert = false;
	private boolean passPower = true;
	private ConfigData<Integer> delay = data -> -1;
	private ConfigData<Double> chance = data -> -1D;
	private ConfigData<Float> subPower = data -> 1F;
	private ConfigData<String[]> args = data -> null;
	private ConfigData<Boolean> passTargeting = data -> null;

	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;

	public Subspell(@NotNull String subspell) {
		String[] split = subspell.split("\\(", 2);

		spellName = split[0].trim();

		if (split.length > 1) {
			split[1] = split[1].trim();
			if (split[1].endsWith(")")) split[1] = split[1].substring(0, split[1].length() - 1);

			String[] castArguments = split[1].split(";");
			for (String castArgument : castArguments) {
				String[] keyValue = castArgument.split("=", 2);
				if (keyValue.length != 2) {
					MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + subspell + "'.");
					continue;
				}

				String key = keyValue[0].toLowerCase().trim(), value = keyValue[1].trim();
				switch (key) {
					case "mode" -> mode = CastMode.getFromString(value);
					case "targeting" -> {
						try {
							targeting = CastTargeting.valueOf(value.toUpperCase());
						} catch (IllegalArgumentException e) {
							MagicSpells.error("Invalid target type '" + value + "' on subspell '" + subspell + "'.");
							DebugHandler.debugIllegalArgumentException(e);
						}
					}
					case "invert" -> invert = Boolean.parseBoolean(value);
					case "pass-power" -> passPower = Boolean.parseBoolean(value);
					case "pass-targeting" -> {
						ConfigData<String> supplier = ConfigDataUtil.getString(value);

						if (supplier.isConstant()) {
							String val = supplier.get();
							if (val == null) {
								passTargeting = data -> null;
								continue;
							}

							Boolean b = switch (val.toLowerCase()) {
								case "true" -> true;
								case "false" -> false;
								default -> null;
							};

							passTargeting = data -> b;
							continue;
						}

						passTargeting = data -> {
							String val = supplier.get(data);
							if (val == null) return null;

							return switch (val.toLowerCase()) {
								case "true" -> true;
								case "false" -> false;
								default -> null;
							};
						};
					}
					case "args" -> {
						try {
							JsonElement element = JsonParser.parseString(value);
							JsonArray array = element.getAsJsonArray();

							List<ConfigData<String>> argumentData = new ArrayList<>();
							List<String> arguments = new ArrayList<>();

							boolean constant = true;
							for (JsonElement je : array) {
								String val = je.getAsString();
								ConfigData<String> supplier = ConfigDataUtil.getString(val);

								argumentData.add(supplier);
								arguments.add(val);

								constant &= supplier.isConstant();
							}

							if (constant) {
								String[] arg = arguments.toArray(new String[0]);
								args = data -> arg;

								continue;
							}

							args = data -> {
								String[] ret = new String[argumentData.size()];
								for (int i = 0; i < argumentData.size(); i++)
									ret[i] = argumentData.get(i).get(data);

								return ret;
							};
						} catch (Exception e) {
							MagicSpells.error("Invalid spell arguments '" + value + "' on subspell '" + subspell + "'.");
							DebugHandler.debug(e);
						}
					}
					case "power" -> {
						try {
							float subPower = Float.parseFloat(value);
							this.subPower = data -> subPower;
						} catch (NumberFormatException e) {
							FunctionData<Float> subPowerData = FunctionData.build(value, Double::floatValue, true);
							if (subPowerData == null) {
								MagicSpells.error("Invalid power '" + value + "' on subspell '" + subspell + "'.");
								continue;
							}

							subPower = subPowerData;
						}
					}
					case "delay" -> {
						try {
							int delay = Integer.parseInt(value);
							this.delay = data -> delay;
						} catch (NumberFormatException e) {
							FunctionData<Integer> delayData = FunctionData.build(value, Double::intValue, true);
							if (delayData == null) {
								MagicSpells.error("Invalid delay '" + value + "' on subspell '" + subspell + "'.");
								continue;
							}

							delay = delayData;
						}
					}
					case "chance" -> {
						try {
							double chance = Double.parseDouble(value) / 100;
							this.chance = data -> chance;
						} catch (NumberFormatException e) {
							FunctionData<Double> chanceData = FunctionData.build(value, Function.identity(), true);
							if (chanceData == null) {
								MagicSpells.error("Invalid chance '" + value + "' on subspell '" + subspell + "'.");
								continue;
							}

							chance = data -> chanceData.get(data) / 100;
						}
					}
					default ->
						MagicSpells.error("Invalid cast argument '" + castArgument + "' on subspell '" + subspell + "'.");
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

			switch (targeting) {
				case NONE -> {
					isTargetedEntity = false;
					isTargetedLocation = false;
					isTargetedEntityFromLocation = false;
				}
				case ENTITY -> {
					if (!isTargetedEntity) return false;

					isTargetedLocation = false;
					isTargetedEntityFromLocation = false;
				}
				case LOCATION -> {
					if (!isTargetedLocation) return false;

					isTargetedEntity = false;
					isTargetedEntityFromLocation = false;
				}
				case ENTITY_FROM_LOCATION -> {
					if (!isTargetedEntityFromLocation) return false;

					isTargetedEntity = false;
					isTargetedLocation = false;
				}
			}
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

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data) {
		return subcast(data, false, true, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting) {
		return subcast(data, passTargeting, true, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting, boolean useTargetForLocation) {
		return subcast(data, passTargeting, useTargetForLocation, CastTargeting.DEFAULT_ORDERING);
	}

	@NotNull
	public SpellCastResult subcast(@NotNull SpellData data, boolean passTargeting, boolean useTargetForLocation, @NotNull CastTargeting @NotNull [] ordering) {
		if (invert) data = data.invert();

		boolean hasCaster = data.caster() != null;
		boolean hasTarget = data.target() != null;
		boolean hasLocation = data.location() != null;

		if (!hasCaster && (mode == CastMode.FULL || mode == CastMode.HARD)) return fail(data);

		CastTargeting targeting = this.targeting;
		if (targeting == CastTargeting.NORMAL) {
			targeting = null;

			for (CastTargeting ct : ordering) {
				boolean valid = switch (ct) {
					case NORMAL -> false;
					case ENTITY_FROM_LOCATION -> isTargetedEntityFromLocation && hasTarget && hasLocation;
					case ENTITY -> isTargetedEntity && hasTarget;
					case LOCATION -> isTargetedLocation && (hasLocation || hasTarget && useTargetForLocation);
					case NONE -> hasCaster;
				};

				if (valid) {
					targeting = ct;
					break;
				}
			}

			if (targeting == null) return fail(data);
		}

		return switch (targeting) {
			case ENTITY_FROM_LOCATION -> {
				if (!hasLocation || !hasTarget) yield wrapResult(spell.noTarget(data));
				yield castAtEntityFromLocation(data, passTargeting);
			}
			case ENTITY -> {
				if (!hasTarget) yield wrapResult(spell.noTarget(data));
				yield castAtEntity(data, passTargeting);
			}
			case LOCATION -> {
				if (hasTarget && useTargetForLocation) {
					data = data.location(data.target().getLocation());
					yield castAtLocation(data);
				}

				if (!hasLocation) yield wrapResult(spell.noTarget(data));
				yield castAtLocation(data);
			}
			case NONE -> {
				if (!hasCaster) yield fail(data);
				yield cast(data);
			}
			default -> fail(data);
		};
	}

	@Deprecated
	public PostCastAction cast(LivingEntity caster, float power) {
		SpellCastResult result = cast(new SpellData(caster, power, null));
		return result.state == SpellCastState.NORMAL ? result.action : PostCastAction.ALREADY_HANDLED;
	}

	@NotNull
	private SpellCastResult cast(@NotNull SpellData data) {
		data = data.builder().recipient(null).power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		double chance = this.chance.get(data);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return fail(data);

		int delay = this.delay.get(data);
		if (delay < 0) return castReal(data.noTargeting());

		SpellData finalData = data.noTargeting();
		MagicSpells.scheduleDelayedTask(() -> castReal(finalData), delay, data.location());

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD, FULL -> spell.hardCast(data);
			case DIRECT -> wrapResult(spell.cast(data));
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				if (!castEvent.callEvent()) yield postCast(castEvent, null, true);

				CastResult result = spell.cast(castEvent.getSpellCastState(), castEvent.getSpellData());
				yield postCast(castEvent, result, true);
			}
		};
	}

	@Deprecated
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(new SpellData(caster, target, power, null), false).success();
	}

	@Deprecated
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, boolean passTargeting) {
		return castAtEntity(new SpellData(caster, target, power, null), passTargeting).success();
	}

	@NotNull
	private SpellCastResult castAtEntity(@NotNull SpellData data, boolean passTargeting) {
		if (!isTargetedEntity) {
			if (isTargetedLocation) return castAtLocation(data);
			return fail(data);
		}

		data = data.builder().recipient(null).power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		if (mode != CastMode.HARD && !this.passTargeting.getOr(data, passTargeting)) {
			ValidTargetList canTarget = spell.getValidTargetList();
			if (!canTarget.canTarget(data.caster(), data.target())) return wrapResult(spell.noTarget(data));
		}

		double chance = this.chance.get(data);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return fail(data);

		int delay = this.delay.get(data);
		if (delay < 0) return castAtEntityReal(data.noLocation());

		SpellData finalData = data.noLocation();
		MagicSpells.scheduleDelayedTask(() -> castAtEntityReal(finalData), delay, data.target());

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtEntityReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				yield wrapResult(targetedSpell.castAtEntity(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, true);

				data = castEvent.getSpellData();

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
				if (!targetEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEvent), true);

				data = targetEvent.getSpellData();

				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				CastResult result = targetedSpell.castAtEntity(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, false);

				data = castEvent.getSpellData();

				SpellTargetEvent targetEvent = new SpellTargetEvent(spell, data);
				if (!targetEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEvent), false);

				data = targetEvent.getSpellData();

				TargetedEntitySpell targetedSpell = (TargetedEntitySpell) spell;
				CastResult result = targetedSpell.castAtEntity(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@Deprecated
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(new SpellData(caster, target, power, null)).success();
	}

	@NotNull
	private SpellCastResult castAtLocation(@NotNull SpellData data) {
		if (!isTargetedLocation) return fail(data);

		data = data.builder().recipient(null).power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		double chance = this.chance.get(data);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return fail(data);

		int delay = this.delay.get(data);
		if (delay < 0) return castAtLocationReal(data.noTarget());

		SpellData finalData = data.noTarget();
		MagicSpells.scheduleDelayedTask(() -> castAtLocationReal(finalData), delay, data.location());

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtLocationReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				yield wrapResult(targetedSpell.castAtLocation(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, true);

				data = castEvent.getSpellData();

				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEvent), true);

				data = targetEvent.getSpellData();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				CastResult result = targetedSpell.castAtLocation(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, false);

				data = castEvent.getSpellData();

				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEvent), false);

				data = targetEvent.getSpellData();

				TargetedLocationSpell targetedSpell = (TargetedLocationSpell) spell;
				CastResult result = targetedSpell.castAtLocation(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@Deprecated
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(new SpellData(caster, target, from, power, null), false).success();
	}

	@Deprecated
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, boolean passTargeting) {
		return castAtEntityFromLocation(new SpellData(caster, target, from, power, null), passTargeting).success();
	}

	@NotNull
	private SpellCastResult castAtEntityFromLocation(@NotNull SpellData data, boolean passTargeting) {
		if (!isTargetedEntityFromLocation) return fail(data);

		data = data.builder().recipient(null).power((passPower ? data.power() : 1) * subPower.get(data)).args(args.get(data)).build();

		if (mode != CastMode.HARD && !this.passTargeting.getOr(data, passTargeting)) {
			ValidTargetList canTarget = spell.getValidTargetList();
			if (!canTarget.canTarget(data.caster(), data.target())) return wrapResult(spell.noTarget(data));
		}

		double chance = this.chance.get(data);
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return fail(data);

		int delay = this.delay.get(data);
		if (delay < 0) return castAtEntityFromLocationReal(data);

		SpellData finalData = data;
		MagicSpells.scheduleDelayedTask(() -> castAtEntityFromLocationReal(finalData), delay, data.target());

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	@NotNull
	private SpellCastResult castAtEntityFromLocationReal(@NotNull SpellData data) {
		return switch (mode) {
			case HARD -> spell.hardCast(data);
			case DIRECT -> {
				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				yield wrapResult(targetedSpell.castAtEntityFromLocation(data));
			}
			case PARTIAL -> {
				SpellCastEvent castEvent = new SpellCastEvent(spell, SpellCastState.NORMAL, data, 0, null, 0);
				castEvent.callEvent();

				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, true);

				data = castEvent.getSpellData();

				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
				if (!targetEntityEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEntityEvent), true);

				data = targetEntityEvent.getSpellData();

				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetLocationEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetLocationEvent), true);

				data = targetLocationEvent.getSpellData();

				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				CastResult result = targetedSpell.castAtEntityFromLocation(data);

				yield postCast(castEvent, result, true);
			}
			case FULL -> {
				SpellCastEvent castEvent = spell.preCast(data);
				if (castEvent.getSpellCastState() != SpellCastState.NORMAL)
					yield postCast(castEvent, null, false);

				data = castEvent.getSpellData();

				SpellTargetEvent targetEntityEvent = new SpellTargetEvent(spell, data);
				if (!targetEntityEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetEntityEvent), false);

				data = targetEntityEvent.getSpellData();

				SpellTargetLocationEvent targetLocationEvent = new SpellTargetLocationEvent(spell, data);
				if (!targetLocationEvent.callEvent())
					yield postCast(castEvent, spell.noTarget(targetLocationEvent), false);

				data = targetLocationEvent.getSpellData();

				TargetedEntityFromLocationSpell targetedSpell = (TargetedEntityFromLocationSpell) spell;
				CastResult result = targetedSpell.castAtEntityFromLocation(data);

				yield postCast(castEvent, result, false);
			}
		};
	}

	@NotNull
	private SpellCastResult wrapResult(@NotNull CastResult result) {
		return new SpellCastResult(SpellCastState.NORMAL, result.action(), result.data());
	}

	@NotNull
	private SpellCastResult fail(@NotNull SpellData data) {
		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.ALREADY_HANDLED, data);
	}

	@NotNull
	private SpellCastResult postCast(@NotNull SpellCastEvent castEvent, @Nullable CastResult result, boolean partial) {
		PostCastAction action = result == null ? PostCastAction.HANDLE_NORMALLY : result.action();
		SpellCastState state = castEvent.getSpellCastState();
		SpellData data = result == null ? castEvent.getSpellData() : result.data();

		if (partial) {
			new SpellCastedEvent(spell, state, action, data, 0, null).callEvent();
			return new SpellCastResult(state, action, data);
		}

		spell.postCast(castEvent, action, data);
		return new SpellCastResult(state, action, data);
	}

	public enum CastMode {

		HARD("hard", "h"),
		FULL("full", "f"),
		PARTIAL("partial", "p"),
		DIRECT("direct", "d");

		private static final Map<String, CastMode> nameMap = new HashMap<>();

		private final String[] names;

		CastMode(String... names) {
			this.names = names;
		}

		public static CastMode getFromString(String label) {
			return nameMap.get(label.toLowerCase());
		}

		static {
			for (CastMode mode : CastMode.values()) {
				nameMap.put(mode.name().toLowerCase(), mode);
				for (String s : mode.names) {
					nameMap.put(s.toLowerCase(), mode);
				}
			}
		}

	}

	public enum CastTargeting {

		NORMAL,
		ENTITY_FROM_LOCATION,
		ENTITY,
		LOCATION,
		NONE;

		private static final CastTargeting[] DEFAULT_ORDERING = {ENTITY_FROM_LOCATION, ENTITY, LOCATION, NONE};

	}

}
