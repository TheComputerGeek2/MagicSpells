package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class StunSpell extends TargetedSpell implements TargetedEntitySpell {

	int duration;
	
	Map<String, Long> stunnedPlayersUntil;
	Map<String, Location> stunnedPlayersLocation;
	Map<LivingEntity, Long> stunnedEntitiesUntil;
	Map<LivingEntity, Location> stunnedEntitiesLocation;
	
	Listener listener = null;
	int taskId = -1;
	
	public StunSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		duration = (int)(getConfigFloat("duration", 10) * TimeUtil.MILLISECONDS_PER_SECOND);
		
		stunnedPlayersUntil = new HashMap<>();
		stunnedPlayersLocation = new HashMap<>();
		stunnedEntitiesUntil = new HashMap<>();
		stunnedEntitiesLocation = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) return noTarget(player);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			if (target instanceof Player) {
				stunPlayer(player, (Player)target, Math.round(duration * power));
			} else {
				stunEntity(player, target, Math.round(duration * power));
			}
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public boolean isStunned(Player player) {
		return stunnedPlayersUntil.containsKey(player.getName());
	}

	public boolean isStunned(LivingEntity entity) {
		return stunnedEntitiesUntil.containsKey(entity);
	}

	public void cancelStun(Player player) {
		if (listener == null) return;
		StunListener listener = (StunListener) this.listener;
		listener.removePlayer(player.getName());
		playSpellEffects(EffectPosition.CLEANSED, player);
	}

	public void cancelStun(LivingEntity entity) {
		stunnedEntitiesUntil.remove(entity);
		stunnedEntitiesLocation.remove(entity);
		playSpellEffects(EffectPosition.CLEANSED, entity);
	}
	
	void stunPlayer(Player caster, Player target, int duration) {
		stunnedPlayersUntil.put(target.getName(), System.currentTimeMillis() + duration);
		stunnedPlayersLocation.put(target.getName(), target.getLocation());
		if (listener == null) {
			listener = new StunListener();
			registerEvents(listener);
		}
		if (caster != null) {
			playSpellEffects(caster, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		playSpellEffectsBuff(target, (Entity entity) -> stunnedPlayersUntil.containsKey(entity.getName()));
	}
	
	void stunEntity(Player caster, LivingEntity target, int duration) {
		stunnedEntitiesUntil.put(target, System.currentTimeMillis() + duration);
		stunnedEntitiesLocation.put(target, target.getLocation());
		if (taskId < 0) {
			taskId = MagicSpells.scheduleRepeatingTask(new StunMonitor(), 5, 5);
		}
		if (caster != null) {
			playSpellEffects(caster, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		playSpellEffectsBuff(target, (Entity entity) -> stunnedEntitiesUntil.containsKey(entity));
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		if (target instanceof Player) {
			stunPlayer(caster, (Player)target, Math.round(duration * power));
		} else {
			stunEntity(caster, target, Math.round(duration * power));
		}
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		if (target instanceof Player) {
			stunPlayer(null, (Player)target, Math.round(duration * power));
		} else {
			stunEntity(null, target, Math.round(duration * power));
		}
		return true;
	}
	
	class StunListener implements Listener {
		
		@EventHandler
		public void onMove(PlayerMoveEvent event) {
			String playerName = event.getPlayer().getName();
			Long until = stunnedPlayersUntil.get(playerName);
			if (until == null) return;
			
			if (until > System.currentTimeMillis()) {
				event.setTo(stunnedPlayersLocation.get(playerName));
			} else {
				removePlayer(playerName);
			}
		}
		
		@EventHandler
		public void onInteract(PlayerInteractEvent event) {
			if (!stunnedPlayersUntil.containsKey(event.getPlayer().getName())) return;
			event.setCancelled(true);
		}
		
		@EventHandler
		public void onQuit(PlayerQuitEvent event) {
			String playerName = event.getPlayer().getName();
			if (!stunnedPlayersUntil.containsKey(playerName)) return;
			removePlayer(playerName);
		}
		
		@EventHandler
		public void onDeath(PlayerDeathEvent event) {
			String playerName = event.getEntity().getName();
			if (!stunnedPlayersUntil.containsKey(playerName)) return;
			removePlayer(playerName);
		}
		
		void removePlayer(String playerName) {
			stunnedPlayersUntil.remove(playerName);
			stunnedPlayersLocation.remove(playerName);
			if (!stunnedPlayersUntil.isEmpty()) return;
			unregisterEvents(this);
			listener = null;
		}
		
	}
	
	class StunMonitor implements Runnable {
		
		@Override
		public void run() {
			Iterator<Map.Entry<LivingEntity, Long>> iter = stunnedEntitiesUntil.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<LivingEntity, Long> entry = iter.next();
				if (entry.getKey().isValid() && entry.getValue() > System.currentTimeMillis()) {
					entry.getKey().teleport(stunnedEntitiesLocation.get(entry.getKey()));
				} else {
					iter.remove();
					stunnedEntitiesLocation.remove(entry.getKey());
				}
			}
			if (stunnedEntitiesUntil.isEmpty()) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}
		
	}

}
