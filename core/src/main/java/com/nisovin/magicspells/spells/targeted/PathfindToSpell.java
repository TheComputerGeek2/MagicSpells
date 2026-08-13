package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;

public class PathfindToSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static Monitor monitor;

	private final ConfigData<Vector> position;

	private final ConfigData<Double> speed;
	private final ConfigData<Double> distanceAllowed;

	private final ConfigData<Boolean> allowInterrupt;
	private final ConfigData<Boolean> forceCasterNavigator;

	private Subspell arriveSpell;

	public PathfindToSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		position = getConfigDataVector("position", null);

		speed = getConfigDataDouble("speed", 1);
		distanceAllowed = getConfigDataDouble("distance-allowed", 1);

		allowInterrupt = getConfigDataBoolean("allow-interrupt", true);
		forceCasterNavigator = getConfigDataBoolean("force-caster-navigator", false);
	}

	@Override
	protected void initialize() {
		super.initialize();

		if (monitor == null) monitor = new Monitor();

		arriveSpell = initSubspell(
			getConfigString("spell-on-arrive", null),
			"PathfindToSpell '" + internalName + "' has an invalid 'spell-on-arrive' defined.",
			true
		);
	}

	@Override
	protected void turnOff() {
		super.turnOff();

		if (monitor == null) return;
		monitor.stop();
		monitor = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		data = info.spellData();
		if (!(data.target() instanceof Mob mob)) return noTarget(data);

		return setPath(mob, position.get(data).toLocation(mob.getWorld()), data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		Mob navigator = null;
		Location destination = null;

		if (forceCasterNavigator.get(data)) {
			if (data.caster() instanceof Mob mob) {
				navigator = mob;
				destination = data.target().getLocation();
			}
		} else {
			if (data.target() instanceof Mob mob) {
				navigator = mob;
				destination = position.get(data).toLocation(mob.getWorld());
			}
		}
		if (navigator == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		return setPath(navigator, destination, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!(data.caster() instanceof Mob navigator))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		return setPath(navigator, data.location(), data);
	}

	private CastResult setPath(Mob mob, Location destination, SpellData data) {
		Pathfinder.PathResult path = mob.getPathfinder().findPath(destination);
		if (path == null || !path.canReachFinalPoint()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		mob.getPathfinder().moveTo(path, speed.get(data));

		monitor.track(new MonitorData(
			data,
			mob.getUniqueId(),
			destination,
			NumberConversions.square(distanceAllowed.get(data)),
			allowInterrupt.get(data),
			arriveSpell
		));

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private record MonitorData(
		SpellData spellData,
		UUID target,
		Location destination,
		double distanceAllowedSquared,
		boolean allowInterrupt,
		Subspell arriveSpell
	) {}

	private static class Monitor implements Runnable, Listener {

		private final Map<UUID, MonitorData> mobs = new HashMap<>();

		private int taskId = -1;

		public void track(MonitorData data) {
			mobs.put(data.target(), data);
			start();
		}

		public void start() {
			if (taskId != -1) return;

			MagicSpells.registerEvents(this);
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, 1);
		}

		public void stop() {
			if (taskId == -1) return;

			EntityPathfindEvent.getHandlerList().unregister(this);
			MagicSpells.cancelTask(taskId);
			taskId = -1;
		}

		@Override
		public void run() {
			mobs.values().removeIf(Monitor::removeIf);
			if (mobs.isEmpty()) stop();
		}

		private static boolean removeIf(MonitorData data) {
			if (!(Bukkit.getEntity(data.target()) instanceof Mob mob) || !mob.isValid()) return true;

			if (mob.getLocation().distanceSquared(data.destination()) > data.distanceAllowedSquared()) {
				Pathfinder.PathResult path = mob.getPathfinder().getCurrentPath();
				return path == null || !path.canReachFinalPoint();
			}

			mob.getPathfinder().stopPathfinding();
			if (data.arriveSpell() != null) data.arriveSpell().subcast(data.spellData());

			return true;
		}

		@EventHandler(priority = EventPriority.LOWEST)
		private void onPath(EntityPathfindEvent event) {
			if (!(event.getEntity() instanceof Mob mob)) return;

			MonitorData data = mobs.get(mob.getUniqueId());
			if (data == null) return;

			if (data.allowInterrupt()) {
				mobs.remove(data.target());
				return;
			}

			event.setCancelled(true);
		}

	}

}
