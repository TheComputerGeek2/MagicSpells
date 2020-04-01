package com.nisovin.magicjutsus;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.Jutsu.PostCastAction;
import com.nisovin.magicjutsus.Jutsu.JutsuCastResult;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.util.CastUtil.CastMode;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class Ninjutsu {

	private static Random random = new Random();
	
	private Jutsu jutsu;
	private String jutsuName;
	private CastMode mode = CastMode.PARTIAL;

	private int delay = 0;
	private float subPower = 1F;
	private double chance = -1D;
	
	private boolean isTargetedEntity = false;
	private boolean isTargetedLocation = false;
	private boolean isTargetedEntityFromLocation = false;

	// jutsuName(mode=hard|h|full|f|partial|p|direct|d;power=[subpower];delay=[delay];chance=[chance])
	public Ninjutsu(String data) {
		String[] split = data.split("\\(", 2);
		
		jutsuName = split[0].trim();
		
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
		jutsu = MagicJutsus.getJutsuByInternalName(jutsuName);
		if (jutsu != null) {
			isTargetedEntity = jutsu instanceof TargetedEntityJutsu;
			isTargetedLocation = jutsu instanceof TargetedLocationJutsu;
			isTargetedEntityFromLocation = jutsu instanceof TargetedEntityFromLocationJutsu;
		}
		return jutsu != null;
	}
	
	public Jutsu getJutsu() {
		return jutsu;
	}
	
	public boolean isTargetedEntityJutsu() {
		return isTargetedEntity;
	}
	
	public boolean isTargetedLocationJutsu() {
		return isTargetedLocation;
	}
	
	public boolean isTargetedEntityFromLocationJutsu() {
		return isTargetedEntityFromLocation;
	}
	
	public PostCastAction cast(final LivingEntity livingEntity, final float power) {
		if ((chance > 0 && chance < 1) && random.nextDouble() > chance) return PostCastAction.ALREADY_HANDLED;
		if (delay <= 0) return castReal(livingEntity, power);
		MagicJutsus.scheduleDelayedTask(() -> castReal(livingEntity, power), delay);
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private PostCastAction castReal(LivingEntity livingEntity, float power) {
		if ((mode == CastMode.HARD || mode == CastMode.FULL) && livingEntity != null) {
			return jutsu.cast(livingEntity, power * subPower, null).action;
		}
		
		if (mode == CastMode.PARTIAL) {
			JutsuCastEvent event = new JutsuCastEvent(jutsu, livingEntity, JutsuCastState.NORMAL, power * subPower, null, 0, null, 0);
			EventUtil.call(event);
			if (!event.isCancelled() && event.getJutsuCastState() == JutsuCastState.NORMAL) {
				PostCastAction act = jutsu.castJutsu(livingEntity, JutsuCastState.NORMAL, event.getPower(), null);
				EventUtil.call(new JutsuCastedEvent(jutsu, livingEntity, JutsuCastState.NORMAL, event.getPower(), null, 0, null, act));
				return act;
			}
			return PostCastAction.ALREADY_HANDLED;
		}
		
		return jutsu.castJutsu(livingEntity, JutsuCastState.NORMAL, power * subPower, null);
	}
	
	public boolean castAtEntity(final LivingEntity livingEntity, final LivingEntity target, final float power) {
		if (delay <= 0) return castAtEntityReal(livingEntity, target, power);
		MagicJutsus.scheduleDelayedTask(() -> castAtEntityReal(livingEntity, target, power), delay);
		return true;
	}
	
	private boolean castAtEntityReal(LivingEntity livingEntity, LivingEntity target, float power) {
		boolean ret = false;

		if (!isTargetedEntity) {
			if (isTargetedLocation) castAtLocationReal(livingEntity, target.getLocation(), power);
			return ret;
		}

		if (mode == CastMode.HARD && livingEntity != null) {
			JutsuCastResult result = jutsu.cast(livingEntity, power, null);
			return result.state == JutsuCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			JutsuCastEvent jutsuCast = jutsu.preCast(livingEntity, power * subPower, null);
			JutsuTargetEvent jutsuTarget = new JutsuTargetEvent(jutsu, livingEntity, target, power);
			EventUtil.call(jutsuTarget);
			if (!jutsuTarget.isCancelled() && jutsuCast != null && jutsuCast.getJutsuCastState() == JutsuCastState.NORMAL) {
				success = ((TargetedEntityJutsu) jutsu).castAtEntity(livingEntity, target, jutsuCast.getPower());
				jutsu.postCast(jutsuCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			JutsuCastEvent event = new JutsuCastEvent(jutsu, livingEntity, JutsuCastState.NORMAL, power * subPower, null, 0, null, 0);
			JutsuTargetEvent jutsuTarget = new JutsuTargetEvent(jutsu, livingEntity, target, power);
			EventUtil.call(jutsuTarget);
			EventUtil.call(event);
			if (!jutsuTarget.isCancelled() && !event.isCancelled() && event.getJutsuCastState() == JutsuCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedEntityJutsu) jutsu).castAtEntity(livingEntity, target, event.getPower());
				else ret = ((TargetedEntityJutsu) jutsu).castAtEntity(target, event.getPower());
				if (ret) EventUtil.call(new JutsuCastedEvent(jutsu, livingEntity, JutsuCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedEntityJutsu) jutsu).castAtEntity(livingEntity, target, power * subPower);
			else ret = ((TargetedEntityJutsu) jutsu).castAtEntity(target, power * subPower);
		}

		return ret;
	}
	
	public boolean castAtLocation(final LivingEntity livingEntity, final Location target, final float power) {
		if (delay <= 0) return castAtLocationReal(livingEntity, target, power);
		MagicJutsus.scheduleDelayedTask(() -> castAtLocationReal(livingEntity, target, power), delay);
		return true;
	}
	
	private boolean castAtLocationReal(LivingEntity livingEntity, Location target, float power) {
		boolean ret = false;

		if (!isTargetedLocation) return ret;

		if (mode == CastMode.HARD && livingEntity != null) {
			JutsuCastResult result = jutsu.cast(livingEntity, power, null);
			return result.state == JutsuCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			JutsuCastEvent jutsuCast = jutsu.preCast(livingEntity, power * subPower, null);
			JutsuTargetLocationEvent jutsuLocation = new JutsuTargetLocationEvent(jutsu, livingEntity, target, power);
			EventUtil.call(jutsuLocation);
			if (!jutsuLocation.isCancelled() && jutsuCast != null && jutsuCast.getJutsuCastState() == JutsuCastState.NORMAL) {
				success = ((TargetedLocationJutsu) jutsu).castAtLocation(livingEntity, target, jutsuCast.getPower());
				jutsu.postCast(jutsuCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			JutsuCastEvent event = new JutsuCastEvent(jutsu, livingEntity, JutsuCastState.NORMAL, power * subPower, null, 0, null, 0);
			JutsuTargetLocationEvent jutsuLocation = new JutsuTargetLocationEvent(jutsu, livingEntity, target, power);
			EventUtil.call(jutsuLocation);
			EventUtil.call(event);
			if (!jutsuLocation.isCancelled() && !event.isCancelled() && event.getJutsuCastState() == JutsuCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedLocationJutsu) jutsu).castAtLocation(livingEntity, target, event.getPower());
				else ret = ((TargetedLocationJutsu) jutsu).castAtLocation(target, event.getPower());
				if (ret) EventUtil.call(new JutsuCastedEvent(jutsu, livingEntity, JutsuCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedLocationJutsu) jutsu).castAtLocation(livingEntity, target, power * subPower);
			else ret = ((TargetedLocationJutsu) jutsu).castAtLocation(target, power * subPower);
		}

		return ret;
	}
	
	public boolean castAtEntityFromLocation(final LivingEntity livingEntity, final Location from, final LivingEntity target, final float power) {
		if (delay <= 0) return castAtEntityFromLocationReal(livingEntity, from, target, power);
		MagicJutsus.scheduleDelayedTask(() -> castAtEntityFromLocationReal(livingEntity, from, target, power), delay);
		return true;
	}
	
	private boolean castAtEntityFromLocationReal(LivingEntity livingEntity, Location from, LivingEntity target, float power) {
		boolean ret = false;

		if (!isTargetedEntityFromLocation) return ret;

		if (mode == CastMode.HARD && livingEntity != null) {
			JutsuCastResult result = jutsu.cast(livingEntity, power, MagicJutsus.NULL_ARGS);
			return result.state == JutsuCastState.NORMAL && result.action == PostCastAction.HANDLE_NORMALLY;
		}

		if (mode == CastMode.FULL && livingEntity != null) {
			boolean success = false;
			JutsuCastEvent jutsuCast = jutsu.preCast(livingEntity, power * subPower, MagicJutsus.NULL_ARGS);
			JutsuTargetEvent jutsuTarget = new JutsuTargetEvent(jutsu, livingEntity, target, power);
			JutsuTargetLocationEvent jutsuLocation = new JutsuTargetLocationEvent(jutsu, livingEntity, from, power);
			EventUtil.call(jutsuLocation);
			EventUtil.call(jutsuTarget);
			if (!jutsuLocation.isCancelled() && !jutsuTarget.isCancelled() && jutsuCast != null && jutsuCast.getJutsuCastState() == JutsuCastState.NORMAL) {
				success = ((TargetedEntityFromLocationJutsu) jutsu).castAtEntityFromLocation(livingEntity, from, target, jutsuCast.getPower());
				jutsu.postCast(jutsuCast, success ? PostCastAction.HANDLE_NORMALLY : PostCastAction.ALREADY_HANDLED);
			}
			return success;
		}

		if (mode == CastMode.PARTIAL) {
			JutsuCastEvent event = new JutsuCastEvent(jutsu, livingEntity, JutsuCastState.NORMAL, power * subPower, null, 0, null, 0);
			JutsuTargetEvent jutsuTarget = new JutsuTargetEvent(jutsu, livingEntity, target, power);
			JutsuTargetLocationEvent jutsuLocation = new JutsuTargetLocationEvent(jutsu, livingEntity, from, power);
			EventUtil.call(jutsuLocation);
			EventUtil.call(jutsuTarget);
			EventUtil.call(event);
			if (!jutsuLocation.isCancelled() && !jutsuTarget.isCancelled() && !event.isCancelled() && event.getJutsuCastState() == JutsuCastState.NORMAL) {
				if (livingEntity != null) ret = ((TargetedEntityFromLocationJutsu) jutsu).castAtEntityFromLocation(livingEntity, from, target, event.getPower());
				else ret = ((TargetedEntityFromLocationJutsu) jutsu).castAtEntityFromLocation(from, target, event.getPower());
				if (ret) EventUtil.call(new JutsuCastedEvent(jutsu, livingEntity, JutsuCastState.NORMAL, event.getPower(), null, 0, null, PostCastAction.HANDLE_NORMALLY));
			}
		} else {
			if (livingEntity != null) ret = ((TargetedEntityFromLocationJutsu) jutsu).castAtEntityFromLocation(livingEntity, from, target, power * subPower);
			else ret = ((TargetedEntityFromLocationJutsu) jutsu).castAtEntityFromLocation(from, target, power * subPower);
		}

		return ret;
	}
	
}
