package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BoundingBox;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class PortalJutsu extends InstantJutsu {

	private String firstMarkJutsuName;
	private String secondMarkJutsuName;

	private MarkJutsu firstMark;
	private MarkJutsu secondMark;

	private JutsuReagents teleportCost;

	private int duration;
	private int minDistanceSq;
	private int maxDistanceSq;
	private int effectInterval;
	private int teleportCooldown;

	private float vertRadius;
	private float horizRadius;

	private boolean allowReturn;
	private boolean tpOtherPlayers;
	private boolean usingSecondMarkJutsu;
	private boolean chargeCostToTeleporter;

	private String strNoMark;
	private String strTooFar;
	private String strTooClose;
	private String strTeleportCostFail;
	private String strTeleportCooldownFail;

	public PortalJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		firstMarkJutsuName = getConfigString("mark-jutsu", "");
		secondMarkJutsuName = getConfigString("second-mark-jutsu", "");

		teleportCost = getConfigReagents("teleport-cost");

		duration = getConfigInt("duration", 400);
		minDistanceSq = getConfigInt("min-distance", 10);
		maxDistanceSq = getConfigInt("max-distance", 0);
		effectInterval = getConfigInt("effect-interval", 10);
		teleportCooldown = getConfigInt("teleport-cooldown", 5) * 1000;

		horizRadius = getConfigFloat("horiz-radius", 1F);
		vertRadius = getConfigFloat("vert-radius", 1F);

		allowReturn = getConfigBoolean("allow-return", true);
		tpOtherPlayers = getConfigBoolean("teleport-other-players", true);
		chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);

		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");

		minDistanceSq *= minDistanceSq;
		maxDistanceSq *= maxDistanceSq;
	}

	@Override
	public void initialize() {
		super.initialize();

		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(firstMarkJutsuName);
		if (jutsu != null && jutsu instanceof MarkJutsu) firstMark = (MarkJutsu) jutsu;
		else MagicJutsus.error("PortalJutsu '" + internalName + "' has an invalid mark-jutsu defined!");

		usingSecondMarkJutsu = false;
		if (!secondMarkJutsuName.isEmpty()) {
			jutsu = MagicJutsus.getJutsuByInternalName(secondMarkJutsuName);
			if (jutsu != null && jutsu instanceof MarkJutsu) {
				secondMark = (MarkJutsu) jutsu;
				usingSecondMarkJutsu = true;
			} else MagicJutsus.error("PortalJutsu '" + internalName + "' has an invalid second-mark-jutsu defined!");
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = firstMark.getEffectiveMark(livingEntity);
			Location locSecond;
			if (loc == null) {
				sendMessage(strNoMark, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (usingSecondMarkJutsu) {
				locSecond = secondMark.getEffectiveMark(livingEntity);
				if (locSecond == null) {
					sendMessage(strNoMark, livingEntity, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else locSecond = livingEntity.getLocation();

			double distanceSq = 0;
			if (maxDistanceSq > 0) {
				if (!loc.getWorld().equals(locSecond.getWorld())) {
					sendMessage(strTooFar, livingEntity, args);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq > maxDistanceSq) {
						sendMessage(strTooFar, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			if (minDistanceSq > 0) {
				if (loc.getWorld().equals(locSecond.getWorld())) {
					if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq < minDistanceSq) {
						sendMessage(strTooClose, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			new PortalLink(this, livingEntity, loc, locSecond);
			playJutsuEffects(EffectPosition.CASTER, livingEntity);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private class PortalLink implements Listener {

		private PortalJutsu jutsu;
		private LivingEntity caster;
		private Location loc1;
		private Location loc2;
		private BoundingBox box1;
		private BoundingBox box2;
		private int taskId1 = -1;
		private int taskId2 = -1;
		private Map<String, Long> cooldownUntil;

		private PortalLink (PortalJutsu jutsu, LivingEntity caster, Location loc1, Location loc2) {
			this.jutsu = jutsu;
			this.caster = caster;
			this.loc1 = loc1;
			this.loc2 = loc2;

			box1 = new BoundingBox(loc1, jutsu.horizRadius, jutsu.vertRadius);
			box2 = new BoundingBox(loc2, jutsu.horizRadius, jutsu.vertRadius);
			cooldownUntil = new HashMap<>();

			cooldownUntil.put(caster.getName(), System.currentTimeMillis() + jutsu.teleportCooldown);
			registerEvents(this);
			startTasks();
		}

		private void startTasks() {
			if (jutsu.effectInterval > 0) {
				taskId1 = MagicJutsus.scheduleRepeatingTask(() -> {
					if (caster.isValid()) {
						playJutsuEffects(EffectPosition.SPECIAL, loc1);
						playJutsuEffects(EffectPosition.SPECIAL, loc2);
					} else disable();

				}, jutsu.effectInterval, jutsu.effectInterval);
			}
			taskId2 = MagicJutsus.scheduleDelayedTask(this::disable, jutsu.duration);
		}

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			if (!tpOtherPlayers && !event.getPlayer().equals(caster)) return;
			if (!caster.isValid()) {
				disable();
				return;
			}
			Player player = event.getPlayer();
			if (box1.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc2.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playJutsuEffects(EffectPosition.TARGET, player);
				}
			} else if (jutsu.allowReturn && box2.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc1.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playJutsuEffects(EffectPosition.TARGET, player);
				}
			}
		}

		private boolean checkTeleport(LivingEntity livingEntity) {
			if (cooldownUntil.containsKey(livingEntity.getName()) && cooldownUntil.get(livingEntity.getName()) > System.currentTimeMillis()) {
				sendMessage(strTeleportCooldownFail, livingEntity, MagicJutsus.NULL_ARGS);
				return false;
			}
			cooldownUntil.put(livingEntity.getName(), System.currentTimeMillis() + teleportCooldown);

			LivingEntity payer = null;
			if (jutsu.teleportCost != null) {
				if (jutsu.chargeCostToTeleporter) {
					if (hasReagents(livingEntity, jutsu.teleportCost)) {
						payer = livingEntity;
					} else {
						sendMessage(jutsu.strTeleportCostFail, livingEntity, MagicJutsus.NULL_ARGS);
						return false;
					}
				} else {
					if (hasReagents(caster, jutsu.teleportCost)) {
						payer = caster;
					} else {
						sendMessage(jutsu.strTeleportCostFail, livingEntity, MagicJutsus.NULL_ARGS);
						return false;
					}
				}
				if (payer == null) return false;
			}

			JutsuTargetEvent event = new JutsuTargetEvent(jutsu, caster, livingEntity, 1);
			Bukkit.getPluginManager().callEvent(event);
			if (payer != null) removeReagents(payer, jutsu.teleportCost);
			return true;
		}

		private void disable() {
			unregisterEvents(this);
			playJutsuEffects(EffectPosition.DELAYED, loc1);
			playJutsuEffects(EffectPosition.DELAYED, loc2);
			if (taskId1 > 0) MagicJutsus.cancelTask(taskId1);
			if (taskId2 > 0) MagicJutsus.cancelTask(taskId2);
			caster = null;
			jutsu = null;
			loc1 = null;
			loc2 = null;
			box1 = null;
			box2 = null;
			cooldownUntil.clear();
			cooldownUntil = null;
		}

	}

}
