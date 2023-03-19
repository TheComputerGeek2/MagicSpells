package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.SpellUtil;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PortalSpell extends InstantSpell {

	private ConfigData<Integer> duration;
	private ConfigData<Integer> minDistance;
	private ConfigData<Integer> maxDistance;
	private ConfigData<Integer> effectInterval;

	private ConfigData<Double> teleportCooldown;

	private ConfigData<Float> vRadiusStart;
	private ConfigData<Float> hRadiusStart;
	private ConfigData<Float> vRadiusEnd;
	private ConfigData<Float> hRadiusEnd;

	private ConfigData<Boolean> canReturn;
	private ConfigData<Boolean> canTeleportOtherPlayers;
	private ConfigData<Boolean> chargeReagentsToTeleporter;

	private String strNoMark;
	private String strTooFar;
	private String strTooClose;
	private String strTeleportNoCost;
	private String strTeleportOnCooldown;

	private MarkSpell startMark;
	private MarkSpell endMark;

	private final String startMarkSpellName;
	private final String endMarkSpellName;

	private SpellReagents teleportReagents;

	private boolean usingSecondMarkSpell;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 400);
		minDistance = getConfigDataInt("min-distance", 10);
		maxDistance = getConfigDataInt("max-distance", 0);
		effectInterval = getConfigDataInt("effect-interval", 10);

		teleportCooldown = getConfigDataDouble("teleport-cooldown", 5.0);

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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {

			SpellData data = new SpellData(caster, null, power, args);
			Location loc = startMark.getEffectiveMark(caster);

			Location locSecond;
			if (loc == null) {
				sendMessage(strNoMark, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (usingSecondMarkSpell) {
				locSecond = endMark.getEffectiveMark(caster);
				if (locSecond == null) {
					sendMessage(strNoMark, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else locSecond = caster.getLocation();

			double distanceSq = 0;

			float maxDistanceSq = maxDistance.get(data);
			maxDistanceSq *= maxDistanceSq;

			if (maxDistanceSq > 0) {
				if (!loc.getWorld().equals(locSecond.getWorld())) {
					sendMessage(strTooFar, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq > maxDistanceSq) {
						sendMessage(strTooFar, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			float minDistanceSq = minDistance.get(data);
			minDistanceSq *= minDistanceSq;

			if (minDistanceSq > 0 && loc.getWorld().equals(locSecond.getWorld())) {
				if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
				if (distanceSq < minDistanceSq) {
					sendMessage(strTooClose, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			}

			Portal startPortal = new Portal(loc, teleportReagents, new BoundingBox(loc, hRadiusStart.get(data), vRadiusStart.get(data)));
			Portal endPortal = new Portal(locSecond, teleportReagents, new BoundingBox(locSecond, hRadiusEnd.get(data), vRadiusEnd.get(data)));

			new PortalLink(caster, startPortal, endPortal, power, data);

			playSpellEffects(EffectPosition.CASTER, caster, data);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public ConfigData<Integer> getEffectInterval() {
		return effectInterval;
	}

	public ConfigData<Integer> getDuration() {
		return duration;
	}

	public ConfigData<Integer> getMinDistance() {
		return duration;
	}

	public ConfigData<Integer> getMaxDistance() {
		return duration;
	}

	public ConfigData<Double> getTeleportCooldown() {
		return teleportCooldown;
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

		private double tpCooldown;

		private boolean allowReturn;
		private boolean teleportOtherPlayers;
		private boolean chargeCostToTeleporter;

		private LivingEntity caster;
		private SpellData data;
		private float power;

		private Portal startPortal;
		private Portal endPortal;

		private int taskPortal = -1;
		private int taskStop = -1;

		private PortalLink(LivingEntity caster, Portal startPortal, Portal endPortal, float power, SpellData data) {
			this.caster = caster;
			this.startPortal = startPortal;
			this.endPortal = endPortal;
			this.power = power;
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

			tpCooldowns.put(caster.getUniqueId(), (long) (System.currentTimeMillis() + tpCooldown));

			int interval = effectInterval.get(data);
			if (interval > 0) {
				taskPortal = MagicSpells.scheduleRepeatingTask(() -> {
					if (caster.isValid()) {
						playSpellEffects(EffectPosition.SPECIAL, startPortal.portalLocation(), data);
						playSpellEffects(EffectPosition.SPECIAL, endPortal.portalLocation(), data);

						playSpellEffects(EffectPosition.START_POSITION, startPortal.portalLocation(), data);
						playSpellEffects(EffectPosition.END_POSITION, endPortal.portalLocation(), data);
					}

				}, interval, interval);
			}

			taskStop = MagicSpells.scheduleDelayedTask(this::stop, duration.get(data));
		}

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			if (!teleportOtherPlayers && !event.getPlayer().equals(caster)) return;
			if (event.getFrom().toVector().equals(event.getTo().toVector())) return;
			if (!caster.isValid()) {
				stop();
				return;
			}

			Player pl = event.getPlayer();

			// Enters start portal
			if (checkHitbox(event.getTo(), startPortal)) {
				if (!checkTeleport(pl, startPortal)) return;
				teleport(endPortal.portalLocation().clone(), pl, event);

				return;
			}

			// Enters end portal
			if (allowReturn && checkHitbox(event.getTo(), endPortal)) {
				if (!checkTeleport(pl, endPortal)) return;
				teleport(startPortal.portalLocation().clone(), pl, event);
			}
		}

		private void teleport(Location loc, LivingEntity entity, PlayerMoveEvent event) {
			loc.setYaw(entity.getLocation().getYaw());
			loc.setPitch(entity.getLocation().getPitch());
			playSpellEffects(EffectPosition.TARGET, entity, data);

			event.setTo(loc);
		}

		private boolean checkHitbox(Location location, Portal portal) {
			return portal.portalHitbox().contains(location);
		}

		private boolean checkTeleport(Player target, Portal portal) {
			SpellTargetEvent event = new SpellTargetEvent(PortalSpell.this, caster, target, power, data.args());
			if (!event.callEvent()) return false;

			target = (Player) event.getTarget();

			if (!checkCooldown(target)) return false;
			if (!checkCost(target, portal)) return false;

			return true;
		}

		private boolean checkCooldown(Player target) {
			if (tpCooldowns.containsKey(target.getUniqueId()) && tpCooldowns.get(target.getUniqueId()) > System.currentTimeMillis()) {
				sendMessage(strTeleportOnCooldown, target, data.args());
				return false;
			}

			tpCooldowns.put(target.getUniqueId(), (long) (System.currentTimeMillis() + tpCooldown));
			return true;
		}

		private boolean checkCost(Player target, Portal portal) {
			LivingEntity payer = null;

			if (portal.portalCost() != null) {
				if (chargeCostToTeleporter) {
					if (SpellUtil.hasReagents(target, portal.portalCost())) {
						payer = target;
					} else {
						sendMessage(strTeleportNoCost, target, data.args());
						return false;
					}
				} else {
					if (SpellUtil.hasReagents(caster, portal.portalCost())) {
						payer = caster;
					} else {
						sendMessage(strTeleportNoCost, target, data.args());
						return false;
					}
				}
				if (payer == null) return false;
			}

			if (payer != null) SpellUtil.removeReagents(payer, portal.portalCost());
			return true;
		}

		private void stop() {
			HandlerList.unregisterAll(this);

			playSpellEffects(EffectPosition.DELAYED, startPortal.portalLocation(), data);
			playSpellEffects(EffectPosition.DELAYED, endPortal.portalLocation(), data);

			if (taskPortal > 0) MagicSpells.cancelTask(taskPortal);
			if (taskStop > 0) MagicSpells.cancelTask(taskStop);

			tpCooldowns.clear();
		}

	}

	private record Portal(Location portalLocation, SpellReagents portalCost, BoundingBox portalHitbox) {

	}

}
