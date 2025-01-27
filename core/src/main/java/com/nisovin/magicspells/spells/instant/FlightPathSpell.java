package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Iterator;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FlightPathSpell extends InstantSpell {

	private FlightHandler flightHandler;

	private final String landSpellName;

	private final ConfigData<Float> speed;
	private final ConfigData<Float> targetX;
	private final ConfigData<Float> targetZ;

	private final int interval;
	private final ConfigData<Integer> cruisingAltitude;

	private Subspell landSpell;

	public FlightPathSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		speed = getConfigDataFloat("speed", 1.5F);
		targetX = getConfigDataFloat("x", 0F);
		targetZ = getConfigDataFloat("z", 0F);

		interval = getConfigInt("interval", 5);
		cruisingAltitude = getConfigDataInt("cruising-altitude", 150);

		landSpellName = getConfigString("land-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = initSubspell(landSpellName,
				"FlightPathSpell '" + internalName + "' has an invalid land-spell defined!",
				true);

		flightHandler = new FlightHandler();
	}

	@Override
	public void turnOff() {
		if (flightHandler == null) return;
		flightHandler.turnOff();
		flightHandler = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ActiveFlight flight = new ActiveFlight(caster, data);
		flightHandler.addFlight(flight);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private class FlightHandler implements Runnable, Listener {

		private final Map<UUID, ActiveFlight> flights = new HashMap<>();

		private boolean initialized = false;

		private ScheduledTask task = null;

		private FlightHandler() {
			init();
		}

		private void addFlight(ActiveFlight flight) {
			flights.put(flight.caster.getUniqueId(), flight);
			flight.start();
			if (task == null) task = MagicSpells.scheduleRepeatingTask(this, 0, interval, flight.caster);
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
				task = null;
			}
		}

	}

	private class ActiveFlight {

		private final SpellData data;
		private final Player caster;

		private FlightState state;
		private Location lastLocation;

		private final boolean wasFlying;
		private final boolean wasFlyingAllowed;

		private int sameLocCount = 0;

		private final float speed;
		private final float targetX;
		private final float targetZ;
		private final int cruisingAltitude;

		private ActiveFlight(Player caster, SpellData data) {
			this.caster = caster;
			this.data = data;

			state = FlightState.TAKE_OFF;
			wasFlying = caster.isFlying();
			wasFlyingAllowed = caster.getAllowFlight();
			lastLocation = caster.getLocation();

			speed = FlightPathSpell.this.speed.get(data);
			targetX = FlightPathSpell.this.targetX.get(data);
			targetZ = FlightPathSpell.this.targetZ.get(data);

			cruisingAltitude = FlightPathSpell.this.cruisingAltitude.get(data);
		}

		private void start() {
			caster.setAllowFlight(true);
			playSpellEffects(EffectPosition.CASTER, caster, data);
		}

		private void fly() {
			if (state == FlightState.DONE) return;
			// Check for stuck
			if (caster.getLocation().distanceSquared(lastLocation) < 0.4) {
				sameLocCount++;
			}
			if (sameLocCount > 12) {
				MagicSpells.error("Flight stuck '" + getInternalName() + "' at " + caster.getLocation());
				cancel();
				return;
			}
			lastLocation = caster.getLocation();

			// Do flight
			if (state == FlightState.TAKE_OFF) {
				caster.setFlying(false);
				double y = caster.getLocation().getY();
				if (y >= cruisingAltitude) {
					caster.setVelocity(new Vector(0, 0, 0));
					state = FlightState.CRUISING;
				} else caster.setVelocity(new Vector(0, 2, 0));
			} else if (state == FlightState.CRUISING) {
				caster.setFlying(true);
				double x = caster.getLocation().getX();
				double z = caster.getLocation().getZ();
				if (targetX - 1 <= x && x <= targetX + 1 && targetZ - 1 <= z && z <= targetZ + 1) {
					caster.setVelocity(new Vector(0, 0, 0));
					state = FlightState.LANDING;
				} else {
					Vector t = new Vector(targetX, cruisingAltitude, targetZ);
					Vector v = t.subtract(caster.getLocation().toVector());
					double len = v.lengthSquared();
					v.normalize().multiply(len > 25 ? speed : 0.3);
					caster.setVelocity(v);
				}
			} else if (state == FlightState.LANDING) {
				caster.setFlying(false);
				Location l = caster.getLocation();
				if (!l.getBlock().getType().isAir() || !l.subtract(0, 1, 0).getBlock().getType().isAir() || !l.subtract(0, 2, 0).getBlock().getType().isAir()) {
					caster.setFallDistance(0f);
					if (landSpell != null) landSpell.subcast(data);
					cancel();
					return;
				} else {
					caster.setVelocity(new Vector(0, -1, 0));
					caster.setFallDistance(0f);
				}
			}

			playSpellEffects(EffectPosition.SPECIAL, caster, data);
		}

		private void cancel() {
			if (state != FlightState.DONE) {
				state = FlightState.DONE;
				caster.setFlying(wasFlying);
				caster.setAllowFlight(wasFlyingAllowed);
				playSpellEffects(EffectPosition.DELAYED, caster, data);
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
