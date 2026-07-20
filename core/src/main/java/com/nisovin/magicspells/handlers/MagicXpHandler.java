package com.nisovin.magicspells.handlers;

import java.io.File;
import java.util.*;
import java.text.NumberFormat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

public class MagicXpHandler implements Listener {

	private final Map<String, String> schools = new HashMap<>();
	private final Map<UUID, Object2IntMap<String>> xp = new HashMap<>();
	private final Map<String, List<Spell>> spellSchoolRequirements = new HashMap<>();

	private final Set<UUID> dirty = new HashSet<>();

	private final boolean autoLearn;
	private final String strXpHeader;
	private final String strNoXp;

	public MagicXpHandler(MagicConfig config) {
		Set<String> keys = config.getKeys("general.magic-schools");
		if (keys != null) {
			for (String school : keys) {
				String name = config.getString("general.magic-schools." + school, null);
				if (name != null) schools.put(school.toLowerCase(), name);
			}
		}

		autoLearn = config.getBoolean("general.magic-xp-auto-learn", false);
		strXpHeader = config.getString("general.str-xp-header", null);
		strNoXp = config.getString("general.str-no-xp", null);

		for (Spell spell : MagicSpells.spells()) {
			Map<String, Integer> xpRequired = spell.getXpRequired();
			if (xpRequired == null) continue;
			for (String school : xpRequired.keySet()) {
				List<Spell> list = spellSchoolRequirements.computeIfAbsent(school.toLowerCase(), _ -> new ArrayList<>());
				list.add(spell);
			}
		}

		Util.forEachPlayerOnline(this::load);
		MagicSpells.scheduleRepeatingTask(this::saveAll, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
		MagicSpells.registerEvents(this);
	}

	public void showXpInfo(Player player) {
		MagicSpells.sendMessage(player, strXpHeader);
		Object2IntMap<String> playerXp = xp.get(player.getUniqueId());

		if (playerXp == null || playerXp.isEmpty()) {
			MagicSpells.sendMessage(player, strNoXp);
			return;
		}

		for (String school : playerXp.keySet()) {
			String schoolName = schools.get(school);
			if (schoolName == null) continue;

			String amt = NumberFormat.getInstance().format(playerXp.getInt(school));
			MagicSpells.sendMessage(player, schoolName + ": " + amt);
		}
	}

	public int getXp(Player player, String school) {
		Object2IntMap<String> playerXp = xp.get(player.getUniqueId());
		return playerXp == null ? 0 : playerXp.getInt(school.toLowerCase());
	}

	@EventHandler
	public void onCast(SpellCastedEvent event) {
		if (event.getPostCastAction() == PostCastAction.ALREADY_HANDLED) return;
		if (event.getSpellCastState() != SpellCastState.NORMAL) return;

		final Map<String, Integer> xpGranted = event.getSpell().getXpGranted();
		if (xpGranted == null) return;

		Object2IntMap<String> playerXp = xp.computeIfAbsent(event.getCaster().getUniqueId(), _ -> new Object2IntOpenHashMap<>());
		xpGranted.forEach((key, value) -> playerXp.mergeInt(key, value, Integer::sum));

		dirty.add(event.getCaster().getUniqueId());

		if (!autoLearn) return;
		if (!(event.getCaster() instanceof Player player)) return;

		MagicSpells.scheduleDelayedTask(() -> {
			Set<Spell> toCheck = new HashSet<>();
			for (String school : xpGranted.keySet()) {
				List<Spell> list = spellSchoolRequirements.get(school.toLowerCase());
				if (list != null) toCheck.addAll(list);
			}
			if (toCheck.isEmpty()) return;

			boolean learned = false;
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (Spell spell : toCheck) {
				if (spellbook.hasSpell(spell, false) || !spellbook.canLearn(spell)) continue;
				if (!new SpellLearnEvent(spell, player, LearnSource.MAGIC_XP, event.getSpell()).callEvent()) continue;

				spellbook.addSpell(spell);
				MagicSpells.sendMessage(player, spell.getStrXpLearned());
				learned = true;
			}
			if (learned) spellbook.save();

		}, 1);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		dirty.remove(player.getUniqueId());
		load(player);
	}

	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (!MagicSpells.arePlayerSpellsSeparatedPerWorld()) return;

		Player player = event.getPlayer();
		if (dirty.remove(player.getUniqueId())) save(player);
		load(player);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		if (dirty.remove(uuid)) save(player);
		xp.remove(uuid);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void load(Player player) {
		File folder = new File(MagicSpells.getInstance().getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();

		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
			folder = new File(folder, player.getWorld().getName());
			if (!folder.exists()) folder.mkdirs();
		}

		File file = new File(folder, Util.getUniqueId(player) + ".txt");
		if (!file.exists()) {
			File file2 = new File(folder, player.getName().toLowerCase());
			if (file2.exists()) file2.renameTo(file);
		}
		if (!file.exists()) return;

		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.load(file);
			Object2IntMap<String> playerXp = new Object2IntOpenHashMap<>();
			for (String school : conf.getKeys(false)) {
				playerXp.put(school.toLowerCase(), conf.getInt(school, 0));
			}
			xp.put(player.getUniqueId(), playerXp);
		} catch (Exception e) {
			MagicSpells.error("Error while loading player XP for player " + player.getName());
			MagicSpells.handleException(e);
		}
	}

	public void saveAll() {
		for (UUID uuid : dirty) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null) continue;
			save(player);
		}
		dirty.clear();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void save(Player player) {
		File folder = new File(MagicSpells.getInstance().getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();

		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
			folder = new File(folder, player.getWorld().getName());
			if (!folder.exists()) folder.mkdirs();
		}

		File file = new File(folder, Util.getUniqueId(player) + ".txt");
		if (file.exists()) file.delete();

		YamlConfiguration conf = new YamlConfiguration();
		Object2IntMap<String> playerXp = xp.get(player.getUniqueId());
		if (playerXp != null) {
			for (String school : playerXp.keySet()) {
				conf.set(school.toLowerCase(), playerXp.getInt(school));
			}
		}

		try {
			conf.save(file);
		} catch (Exception e) {
			MagicSpells.error("Error while saving player XP for player " + player);
			MagicSpells.handleException(e);
		}
	}

}
