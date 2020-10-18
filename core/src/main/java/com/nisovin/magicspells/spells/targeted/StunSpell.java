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

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class StunSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Map<UUID, StunnedInfo> stunnedLivingEntities;

	private final int interval;
	private final int duration;

	private final Listener listener;

	public StunSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		interval = getConfigInt("interval", 5);
		duration = (int) ((getConfigInt("duration", 200) / 20) * TimeUtil.MILLISECONDS_PER_SECOND);

		listener = new StunListener();
		stunnedLivingEntities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		registerEvents(listener);
		MagicSpells.scheduleRepeatingTask(new StunMonitor(), interval, interval);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();

			stunLivingEntity(caster, target, Math.round(duration * power));
			sendMessages(caster, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		stunLivingEntity(caster, target, Math.round(duration * power));
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		stunLivingEntity(null, target, Math.round(duration * power));
		return true;
	}

	private void stunLivingEntity(LivingEntity caster, LivingEntity target, int duration) {
		StunnedInfo info = new StunnedInfo(caster, target, System.currentTimeMillis() + duration, target.getLocation());
		stunnedLivingEntities.put(target.getUniqueId(), info);

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		playSpellEffectsBuff(target, entity -> {
			if (!(entity instanceof LivingEntity)) return false;
			return isStunned((LivingEntity) entity);
		});

	}

	public boolean isStunned(LivingEntity entity) {
		return stunnedLivingEntities.containsKey(entity.getUniqueId());
	}

	public void removeStun(LivingEntity entity) {
		stunnedLivingEntities.remove(entity.getUniqueId());
	}

	private static class StunnedInfo {

		private final Long until;
		private final LivingEntity caster;
		private final LivingEntity target;
		private final Location targetLocation;

		private StunnedInfo(LivingEntity caster, LivingEntity target, Long until, Location targetLocation) {
			this.caster = caster;
			this.target = target;
			this.until = until;
			this.targetLocation = targetLocation;
		}

	}

	private class StunListener implements Listener {

		@EventHandler
		private void onMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			if (!isStunned(player)) return;
			StunnedInfo info = stunnedLivingEntities.get(player.getUniqueId());
			if (info == null) return;

			if (info.until > System.currentTimeMillis()) {
				event.setTo(info.targetLocation);
				return;
			}

			removeStun(player);
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
				Long until = info.until;
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
