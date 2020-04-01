package com.nisovin.magicjutsus.jutsus.targeted;

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

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class StunJutsu extends TargetedJutsu implements TargetedEntityJutsu {
	
	private Map<UUID, StunnedInfo> stunnedLivingEntities;

	private int taskId = -1;
	private int interval;
	private int duration;

	private Listener listener;
	
	public StunJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		interval = getConfigInt("interval", 5);
		duration = (int) ((getConfigInt("duration", 200) / 20) * TimeUtil.MILLISECONDS_PER_SECOND);

		listener = new StunListener();
		stunnedLivingEntities = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		registerEvents(listener);
		taskId = MagicJutsus.scheduleRepeatingTask(new StunMonitor(), interval, interval);
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
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
		
		if (caster != null) playJutsuEffects(caster, target);
		else playJutsuEffects(EffectPosition.TARGET, target);
		
		playJutsuEffectsBuff(target, entity -> {
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
		
		private Long until;
		private LivingEntity caster;
		private LivingEntity target;
		private Location targetLocation;
		
		private StunnedInfo(LivingEntity caster, LivingEntity target, Long until, Location targetLocation) {
			this.caster = caster;
			this.target = target;
			this.until = until;
			this.targetLocation = targetLocation;
		}
		
	}
	
	private class StunListener implements Listener {
		
		@EventHandler
		private void onMove(PlayerMoveEvent e) {
			Player pl = e.getPlayer();
			if (!isStunned(pl)) return;
			StunnedInfo info = stunnedLivingEntities.get(pl.getUniqueId());
			if (info == null) return;
			
			if (info.until > System.currentTimeMillis()) {
				e.setTo(info.targetLocation);
				return;
			}
			
			removeStun(pl);
		}
		
		@EventHandler
		private void onInteract(PlayerInteractEvent e) {
			if (!isStunned(e.getPlayer())) return;
			e.setCancelled(true);
		}
		
		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			Player pl = e.getPlayer();
			if (!isStunned(pl)) return;
			removeStun(pl);
		}
		
		@EventHandler
		private void onDeath(PlayerDeathEvent e) {
			Player pl = e.getEntity();
			if (!isStunned(pl)) return;
			removeStun(pl);
		}
		
	}
	
	private class StunMonitor implements Runnable {

		private Set<LivingEntity> toRemove = new HashSet<>();

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
