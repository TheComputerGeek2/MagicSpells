package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FlightPathSpell extends InstantSpell {

	private FlightHandler flightHandler;

	private ConfigData<Float> speed;
	private ConfigData<Float> targetX;
	private ConfigData<Float> targetZ;

	private ConfigData<Integer> interval;
	private ConfigData<Integer> cruisingAltitude;

	public FlightPathSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		speed = getConfigDataFloat("speed", 1.5F);
		targetX = getConfigDataFloat("x", 0F);
		targetZ = getConfigDataFloat("z", 0F);

		interval = getConfigDataInt("interval", 5);
		cruisingAltitude = getConfigDataInt("cruising-altitude", 150);
	}

	@Override
	public void initialize() {
		super.initialize();

		flightHandler = new FlightHandler();
	}

	@Override
	public void turnOff() {
		if (flightHandler == null) return;
		flightHandler.turnOff();
		flightHandler = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			ActiveFlight flight = new ActiveFlight(player, power, args);
			flightHandler.addFlight(flight, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private class FlightHandler implements Runnable, Listener {

		private final Map<UUID, ActiveFlight> flights = new HashMap<>();

		private boolean initialized = false;

		private int task = -1;

		private FlightHandler() {
			init();
		}

		private void addFlight(ActiveFlight flight, float power, String[] args) {
			flights.put(flight.player.getUniqueId(), flight);
			flight.start();
			if (task < 0) task = MagicSpells.scheduleRepeatingTask(this, 0, interval.get(flight.player, null, power, args));
		}

		private void init() {
			if (initialized) return;
			initialized = true;
			MagicSpells.registerEvents(this);
		}

		private void cancel(Player player) {
			ActiveFlight flight = flights.remove(player.getUniqueId());
			if (flight != null) flight.cancel();
		}

		private void turnOff() {
			for (ActiveFlight flight : flights.values()) {
				flight.cancel();
			}
			MagicSpells.cancelTask(task);
			flights.clear();
		}

		@EventHandler
		private void onTeleport(PlayerTeleportEvent event) {
			cancel(event.getPlayer());
		}

		@EventHandler
		private void onPlayerDeath(PlayerDeathEvent event) {
			cancel(event.getEntity());
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent event) {
			cancel(event.getPlayer());
		}

		@Override
		public void run() {
			Iterator<ActiveFlight> iterator = flights.values().iterator();
			while (iterator.hasNext()) {
				ActiveFlight flight = iterator.next();
				if (flight.isDone()) iterator.remove();
				else flight.fly();
			}
			if (flights.isEmpty()) {
				MagicSpells.cancelTask(task);
				task = -1;
			}
		}

	}

	private class ActiveFlight {

		private Player player;
		private FlightState state;
		private Entity mountActive;
		private Entity entityToPush;
		private Location lastLocation;

		private boolean wasFlying;
		private boolean wasFlyingAllowed;

		private int sameLocCount = 0;

		private final float speed;
		private final float targetX;
		private final float targetZ;

		private final int cruisingAltitude;

		private ActiveFlight(Player caster, float power, String[] args) {
			player = caster;
			state = FlightState.TAKE_OFF;
			wasFlying = caster.isFlying();
			wasFlyingAllowed = caster.getAllowFlight();
			lastLocation = caster.getLocation();

			speed = FlightPathSpell.this.speed.get(caster, null, power, args);
			targetX = FlightPathSpell.this.targetX.get(caster, null, power, args);
			targetZ = FlightPathSpell.this.targetZ.get(caster, null, power, args);

			cruisingAltitude = FlightPathSpell.this.cruisingAltitude.get(caster, null, power, args);
		}

		private void start() {
			player.setAllowFlight(true);
			playSpellEffects(EffectPosition.CASTER, player);
			entityToPush = player;
		}

		private void fly() {
			if (state == FlightState.DONE) return;
			// Check for stuck
			if (player.getLocation().distanceSquared(lastLocation) < 0.4) {
				sameLocCount++;
			}
			if (sameLocCount > 12) {
				MagicSpells.error("Flight stuck '" + getInternalName() + "' at " + player.getLocation());
				cancel();
				return;
			}
			lastLocation = player.getLocation();

			// Do flight
			if (state == FlightState.TAKE_OFF) {
				player.setFlying(false);
				double y = entityToPush.getLocation().getY();
				if (y >= cruisingAltitude) {
					entityToPush.setVelocity(new Vector(0, 0, 0));
					state = FlightState.CRUISING;
				} else entityToPush.setVelocity(new Vector(0, 2, 0));
			} else if (state == FlightState.CRUISING) {
				player.setFlying(true);
				double x = entityToPush.getLocation().getX();
				double z = entityToPush.getLocation().getZ();
				if (targetX - 1 <= x && x <= targetX + 1 && targetZ - 1 <= z && z <= targetZ + 1) {
					entityToPush.setVelocity(new Vector(0, 0, 0));
					state = FlightState.LANDING;
				} else {
					Vector t = new Vector(targetX, cruisingAltitude, targetZ);
					Vector v = t.subtract(entityToPush.getLocation().toVector());
					double len = v.lengthSquared();
					v.normalize().multiply(len > 25 ? speed : 0.3);
					entityToPush.setVelocity(v);
				}
			} else if (state == FlightState.LANDING) {
				player.setFlying(false);
				Location l = entityToPush.getLocation();
				if (!BlockUtils.isAir(l.getBlock().getType()) || !BlockUtils.isAir(l.subtract(0, 1, 0).getBlock().getType()) || !BlockUtils.isAir(l.subtract(0, 2, 0).getBlock().getType())) {
					player.setFallDistance(0f);
					cancel();
					return;
				} else {
					entityToPush.setVelocity(new Vector(0, -1, 0));
					player.setFallDistance(0f);
				}
			}

			playSpellEffects(EffectPosition.SPECIAL, player);
		}

		private void cancel() {
			if (state != FlightState.DONE) {
				state = FlightState.DONE;
				player.setFlying(wasFlying);
				player.setAllowFlight(wasFlyingAllowed);
				if (mountActive != null) {
					mountActive.eject();
					mountActive.remove();
				}
				playSpellEffects(EffectPosition.DELAYED, player);

				player = null;
				mountActive = null;
				entityToPush = null;
			}
		}

		private boolean isDone() {
			return state == FlightState.DONE;
		}

	}

	private enum FlightState {

		TAKE_OFF,
		CRUISING,
		LANDING,
		DONE

	}

}
