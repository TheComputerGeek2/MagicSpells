package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.papermc.paper.event.player.AsyncChatEvent;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SilenceSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Map<UUID, Unsilencer> silenced;

	private final String strSilenced;

	private final ConfigData<Integer> duration;

	private final boolean preventCast;
	private final boolean preventChat;
	private final boolean preventCommands;
	private final boolean notifyHelperSpells;
	private final boolean notifyPassiveSpells;
	private final ConfigData<Boolean> powerAffectsDuration;

	private final String preventCastSpellName;
	private final String preventChatSpellName;
	private final String preventCommandSpellName;

	private SpellFilter filter;

	private Subspell preventCastSpell;
	private Subspell preventChatSpell;
	private Subspell preventCommandSpell;

	public SilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strSilenced = getConfigString("str-silenced", "You are silenced!");

		duration = getConfigDataInt("duration", 200);

		preventCast = getConfigBoolean("prevent-cast", true);
		preventChat = getConfigBoolean("prevent-chat", false);
		preventCommands = getConfigBoolean("prevent-commands", false);
		notifyHelperSpells = getConfigBoolean("notify-helper-spells", true);
		notifyPassiveSpells = getConfigBoolean("notify-passive-spells", true);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);

		preventCastSpellName = getConfigString("spell-on-denied-cast", "");
		preventChatSpellName = getConfigString("spell-on-denied-chat", "");
		preventCommandSpellName = getConfigString("spell-on-denied-command", "");

		if (preventChat) silenced = new ConcurrentHashMap<>();
		else silenced = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "SilenceSpell '" + internalName + "' has an invalid '%s' defined!";
		if (preventCast) {
			preventCastSpell = initSubspell(preventCastSpellName,
					error.formatted("spell-on-denied-cast"),
					true);
			registerEvents(new CastListener());
		}
		if (preventChat) {
			preventChatSpell = initSubspell(preventChatSpellName,
					error.formatted("spell-on-denied-chat"),
					true);
			registerEvents(new ChatListener());
		}
		if (preventCommands) {
			preventCommandSpell = initSubspell(preventCommandSpellName,
					error.formatted("spell-on-denied-command"),
					true);
			registerEvents(new CommandListener());
		}

		filter = SpellFilter.fromLegacySection(config.getMainConfig(), internalKey);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		UUID uuid = data.target().getUniqueId();

		Unsilencer unsilencer = silenced.get(uuid);
		if (unsilencer != null) unsilencer.cancel();

		int duration = this.duration.get(data);
		if (powerAffectsDuration.get(data)) duration = Math.round(duration * data.power());

		silenced.put(uuid, new Unsilencer(uuid, duration));
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
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

		@EventHandler(ignoreCancelled = true)
		public void onSpellCast(final SpellCastEvent event) {
			if (event.getCaster() == null) return;
			if (!silenced.containsKey(event.getCaster().getUniqueId())) return;
			Spell spell = event.getSpell();
			if (filter.check(spell)) return;
			event.setCancelled(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> {
				if (preventCastSpell != null) preventCastSpell.subcast(event.getSpellData().noTargeting());
				if (spell.isHelperSpell() && !notifyHelperSpells) return;
				if (spell instanceof PassiveSpell && !notifyPassiveSpells) return;
				sendMessage(strSilenced, event.getCaster(), event.getSpellData());
			});
		}

	}

	public class ChatListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		public void onChat(AsyncChatEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			if (preventChatSpell != null) preventChatSpell.subcast(new SpellData(event.getPlayer()));
			sendMessage(strSilenced, event.getPlayer(), SpellData.NULL);
		}

	}

	public class CommandListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		public void onCommand(PlayerCommandPreprocessEvent event) {
			if (!silenced.containsKey(event.getPlayer().getUniqueId())) return;
			event.setCancelled(true);
			if (preventCommandSpell != null) preventCommandSpell.subcast(new SpellData(event.getPlayer()));
			sendMessage(strSilenced, event.getPlayer(), SpellData.NULL);
		}

	}

	private class Unsilencer implements Runnable {

		private final UUID uuid;
		private final ScheduledTask task;
		private boolean canceled = false;

		private Unsilencer(UUID uuid, int delay) {
			this.uuid = uuid;
			task = MagicSpells.scheduleDelayedTask(this, delay);
		}

		@Override
		public void run() {
			if (!canceled) silenced.remove(uuid);
		}

		private void cancel() {
			canceled = true;
			if (!task.isCancelled()) MagicSpells.cancelTask(task);
		}

	}

}
