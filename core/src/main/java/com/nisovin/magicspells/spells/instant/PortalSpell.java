package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PortalSpell extends InstantSpell {

	private final String firstMarkSpellName;
	private final String secondMarkSpellName;

	private MarkSpell firstMark;
	private MarkSpell secondMark;

	private SpellReagents teleportCost;

	private ConfigData<Integer> duration;
	private ConfigData<Integer> minDistance;
	private ConfigData<Integer> maxDistance;
	private ConfigData<Integer> effectInterval;
	private ConfigData<Integer> teleportCooldown;

	private ConfigData<Float> vertRadius;
	private ConfigData<Float> horizRadius;

	private boolean allowReturn;
	private boolean tpOtherPlayers;
	private boolean usingSecondMarkSpell;
	private boolean chargeCostToTeleporter;

	private String strNoMark;
	private String strTooFar;
	private String strTooClose;
	private String strTeleportCostFail;
	private String strTeleportCooldownFail;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		firstMarkSpellName = getConfigString("mark-spell", "");
		secondMarkSpellName = getConfigString("second-mark-spell", "");

		teleportCost = getConfigReagents("teleport-cost");

		duration = getConfigDataInt("duration", 400);
		minDistance = getConfigDataInt("min-distance", 10);
		maxDistance = getConfigDataInt("max-distance", 0);
		effectInterval = getConfigDataInt("effect-interval", 10);
		teleportCooldown = getConfigDataInt("teleport-cooldown", 5);

		horizRadius = getConfigDataFloat("horiz-radius", 1F);
		vertRadius = getConfigDataFloat("vert-radius", 1F);

		allowReturn = getConfigBoolean("allow-return", true);
		tpOtherPlayers = getConfigBoolean("teleport-other-players", true);
		chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);

		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(firstMarkSpellName);
		if (spell instanceof MarkSpell) firstMark = (MarkSpell) spell;
		else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid mark-spell defined!");

		usingSecondMarkSpell = false;
		if (!secondMarkSpellName.isEmpty()) {
			spell = MagicSpells.getSpellByInternalName(secondMarkSpellName);
			if (spell instanceof MarkSpell) {
				secondMark = (MarkSpell) spell;
				usingSecondMarkSpell = true;
			} else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid second-mark-spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = firstMark.getEffectiveMark(caster);
			Location locSecond;
			if (loc == null) {
				sendMessage(strNoMark, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (usingSecondMarkSpell) {
				locSecond = secondMark.getEffectiveMark(caster);
				if (locSecond == null) {
					sendMessage(strNoMark, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else locSecond = caster.getLocation();

			double distanceSq = 0;

			float maxDistanceSq = maxDistance.get(caster, null, power, args);
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

			float minDistanceSq = minDistance.get(caster, null, power, args);
			minDistanceSq *= minDistanceSq;
			if (minDistanceSq > 0) {
				if (loc.getWorld().equals(locSecond.getWorld())) {
					if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq < minDistanceSq) {
						sendMessage(strTooClose, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			new PortalLink(caster, loc, locSecond, power, args);
			playSpellEffects(EffectPosition.CASTER, caster);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public MarkSpell getFirstMark() {
		return firstMark;
	}

	public void setFirstMark(MarkSpell firstMark) {
		this.firstMark = firstMark;
	}

	public MarkSpell getSecondMark() {
		return secondMark;
	}

	public void setSecondMark(MarkSpell secondMark) {
		this.secondMark = secondMark;
	}

	public SpellReagents getTeleportCost() {
		return teleportCost;
	}

	public void setTeleportCost(SpellReagents teleportCost) {
		this.teleportCost = teleportCost;
	}

	public boolean shouldAllowReturn() {
		return allowReturn;
	}

	public void setAllowReturn(boolean allowReturn) {
		this.allowReturn = allowReturn;
	}

	public boolean shouldTpOtherPlayers() {
		return tpOtherPlayers;
	}

	public void setTpOtherPlayers(boolean tpOtherPlayers) {
		this.tpOtherPlayers = tpOtherPlayers;
	}

	public boolean isUsingSecondMarkSpell() {
		return usingSecondMarkSpell;
	}

	public void setUsingSecondMarkSpell(boolean usingSecondMarkSpell) {
		this.usingSecondMarkSpell = usingSecondMarkSpell;
	}

	public boolean shouldChargeCostToTeleporter() {
		return chargeCostToTeleporter;
	}

	public void setChargeCostToTeleporter(boolean chargeCostToTeleporter) {
		this.chargeCostToTeleporter = chargeCostToTeleporter;
	}

	public String getStrNoMark() {
		return strNoMark;
	}

	public void setStrNoMark(String strNoMark) {
		this.strNoMark = strNoMark;
	}

	public String getStrTooFar() {
		return strTooFar;
	}

	public void setStrTooFar(String strTooFar) {
		this.strTooFar = strTooFar;
	}

	public String getStrTooClose() {
		return strTooClose;
	}

	public void setStrTooClose(String strTooClose) {
		this.strTooClose = strTooClose;
	}

	public String getStrTeleportCostFail() {
		return strTeleportCostFail;
	}

	public void setStrTeleportCostFail(String strTeleportCostFail) {
		this.strTeleportCostFail = strTeleportCostFail;
	}

	public String getStrTeleportCooldownFail() {
		return strTeleportCooldownFail;
	}

	public void setStrTeleportCooldownFail(String strTeleportCooldownFail) {
		this.strTeleportCooldownFail = strTeleportCooldownFail;
	}

	private class PortalLink implements Listener {

		private final LivingEntity caster;
		private final String[] args;
		private final float power;

		private final Location loc1;
		private final Location loc2;

		private final BoundingBox box1;
		private final BoundingBox box2;

		private final Map<UUID, Long> cooldownUntil;
		private int taskId1 = -1;
		private int taskId2 = -1;

		private PortalLink(LivingEntity caster, Location loc1, Location loc2, float power, String[] args) {
			this.caster = caster;
			this.power = power;
			this.args = args;

			this.loc1 = loc1;
			this.loc2 = loc2;

			float horizRadius = PortalSpell.this.horizRadius.get(caster, null, power, args);
			float vertRadius = PortalSpell.this.vertRadius.get(caster, null, power, args);

			box1 = new BoundingBox(loc1, horizRadius, vertRadius);
			box2 = new BoundingBox(loc2, horizRadius, vertRadius);

			cooldownUntil = new HashMap<>();
			int teleportCooldown = PortalSpell.this.teleportCooldown.get(caster, caster, power, args) * 1000;
			cooldownUntil.put(caster.getUniqueId(), System.currentTimeMillis() + teleportCooldown);

			registerEvents(this);
			startTasks();
		}

		private void startTasks() {
			int effectInterval = PortalSpell.this.effectInterval.get(caster, null, power, args);

			if (effectInterval > 0) {
				taskId1 = MagicSpells.scheduleRepeatingTask(() -> {
					if (caster.isValid()) {
						playSpellEffects(EffectPosition.SPECIAL, loc1);
						playSpellEffects(EffectPosition.SPECIAL, loc2);
					} else disable();

				}, effectInterval, effectInterval);
			}

			int duration = PortalSpell.this.duration.get(caster, null, power, args);
			taskId2 = MagicSpells.scheduleDelayedTask(this::disable, duration);
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
					playSpellEffects(EffectPosition.TARGET, player);
				}
			} else if (allowReturn && box2.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc1.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playSpellEffects(EffectPosition.TARGET, player);
				}
			}
		}

		private boolean checkTeleport(LivingEntity target) {
			SpellTargetEvent event = new SpellTargetEvent(PortalSpell.this, caster, target, power, args);
			if (!event.callEvent()) return false;

			target  = event.getTarget();

			if (cooldownUntil.containsKey(target.getUniqueId()) && cooldownUntil.get(target.getUniqueId()) > System.currentTimeMillis()) {
				sendMessage(strTeleportCooldownFail, target, args);
				return false;
			}

			float power = event.getPower();

			int teleportCooldown = PortalSpell.this.teleportCooldown.get(caster, target, power, args) * 1000;
			cooldownUntil.put(target.getUniqueId(), System.currentTimeMillis() + teleportCooldown);

			LivingEntity payer = null;
			if (teleportCost != null) {
				if (chargeCostToTeleporter) {
					if (hasReagents(target, teleportCost)) {
						payer = target;
					} else {
						sendMessage(strTeleportCostFail, target, args);
						return false;
					}
				} else {
					if (hasReagents(this.caster, teleportCost)) {
						payer = this.caster;
					} else {
						sendMessage(strTeleportCostFail, target, args);
						return false;
					}
				}
				if (payer == null) return false;
			}

			if (payer != null) removeReagents(payer, teleportCost);

			return true;
		}

		private void disable() {
			unregisterEvents(this);

			playSpellEffects(EffectPosition.DELAYED, loc1);
			playSpellEffects(EffectPosition.DELAYED, loc2);

			if (taskId1 > 0) MagicSpells.cancelTask(taskId1);
			if (taskId2 > 0) MagicSpells.cancelTask(taskId2);

			cooldownUntil.clear();
		}

	}

}
