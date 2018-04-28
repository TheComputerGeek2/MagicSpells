package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.magicspells.spells.instant.MarkSpell;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;

public class PortalSpell extends InstantSpell {

	private String firstMarkSpellName;
	private String secondMarkSpellName;
	int duration;
	int teleportCooldown;
	private int minDistanceSq;
	private int maxDistanceSq;
	int effectInterval;
	SpellReagents teleportCost;
	boolean allowReturn;
	boolean chargeCostToTeleporter;

	float horizRadius;
	float vertRadius;

	private String strNoMark;
	private String strTooClose;
	private String strTooFar;
	String strTeleportCostFail;
	String strTeleportCooldownFail;
	boolean tpOtherPlayers;

	private MarkSpell mark;
	private MarkSpell markSecond;

	private boolean usingSecondMarkSpell = false;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		this.horizRadius = getConfigFloat("horiz-radius", 1F);
		this.vertRadius = getConfigFloat("vert-radius", 1F);

		this.firstMarkSpellName = getConfigString("mark-spell", "mark");
		this.secondMarkSpellName = getConfigString("second-mark-spell", null);
		this.duration = getConfigInt("duration", 400);
		this.teleportCooldown = getConfigInt("teleport-cooldown", 5) * 1000;
		this.minDistanceSq = getConfigInt("min-distance", 10);
		this.minDistanceSq *= this.minDistanceSq;
		this.maxDistanceSq = getConfigInt("max-distance", 0);
		this.maxDistanceSq *= this.maxDistanceSq;
		this.effectInterval = getConfigInt("effect-interval", 10);
		this.teleportCost = getConfigReagents("teleport-cost");
		this.allowReturn = getConfigBoolean("allow-return", true);
		this.chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);
		this.tpOtherPlayers = getConfigBoolean("teleport-other-players", true);

		this.strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		this.strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		this.strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		this.strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		this.strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		//Parse the first mark spell.
		Spell spell = MagicSpells.getSpellByInternalName(this.firstMarkSpellName);
		if (spell instanceof MarkSpell) {
			this.mark = (MarkSpell)spell;
		} else {
			MagicSpells.error("Failed to get marks list for '" + this.internalName + "' spell");
		}

		//Parse the second if it is there.
		if (secondMarkSpellName != null) {
			spell = MagicSpells.getSpellByInternalName(this.secondMarkSpellName);
			if (spell instanceof MarkSpell) {
				this.markSecond = (MarkSpell)spell;
				usingSecondMarkSpell = true;
			} else {
				MagicSpells.error("Failed to get marks list for '" + this.internalName + "' spell");
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {

			//Declaring two location used to set up the portal link
			Location loc = this.mark.getEffectiveMark(player);
			Location locSecond;

			//The first one being the markspell must always be right.
			if (loc == null) {
				// No mark
				sendMessage(this.strNoMark, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			/*The second one can either be a player or a markspell.
			If we are using a second mark spell checked by the initialize function
			We are going to parse it, if incorrect we are not going to use the player's
			location.*/
			if (usingSecondMarkSpell) {
				locSecond = this.markSecond.getEffectiveMark(player);
				if (locSecond == null) {
					sendMessage(this.strNoMark, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			else locSecond = player.getLocation();

			double distanceSq = 0;
			if (this.maxDistanceSq > 0) {
				if (!loc.getWorld().equals(locSecond.getWorld())) {
					sendMessage(this.strTooFar, player, args);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq > this.maxDistanceSq) {
						sendMessage(this.strTooFar, player, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			if (this.minDistanceSq > 0) {
				if (loc.getWorld().equals(locSecond.getWorld())) {
					if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq < this.minDistanceSq) {
						sendMessage(this.strTooClose, player, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			new PortalLink(this, player, loc, locSecond);
			playSpellEffects(EffectPosition.CASTER, player);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean isBeneficialDefault() {
		return true;
	}

	class PortalLink implements Listener {

		PortalSpell spell;
		Player caster;
		Location loc1;
		Location loc2;
		BoundingBox box1;
		BoundingBox box2;
		int taskId1 = -1;
		int taskId2 = -1;
		Map<String, Long> cooldownUntil = new HashMap<>();

		public PortalLink (PortalSpell spell, Player caster, Location loc1, Location loc2) {
			this.spell = spell;
			this.caster = caster;
			this.loc1 = loc1;
			this.loc2 = loc2;
			this.box1 = new BoundingBox(loc1, PortalSpell.this.horizRadius, PortalSpell.this.vertRadius);
			this.box2 = new BoundingBox(loc2, PortalSpell.this.horizRadius, PortalSpell.this.vertRadius);

			this.cooldownUntil.put(caster.getName(), System.currentTimeMillis() + PortalSpell.this.teleportCooldown);
			registerEvents(this);
			startTasks();
		}

		void startTasks() {
			if (PortalSpell.this.effectInterval > 0) {
				this.taskId1 = MagicSpells.scheduleRepeatingTask(new Runnable() {
					@Override
					public void run() {
						if (caster.isValid()) {
							playSpellEffects(EffectPosition.SPECIAL, loc1);
							playSpellEffects(EffectPosition.SPECIAL, loc2);
						} else {
							disable();
						}
					}
				}, PortalSpell.this.effectInterval, PortalSpell.this.effectInterval);
			}
			this.taskId2 = MagicSpells.scheduleDelayedTask(this::disable, PortalSpell.this.duration);
		}

		@EventHandler(priority= EventPriority.NORMAL, ignoreCancelled=true)
		void onMove(PlayerMoveEvent event) {
			if (!tpOtherPlayers && !event.getPlayer().equals(caster)) return;
			if (this.caster.isValid()) {
				Player player = event.getPlayer();
				if (this.box1.contains(event.getTo())) {
					if (checkTeleport(player)) {
						Location loc = this.loc2.clone();
						loc.setYaw(player.getLocation().getYaw());
						loc.setPitch(player.getLocation().getPitch());
						event.setTo(loc);
						playSpellEffects(EffectPosition.TARGET, player);
					}
				} else if (PortalSpell.this.allowReturn && this.box2.contains(event.getTo())) {
					if (checkTeleport(player)) {
						Location loc = this.loc1.clone();
						loc.setYaw(player.getLocation().getYaw());
						loc.setPitch(player.getLocation().getPitch());
						event.setTo(loc);
						playSpellEffects(EffectPosition.TARGET, player);
					}
				}
			} else {
				disable();
			}
		}

		boolean checkTeleport(Player player) {
			if (cooldownUntil.containsKey(player.getName()) && cooldownUntil.get(player.getName()) > System.currentTimeMillis()) {
				sendMessage(strTeleportCooldownFail, player, MagicSpells.NULL_ARGS);
				return false;
			}
			cooldownUntil.put(player.getName(), System.currentTimeMillis() + teleportCooldown);

			Player payer = null;
			if (PortalSpell.this.teleportCost != null) {
				if (PortalSpell.this.chargeCostToTeleporter) {
					if (hasReagents(player, PortalSpell.this.teleportCost)) {
						payer = player;
					} else {
						sendMessage(PortalSpell.this.strTeleportCostFail, player, MagicSpells.NULL_ARGS);
						return false;
					}
				} else {
					if (hasReagents(this.caster, PortalSpell.this.teleportCost)) {
						payer = this.caster;
					} else {
						sendMessage(PortalSpell.this.strTeleportCostFail, player, MagicSpells.NULL_ARGS);
						return false;
					}
				}
				if (payer == null) return false;
			}

			SpellTargetEvent event = new SpellTargetEvent(this.spell, this.caster, player, 1);
			Bukkit.getPluginManager().callEvent(event);
			if (payer != null) removeReagents(payer, PortalSpell.this.teleportCost);
			return true;
		}

		void disable() {
			playSpellEffects(EffectPosition.DELAYED, this.loc1);
			playSpellEffects(EffectPosition.DELAYED, this.loc2);
			unregisterEvents(this);
			if (this.taskId1 > 0) MagicSpells.cancelTask(this.taskId1);
			if (this.taskId2 > 0) MagicSpells.cancelTask(this.taskId2);
			this.spell = null;
			this.caster = null;
			this.loc1 = null;
			this.loc2 = null;
			this.box1 = null;
			this.box2 = null;
			this.cooldownUntil.clear();
			this.cooldownUntil = null;
		}

	}

}
