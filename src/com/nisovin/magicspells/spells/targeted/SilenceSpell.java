package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nisovin.magicspells.util.SpellFilter;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.ValidTargetList;

public class SilenceSpell extends TargetedSpell implements TargetedEntitySpell {

	private boolean preventCast;
	private boolean preventChat;
	private boolean preventCommands;
	private int duration;
	private List<String> allowedSpellNames;
	private List<String> disallowedSpellNames;
	private String strSilenced;
	
	private SpellFilter shouldAllow = null;
	
	Map<String,Unsilencer> silenced;
	
	public SilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		preventCast = getConfigBoolean("prevent-cast", true);
		preventChat = getConfigBoolean("prevent-chat", false);
		preventCommands = getConfigBoolean("prevent-commands", false);
		duration = getConfigInt("duration", 200);
		allowedSpellNames = getConfigStringList("allowed-spells", null);
		disallowedSpellNames = getConfigStringList("disallowed-spells", null);
		List<String> tagList = getConfigStringList("allowed-spell-tags", null);
		List<String> deniedTagList = getConfigStringList("disallowed-spell-tags", null);
		strSilenced = getConfigString("str-silenced", "You are silenced!");
		
		if (preventChat) {
			silenced = new ConcurrentHashMap<>();
		} else {
			silenced = new HashMap<>();
		}
		
		validTargetList = new ValidTargetList(true, false);
		
		this.shouldAllow = new SpellFilter(allowedSpellNames, disallowedSpellNames, tagList, deniedTagList);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (preventCast) registerEvents(new CastListener());
		if (preventChat) registerEvents(new ChatListener());
		if (preventCommands) registerEvents(new CommandListener());
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> target = getTargetedPlayer(player, power);
			if (target == null) return noTarget(player);
			
			// Silence player
			silence(target.getTarget(), target.getPower());
			playSpellEffects(player, target.getTarget());
			
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void silence(Player player, float power) {
		// Handle previous silence
		String playerName = player.getName();
		Unsilencer u = silenced.get(playerName);
		if (u != null) u.cancel();
		
		// Silence now
		silenced.put(playerName, new Unsilencer(player, Math.round(duration * power)));
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		silence((Player)target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		silence((Player)target, power);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	public class CastListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onSpellCast(final SpellCastEvent event) {
			if (event.getCaster() == null) return;
			if (!silenced.containsKey(event.getCaster().getName())) return;
			if (shouldAllow.check(event.getSpell())) return;
			event.setCancelled(true);
			
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> sendMessage(strSilenced, event.getCaster(), event.getSpellArgs()));
		}
		
	}
	
	public class ChatListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onChat(AsyncPlayerChatEvent event) {
			if (!silenced.containsKey(event.getPlayer().getName())) return;
			event.setCancelled(true);
			sendMessage(strSilenced, event.getPlayer(), MagicSpells.NULL_ARGS);
		}
		
	}
	
	public class CommandListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onCommand(PlayerCommandPreprocessEvent event) {
			if (!silenced.containsKey(event.getPlayer().getName())) return;
			event.setCancelled(true);
			sendMessage(strSilenced, event.getPlayer(), MagicSpells.NULL_ARGS);
		}
		
	}
	
	public class Unsilencer implements Runnable {

		private String playerName;
		private boolean canceled = false;
		private int taskId = -1;
		
		public Unsilencer(Player player, int delay) {
			this.playerName = player.getName();
			taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, this, delay);
		}
		
		@Override
		public void run() {
			if (!canceled) silenced.remove(playerName);
		}
		
		public void cancel() {
			canceled = true;
			if (taskId > 0) Bukkit.getScheduler().cancelTask(taskId);
		}
		
	}

}
