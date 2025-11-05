package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class StunSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Map<UUID, StunnedInfo> stunnedLivingEntities;

	private final int interval;
	private final ConfigData<Integer> duration;

	private final ConfigData<Boolean> stunBody;
	private final ConfigData<Boolean> stunMonitor;
	private final ConfigData<Boolean> useTargetLocation;
	private final ConfigData<Boolean> powerAffectsDuration;

	private final StunListener listener;

	public StunSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		interval = getConfigInt("interval", 5);
		duration = getConfigDataInt("duration", 200);

		stunBody = getConfigDataBoolean("stun-body", true);
		stunMonitor = getConfigDataBoolean("stun-monitor", true);
		useTargetLocation = getConfigDataBoolean("use-target-location", true);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);

		listener = new StunListener();
		stunnedLivingEntities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		if ((stunBody.isConstant() && !stunBody.get()) && (stunMonitor.isConstant() && !stunMonitor.get())) {
			MagicSpells.error("StunSpell '" + internalName + "' is not attempting to stun the body or the monitor.");
			return;
		}

		registerEvents(listener);
		MagicSpells.scheduleRepeatingTask(new StunMonitor(), interval, interval);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		long duration = this.duration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND / 20;
		if (powerAffectsDuration.get(data)) duration = Math.round(duration * data.power());

		boolean stunBody = this.stunBody.get(data);
		boolean stunMonitor = this.stunMonitor.get(data);
		boolean useTargetLocation = this.useTargetLocation.get(data);
		Location targetLocation = useTargetLocation ? data.target().getLocation() : null;

		StunnedInfo info = new StunnedInfo(data.caster(), data.target(), System.currentTimeMillis() + duration, stunBody, stunMonitor, useTargetLocation, targetLocation);
		stunnedLivingEntities.put(data.target().getUniqueId(), info);

		playSpellEffects(data);
		playSpellEffectsBuff(data.target(), e -> stunnedLivingEntities.containsKey(e.getUniqueId()), data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public boolean isStunned(LivingEntity entity) {
		return stunnedLivingEntities.containsKey(entity.getUniqueId());
	}

	public void removeStun(LivingEntity entity) {
		stunnedLivingEntities.remove(entity.getUniqueId());
	}

	private record StunnedInfo(LivingEntity caster, LivingEntity target, long until, boolean stunBody, boolean stunMonitor, boolean useTargetLocation, Location targetLocation) {
	}

	private class StunListener implements Listener {

		@EventHandler
		private void onMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			if (!isStunned(player)) return;
			StunnedInfo info = stunnedLivingEntities.get(player.getUniqueId());
			if (info == null) return;

			if (System.currentTimeMillis() >= info.until) {
				removeStun(player);
				return;
			}

			boolean stunBody = info.stunBody && event.hasExplicitlyChangedPosition();
			boolean stunMonitor = info.stunMonitor && event.hasChangedOrientation();
			if (!stunBody && !stunMonitor) return;
			player.teleport(info.useTargetLocation ? info.targetLocation : event.getFrom());
		}

		@EventHandler
		private void onInteract(PlayerInteractEvent event) {
			if (!isStunned(event.getPlayer())) return;
			event.setCancelled(true);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			if (!isStunned(player)) return;
			removeStun(player);
		}

		@EventHandler
		private void onDeath(PlayerDeathEvent event) {
			Player player = event.getEntity();
			if (!isStunned(player)) return;
			removeStun(player);
		}

	}

	private class StunMonitor implements Runnable {

		private final Set<LivingEntity> toRemove = new HashSet<>();

		@Override
		public void run() {
			for (UUID id : stunnedLivingEntities.keySet()) {
				StunnedInfo info = stunnedLivingEntities.get(id);
				LivingEntity entity = info.target;
				long until = info.until;
				if (entity instanceof Player) continue;

				if (entity.isValid() && until > System.currentTimeMillis()) {
					entity.teleport(info.targetLocation);
					continue;
				}

				toRemove.add(entity);
			}

			for (LivingEntity entity : toRemove) {
				removeStun(entity);
			}

			toRemove.clear();
		}

	}

}
