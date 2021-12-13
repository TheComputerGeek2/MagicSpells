package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class SilenceSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, Unsilencer> silenced;

	private SpellFilter filter;

	private String strSilenced;

	private int duration;

	private boolean preventCast;
	private boolean preventChat;
	private boolean preventCommands;

	private final String preventCastSpellName;
	private final String preventChatSpellName;
	private final String preventCommandSpellName;

	private Subspell preventCastSpell;
	private Subspell preventChatSpell;
	private Subspell preventCommandSpell;
	
	public SilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strSilenced = getConfigString("str-silenced", "You are silenced!");

		duration = getConfigInt("duration", 200);

		preventCast = getConfigBoolean("prevent-cast", true);
		preventChat = getConfigBoolean("prevent-chat", false);
		preventCommands = getConfigBoolean("prevent-commands", false);

		preventCastSpellName = getConfigString("spell-on-denied-cast", "");
		preventChatSpellName = getConfigString("spell-on-denied-chat", "");
		preventCommandSpellName = getConfigString("spell-on-denied-command", "");

		List<String> allowedSpellNames = getConfigStringList("allowed-spells", null);
		List<String> disallowedSpellNames = getConfigStringList("disallowed-spells", null);
		List<String> tagList = getConfigStringList("allowed-spell-tags", null);
		List<String> deniedTagList = getConfigStringList("disallowed-spell-tags", null);
		filter = new SpellFilter(allowedSpellNames, disallowedSpellNames, tagList, deniedTagList);

		if (preventChat) silenced = new ConcurrentHashMap<>();
		else silenced = new HashMap<>();

		validTargetList = new ValidTargetList(true, false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (preventCast) {
			preventCastSpell = initSubspell(preventCastSpellName, "SilenceSpell '" + internalName + "' has an invalid spell-on-denied-cast defined.");
			registerEvents(new CastListener());
		}
		if (preventChat) {
			preventChatSpell = initSubspell(preventChatSpellName, "SilenceSpell '" + internalName + "' has an invalid spell-on-denied-chat defined.");
			registerEvents(new ChatListener());
		}
		if (preventCommands) {
			preventCommandSpell = initSubspell(preventCommandSpellName, "SilenceSpell '" + internalName + "' has an invalid spell-on-denied-command defined.");
			registerEvents(new CommandListener());
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			
			silence(caster, target.getTarget(), target.getPower(), args);
			playSpellEffects(caster, target.getTarget());
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		silence(caster, target, power, args);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		silence(null, target, power, args);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private void silence(LivingEntity caster, LivingEntity target, float power, String[] args) {
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
		public void onSpellCast(final SpellCastEvent event) {
			if (event.getCaster() == null) return;
			if (!silenced.containsKey(event.getCaster().getUniqueId())) return;
			if (filter.check(event.getSpell())) return;
			event.setCancelled(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> {
				if (preventCastSpell != null) preventCastSpell.cast(event.getCaster(), 1);
				sendMessage(strSilenced, event.getCaster(), event.getSpellArgs());
			});
		}
		
	}
	
	public class ChatListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		public void onChat(AsyncChatEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			if (preventChatSpell != null) preventChatSpell.cast(event.getPlayer(), 1);
			sendMessage(strSilenced, event.getPlayer(), MagicSpells.NULL_ARGS);
		}
		
	}
	
	public class CommandListener implements Listener {
		
		@EventHandler(ignoreCancelled=true)
		public void onCommand(PlayerCommandPreprocessEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			if (preventCommandSpell != null) preventCommandSpell.cast(event.getPlayer(), 1);
			sendMessage(strSilenced, event.getPlayer(), MagicSpells.NULL_ARGS);
		}
		
	}
	
	private class Unsilencer implements Runnable {

		private UUID id;
		private int taskId;
		private boolean canceled = false;

		private Unsilencer(LivingEntity livingEntity, int delay) {
			id = livingEntity.getUniqueId();
			taskId = MagicSpells.scheduleDelayedTask(this, delay);
		}
		
		@Override
		public void run() {
			if (!canceled) silenced.remove(id);
		}

		private void cancel() {
			canceled = true;
			if (taskId > 0) MagicSpells.cancelTask(taskId);
		}
		
	}

}
