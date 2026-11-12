package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import io.papermc.paper.event.entity.EntityMoveEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

public class StunSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Map<UUID, StunnedInfo> stunned = new HashMap<>();

	private final ConfigData<Integer> duration;

	private final ConfigData<Boolean> stunBody;
	private final ConfigData<Boolean> stunMonitor;
	private final ConfigData<Boolean> useTargetLocation;
	private final ConfigData<Boolean> powerAffectsDuration;

	private final StunListener listener = new StunListener();

	public StunSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 200);

		stunBody = getConfigDataBoolean("stun-body", true);
		stunMonitor = getConfigDataBoolean("stun-monitor", true);
		useTargetLocation = getConfigDataBoolean("use-target-location", true);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);
	}

	@Override
	public void initialize() {
		super.initialize();

		if ((stunBody.isConstant() && !stunBody.get()) && (stunMonitor.isConstant() && !stunMonitor.get())) {
			MagicSpells.error("StunSpell '" + internalName + "' is not attempting to stun the body or the monitor.");
			return;
		}

		registerEvents(listener);
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

		StunnedInfo info = new StunnedInfo(
			data.caster(),
			data.target(),
			System.currentTimeMillis() + duration,
			stunBody.get(data),
			stunMonitor.get(data),
			useTargetLocation.get(data) ? data.target().getLocation() : null
		);
		stunned.put(data.target().getUniqueId(), info);

		Bukkit.getAsyncScheduler().runDelayed(
			MagicSpells.getInstance(),
			_ -> {
				if (info.until > System.currentTimeMillis()) return;
				MagicSpells.scheduleDelayedTask(() -> removeStun(data.target()), 0);
			},
			duration,
			TimeUnit.MILLISECONDS
		);

		playSpellEffects(data);
		playSpellEffectsBuff(data.target(), this::isStunned, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public boolean isStunned(Entity entity) {
		return stunned.containsKey(entity.getUniqueId());
	}

	public void removeStun(Entity entity) {
		stunned.remove(entity.getUniqueId());
	}

	private record StunnedInfo(
		LivingEntity caster,
		LivingEntity target,
		long until,
		boolean stunBody,
		boolean stunMonitor,
		Location targetLocation
	) {}

	private class StunListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		private void onMove(EntityMoveEvent event) {
			onMove(event.getEntity(), event.hasExplicitlyChangedPosition(), event.hasChangedOrientation(), event.getFrom());
		}

		@EventHandler(ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			onMove(event.getPlayer(), event.hasExplicitlyChangedPosition(), event.hasChangedOrientation(), event.getFrom());
		}

		private void onMove(LivingEntity entity, boolean hasExplicitlyChangedPosition, boolean hasChangedOrientation, Location from) {
			StunnedInfo info = stunned.get(entity.getUniqueId());
			if (info == null) return;

			if (System.currentTimeMillis() >= info.until) {
				removeStun(entity);
				return;
			}

			boolean stunBody = info.stunBody && hasExplicitlyChangedPosition;
			boolean stunMonitor = info.stunMonitor && hasChangedOrientation;
			if (!stunBody && !stunMonitor) return;

			entity.teleport(Objects.requireNonNullElse(info.targetLocation, from));
		}

		@EventHandler
		private void onInteract(PlayerInteractEvent event) {
			if (!isStunned(event.getPlayer())) return;
			event.setCancelled(true);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent event) {
			removeStun(event.getPlayer());
		}

		@EventHandler(ignoreCancelled = true)
		private void onDeath(PlayerDeathEvent event) {
			removeStun(event.getEntity());
		}

		@EventHandler
		private void onRemove(EntityRemoveFromWorldEvent event) {
			removeStun(event.getEntity());
		}

	}

}
