package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ValidTargetList;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class SilenceJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<UUID, Unsilencer> silenced;

	private JutsuFilter filter;

	private String strSilenced;

	private int duration;

	private boolean preventCast;
	private boolean preventChat;
	private boolean preventCommands;
	
	public SilenceJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		strSilenced = getConfigString("str-silenced", "You are silenced!");

		duration = getConfigInt("duration", 200);

		preventCast = getConfigBoolean("prevent-cast", true);
		preventChat = getConfigBoolean("prevent-chat", false);
		preventCommands = getConfigBoolean("prevent-commands", false);

		List<String> allowedJutsuNames = getConfigStringList("allowed-jutsus", null);
		List<String> disallowedJutsuNames = getConfigStringList("disallowed-jutsus", null);
		List<String> tagList = getConfigStringList("allowed-jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("disallowed-jutsu-tags", null);
		filter = new JutsuFilter(allowedJutsuNames, disallowedJutsuNames, tagList, deniedTagList);

		if (preventChat) silenced = new ConcurrentHashMap<>();
		else silenced = new HashMap<>();

		validTargetList = new ValidTargetList(true, false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (preventCast) registerEvents(new CastListener());
		if (preventChat) registerEvents(new ChatListener());
		if (preventCommands) registerEvents(new CommandListener());
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			
			silence(target.getTarget(), target.getPower());
			playJutsuEffects(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		silence(target, power);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		silence(target, power);
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}

	private void silence(LivingEntity target, float power) {
		Unsilencer u = silenced.get(target.getUniqueId());
		if (u != null) u.cancel();
		silenced.put(target.getUniqueId(), new Unsilencer(target, Math.round(duration * power)));
	}

	public boolean isSilenced(LivingEntity target) {
		return silenced.containsKey(target.getUniqueId());
	}

	public void removeSilence(LivingEntity target) {
		if (!isSilenced(target)) return;
		Unsilencer unsilencer = silenced.get(target.getUniqueId());
		unsilencer.cancel();
		silenced.remove(target.getUniqueId());
	}
	
	public class CastListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onJutsuCast(final JutsuCastEvent event) {
			if (event.getCaster() == null) return;
			if (!silenced.containsKey(event.getCaster().getUniqueId())) return;
			if (filter.check(event.getJutsu())) return;
			event.setCancelled(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicJutsus.plugin, () -> sendMessage(strSilenced, event.getCaster(), event.getJutsuArgs()));
		}
		
	}
	
	public class ChatListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onChat(AsyncPlayerChatEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			sendMessage(strSilenced, event.getPlayer(), MagicJutsus.NULL_ARGS);
		}
		
	}
	
	public class CommandListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onCommand(PlayerCommandPreprocessEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			sendMessage(strSilenced, event.getPlayer(), MagicJutsus.NULL_ARGS);
		}
		
	}
	
	private class Unsilencer implements Runnable {

		private UUID id;
		private int taskId;
		private boolean canceled = false;

		private Unsilencer(LivingEntity livingEntity, int delay) {
			id = livingEntity.getUniqueId();
			taskId = MagicJutsus.scheduleDelayedTask(this, delay);
		}
		
		@Override
		public void run() {
			if (!canceled) silenced.remove(id);
		}

		private void cancel() {
			canceled = true;
			if (taskId > 0) MagicJutsus.cancelTask(taskId);
		}
		
	}

}
