package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.PortalEnterEvent;
import com.nisovin.magicspells.events.PortalLeaveEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PortalSpell extends InstantSpell {

	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> minDistance;
	private final ConfigData<Integer> maxDistance;
	private final ConfigData<Integer> effectInterval;

	private final ConfigData<Double> teleportCooldown;
	private final ConfigData<Double> startTeleportCooldown;

	private final ConfigData<Float> vRadiusStart;
	private final ConfigData<Float> hRadiusStart;
	private final ConfigData<Float> vRadiusEnd;
	private final ConfigData<Float> hRadiusEnd;

	private final ConfigData<Boolean> canReturn;
	private final ConfigData<Boolean> canTeleportOtherPlayers;
	private final ConfigData<Boolean> chargeReagentsToTeleporter;

	private final String strNoMark;
	private final String strTooFar;
	private final String strTooClose;
	private final String strTeleportNoCost;
	private final String strTeleportOnCooldown;

	private MarkSpell startMark;
	private MarkSpell endMark;

	private final String startMarkSpellName;
	private final String endMarkSpellName;

	private final SpellReagents teleportReagents;

	private boolean usingSecondMarkSpell;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 400);
		minDistance = getConfigDataInt("min-distance", 10);
		maxDistance = getConfigDataInt("max-distance", 0);
		effectInterval = getConfigDataInt("effect-interval", 10);

		teleportCooldown = getConfigDataDouble("teleport-cooldown", 5.0);
		startTeleportCooldown = getConfigDataDouble("start-teleport-cooldown", teleportCooldown);

		hRadiusStart = getConfigDataFloat("horiz-radius", 1F);
		vRadiusStart = getConfigDataFloat("vert-radius", 1F);
		hRadiusEnd = hRadiusStart;
		vRadiusEnd = vRadiusStart;

		canReturn = getConfigDataBoolean("allow-return", true);
		canTeleportOtherPlayers = getConfigDataBoolean("teleport-other-players", true);
		chargeReagentsToTeleporter = getConfigDataBoolean("charge-cost-to-teleporter", false);

		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTeleportNoCost = getConfigString("str-teleport-cost-fail", "");
		strTeleportOnCooldown = getConfigString("str-teleport-cooldown-fail", "");

		startMarkSpellName = getConfigString("mark-spell", "");
		endMarkSpellName = getConfigString("second-mark-spell", "");

		teleportReagents = getConfigReagents("teleport-cost");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(startMarkSpellName);
		if (spell instanceof MarkSpell) startMark = (MarkSpell) spell;
		else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid mark-spell defined: '" + startMarkSpellName + "'.");

		usingSecondMarkSpell = false;
		if (!endMarkSpellName.isEmpty()) {
			spell = MagicSpells.getSpellByInternalName(endMarkSpellName);
			if (spell instanceof MarkSpell) {
				endMark = (MarkSpell) spell;
				usingSecondMarkSpell = true;
			} else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid second-mark-spell defined: '" + endMarkSpellName + "'.");
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		Location loc = startMark.getEffectiveMark(data.caster());

		Location locSecond;
		if (loc == null) {
			sendMessage(strNoMark, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (usingSecondMarkSpell) {
			locSecond = endMark.getEffectiveMark(data.caster());
			if (locSecond == null) {
				sendMessage(strNoMark, data.caster(), data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}
		} else locSecond = data.caster().getLocation();

		double distanceSq = 0;

		float maxDistanceSq = maxDistance.get(data);
		maxDistanceSq *= maxDistanceSq;

		if (maxDistanceSq > 0) {
			if (!loc.getWorld().equals(locSecond.getWorld())) {
				sendMessage(strTooFar, data.caster(), data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			distanceSq = locSecond.distanceSquared(loc);
			if (distanceSq > maxDistanceSq) {
				sendMessage(strTooFar, data.caster(), data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}
		}

		float minDistanceSq = minDistance.get(data);
		minDistanceSq *= minDistanceSq;

		if (minDistanceSq > 0 && loc.getWorld().equals(locSecond.getWorld())) {
			if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
			if (distanceSq < minDistanceSq) {
				sendMessage(strTooClose, data.caster(), data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}
		}

		Portal startPortal = new Portal(loc, teleportReagents, new BoundingBox(loc, hRadiusStart.get(data), vRadiusStart.get(data)));
		Portal endPortal = new Portal(locSecond, teleportReagents, new BoundingBox(locSecond, hRadiusEnd.get(data), vRadiusEnd.get(data)));

		new PortalLink(data, startPortal, endPortal);

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public ConfigData<Integer> getEffectInterval() {
		return effectInterval;
	}

	public ConfigData<Integer> getDuration() {
		return duration;
	}

	public ConfigData<Integer> getMinDistance() {
		return minDistance;
	}

	public ConfigData<Integer> getMaxDistance() {
		return maxDistance;
	}

	public ConfigData<Double> getTeleportCooldown() {
		return teleportCooldown;
	}

	public ConfigData<Double> getStartTeleportCooldown() {
		return startTeleportCooldown;
	}

	public ConfigData<Float> getHRadiusStart() {
		return hRadiusStart;
	}

	public ConfigData<Float> getVRadiusStart() {
		return vRadiusStart;
	}

	public ConfigData<Float> getHRadiusEnd() {
		return hRadiusEnd;
	}

	public ConfigData<Float> getVRadiusEnd() {
		return vRadiusEnd;
	}

	public ConfigData<Boolean> canReturn() {
		return canReturn;
	}

	public ConfigData<Boolean> canTeleportOtherPlayers() {
		return canTeleportOtherPlayers;
	}

	public ConfigData<Boolean> shouldChargeReagentsToTeleporter() {
		return chargeReagentsToTeleporter;
	}

	public String getStrNoMark() {
		return strNoMark;
	}

	public String getStrTooFar() {
		return strTooFar;
	}

	public String getStrTooClose() {
		return strTooClose;
	}

	public String getStrTeleportNoCost() {
		return strTeleportNoCost;
	}

	public String getStrTeleportOnCooldown() {
		return strTeleportOnCooldown;
	}

	public MarkSpell getStartMark() {
		return startMark;
	}

	public MarkSpell getEndMark() {
		return endMark;
	}

	public SpellReagents getTeleportReagents() {
		return teleportReagents;
	}
	public boolean isUsingSecondMarkSpell() {
		return usingSecondMarkSpell;
	}

	private class PortalLink implements Listener {

		private Map<UUID, Long> tpCooldowns;

		private final double tpCooldown;

		private final boolean allowReturn;
		private final boolean teleportOtherPlayers;
		private final boolean chargeCostToTeleporter;

		private final SpellData data;

		private final Portal startPortal;
		private final Portal endPortal;

		private ScheduledTask taskPortal = null;
		private ScheduledTask taskStop = null;

		private PortalLink(SpellData data, Portal startPortal, Portal endPortal) {
			this.startPortal = startPortal;
			this.endPortal = endPortal;
			this.data = data;

			tpCooldown = teleportCooldown.get(data) * 1000;

			allowReturn = canReturn.get(data);
			teleportOtherPlayers = canTeleportOtherPlayers.get(data);
			chargeCostToTeleporter = chargeReagentsToTeleporter.get(data);

			start();
		}

		private void start() {
			tpCooldowns = new HashMap<>();
			MagicSpells.registerEvents(this);

			tpCooldowns.put(data.caster().getUniqueId(), (long) (System.currentTimeMillis() + startTeleportCooldown.get(data) * 1000));

			int interval = effectInterval.get(data);
			if (interval > 0) {
				taskPortal = MagicSpells.scheduleRepeatingTask(() -> {
					if (data.caster().isValid()) {
						playSpellEffects(EffectPosition.SPECIAL, startPortal.portalLocation(), data);
						playSpellEffects(EffectPosition.SPECIAL, endPortal.portalLocation(), data);

						playSpellEffects(EffectPosition.START_POSITION, startPortal.portalLocation(), data);
						playSpellEffects(EffectPosition.END_POSITION, endPortal.portalLocation(), data);
					}

				}, interval, interval, startPortal.portalLocation);
			}

			taskStop = MagicSpells.scheduleDelayedTask(this::stop, duration.get(data), startPortal.portalLocation);
		}

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			if (!teleportOtherPlayers && !event.getPlayer().equals(data.caster())) return;
			if (!event.hasExplicitlyChangedPosition()) return;
			if (!data.caster().isValid()) {
				stop();
				return;
			}

			Player pl = event.getPlayer();

			// Enters start portal
			if (checkHitbox(event.getTo(), startPortal)) {
				if (!checkTeleport(pl, startPortal)) return;

				PortalEnterEvent portalEvent = new PortalEnterEvent(pl, endPortal.portalLocation(), PortalSpell.this);
				if (!portalEvent.callEvent()) return;

				teleport(portalEvent.getDestination(), pl, event);
				return;
			}

			// Enters end portal
			if (allowReturn && checkHitbox(event.getTo(), endPortal)) {
				if (!checkTeleport(pl, endPortal)) return;

				PortalLeaveEvent portalEvent = new PortalLeaveEvent(pl, startPortal.portalLocation(), PortalSpell.this);
				if (!portalEvent.callEvent()) return;

				teleport(portalEvent.getDestination(), pl, event);
			}
		}

		private void teleport(Location loc, LivingEntity entity, PlayerMoveEvent event) {
			loc.setYaw(entity.getLocation().getYaw());
			loc.setPitch(entity.getLocation().getPitch());
			playSpellEffects(EffectPosition.TARGET, entity, data.target(entity));

			event.setTo(loc);
		}

		private boolean checkHitbox(Location location, Portal portal) {
			return portal.portalHitbox().contains(location);
		}

		private boolean checkTeleport(Player target, Portal portal) {
			SpellTargetEvent event = new SpellTargetEvent(PortalSpell.this, data, target);
			if (!event.callEvent()) return false;

			target = (Player) event.getTarget();

			if (!checkCooldown(target)) return false;
			if (!checkCost(target, portal)) return false;

			return true;
		}

		private boolean checkCooldown(Player target) {
			if (tpCooldowns.containsKey(target.getUniqueId()) && tpCooldowns.get(target.getUniqueId()) > System.currentTimeMillis()) {
				sendMessage(strTeleportOnCooldown, target, data);
				return false;
			}

			tpCooldowns.put(target.getUniqueId(), (long) (System.currentTimeMillis() + tpCooldown));
			return true;
		}

		private boolean checkCost(Player target, Portal portal) {
			LivingEntity payer;
			if (portal.portalCost == null) return true;

			if (chargeCostToTeleporter) {
				if (SpellUtil.hasReagents(target, portal.portalCost())) {
					payer = target;
				} else {
					sendMessage(strTeleportNoCost, target, data);
					return false;
				}
			} else {
				if (SpellUtil.hasReagents(data.caster(), portal.portalCost())) {
					payer = data.caster();
				} else {
					sendMessage(strTeleportNoCost, target, data);
					return false;
				}
			}

			if (payer == null) return false;
			SpellUtil.removeReagents(payer, portal.portalCost());
			return true;
		}

		private void stop() {
			HandlerList.unregisterAll(this);

			playSpellEffects(EffectPosition.DELAYED, startPortal.portalLocation(), data);
			playSpellEffects(EffectPosition.DELAYED, endPortal.portalLocation(), data);

			if (taskPortal != null) MagicSpells.cancelTask(taskPortal);
			if (taskStop != null) MagicSpells.cancelTask(taskStop);

			tpCooldowns.clear();
		}

	}

	private record Portal(Location portalLocation, SpellReagents portalCost, BoundingBox portalHitbox) {

	}

}
