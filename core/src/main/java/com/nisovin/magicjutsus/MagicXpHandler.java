package com.nisovin.magicjutsus;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.text.NumberFormat;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.IntMap;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.Jutsu.PostCastAction;
import com.nisovin.magicjutsus.Jutsu.JutsuCastState;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuLearnEvent.LearnSource;

public class MagicXpHandler implements Listener {

	private MagicJutsus plugin;

	private Map<String, String> schools = new HashMap<>();
	private Map<String, IntMap<String>> xp = new HashMap<>();
	private Map<String, String> currentWorld = new HashMap<>();
	private Map<String, List<Jutsu>> jutsuSchoolRequirements = new HashMap<>();

	private Set<String> dirty = new HashSet<>();

	private boolean autoLearn;
	private String strXpHeader;
	private String strNoXp;
	
	MagicXpHandler(MagicJutsus plugin, MagicConfig config) {
		this.plugin = plugin;
		
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
		
		for (Jutsu jutsu : MagicJutsus.jutsus()) {
			Map<String, Integer> xpRequired = jutsu.getXpRequired();
			if (xpRequired == null) continue;
			for (String school : xpRequired.keySet()) {
				List<Jutsu> list = jutsuSchoolRequirements.computeIfAbsent(school.toLowerCase(), s -> new ArrayList<>());
				list.add(jutsu);
			}
		}
		Util.forEachPlayerOnline(this::load);
		MagicJutsus.scheduleRepeatingTask(this::saveAll, TimeUtil.TICKS_PER_MINUTE, TimeUtil.TICKS_PER_MINUTE);
		MagicJutsus.registerEvents(this);
	}
	
	public void showXpInfo(Player player) {
		MagicJutsus.sendMessage(strXpHeader, player, MagicJutsus.NULL_ARGS);
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp == null || playerXp.isEmpty()) {
			MagicJutsus.sendMessage(strNoXp, player, MagicJutsus.NULL_ARGS);
			return;
		}
		for (String school : playerXp.keySet()) {
			String schoolName = schools.get(school);
			if (schoolName == null) continue;
			String amt = NumberFormat.getInstance().format(playerXp.get(school));
			MagicJutsus.sendMessage(schoolName + ": " + amt, player, MagicJutsus.NULL_ARGS);
		}
	}
	
	public int getXp(Player player, String school) {
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp != null) return playerXp.get(school.toLowerCase());
		return 0;
	}
	
	@EventHandler
	public void onCast(JutsuCastedEvent event) {
		if (event.getPostCastAction() == PostCastAction.ALREADY_HANDLED) return;
		if (event.getJutsuCastState() != JutsuCastState.NORMAL) return;
		
		final Map<String, Integer> xpGranted = event.getJutsu().getXpGranted();
		if (xpGranted == null) return;

		// Get player xp
		IntMap<String> playerXp = xp.computeIfAbsent(event.getCaster().getName(), s -> new IntMap<>());
		
		// Grant xp
		// FIXME use entry set here
		for (String school : xpGranted.keySet()) {
			playerXp.increment(school.toLowerCase(), xpGranted.get(school));
		}

		dirty.add(event.getCaster().getName());

		if (!autoLearn) return;
		final LivingEntity caster = event.getCaster();
		if (!(caster instanceof Player)) return;

		Player player = (Player) caster;
		final Jutsu castedJutsu = event.getJutsu();
		MagicJutsus.scheduleDelayedTask(() -> {

			// Get jutsus to check if learned
			Set<Jutsu> toCheck = new HashSet<>();
			for (String school : xpGranted.keySet()) {
				List<Jutsu> list = jutsuSchoolRequirements.get(school.toLowerCase());
				if (list != null) toCheck.addAll(list);
			}

			// Check for new learned jutsus
			if (toCheck.isEmpty()) return;
			boolean learned = false;
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			for (Jutsu jutsu : toCheck) {
				if (!jutsubook.hasJutsu(jutsu, false) && jutsubook.canLearn(jutsu)) {
					JutsuLearnEvent evt = new JutsuLearnEvent(jutsu, player, LearnSource.MAGIC_XP, castedJutsu);
					EventUtil.call(evt);
					if (!evt.isCancelled()) {
						jutsubook.addJutsu(jutsu);
						MagicJutsus.sendMessage(jutsu.getStrXpLearned(), player, MagicJutsus.NULL_ARGS);
						learned = true;
					}
				}
			}
			if (learned) jutsubook.save();

		}, 1);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		currentWorld.put(event.getPlayer().getName(), event.getPlayer().getWorld().getName());
		dirty.remove(event.getPlayer().getName());
		load(event.getPlayer());
	}

	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (!plugin.separatePlayerJutsusPerWorld) return;
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (dirty.contains(playerName)) save(player);
		currentWorld.put(playerName, player.getWorld().getName());
		load(player);
		dirty.remove(playerName);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (dirty.contains(playerName)) save(player);
		xp.remove(playerName);
		dirty.remove(playerName);
		currentWorld.remove(playerName);
	}
	
	public void load(Player player) {
		File folder = new File(plugin.getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();
		if (plugin.separatePlayerJutsusPerWorld) {
			String world = currentWorld.get(player.getName());
			if (world == null) world = player.getWorld().getName();
			folder = new File(folder, world);
			if (!folder.exists()) folder.mkdirs();
		}
		String uuid = Util.getUniqueId(player);
		File file = new File(folder, uuid + ".txt");
		if (!file.exists()) {
			File file2 = new File(folder, player.getName().toLowerCase());
			if (file2.exists()) file2.renameTo(file);
		}
		if (!file.exists()) return;
		YamlConfiguration conf = new YamlConfiguration();
		try {
			conf.load(file);
			IntMap<String> playerXp = new IntMap<>();
			for (String school : conf.getKeys(false)) {
				playerXp.put(school.toLowerCase(), conf.getInt(school, 0));
			}
			xp.put(player.getName(), playerXp);
		} catch (Exception e) {
			MagicJutsus.error("Error while loading player XP for player " + player.getName());
			MagicJutsus.handleException(e);
		}
	}
	
	public void saveAll() {
		for (String playerName : dirty) {
			Player player = PlayerNameUtils.getPlayerExact(playerName);
			if (player != null) save(player);
		}
		dirty.clear();
	}
	
	public void save(Player player) {
		String world = currentWorld.get(player.getName());
		if (world == null) world = player.getWorld().getName();
		File folder = new File(plugin.getDataFolder(), "xp");
		if (!folder.exists()) folder.mkdirs();
		if (plugin.separatePlayerJutsusPerWorld) {
			if (world == null) return;
			folder = new File(folder, world);
			if (!folder.exists()) folder.mkdirs();
		}
		File file = new File(folder, Util.getUniqueId(player) + ".txt");
		if (file.exists()) file.delete();
		
		YamlConfiguration conf = new YamlConfiguration();
		IntMap<String> playerXp = xp.get(player.getName());
		if (playerXp != null) {
			for (String school : playerXp.keySet()) {
				conf.set(school.toLowerCase(), playerXp.get(school));
			}
		}

		try {
			conf.save(file);
		} catch (Exception e) {
			MagicJutsus.error("Error while saving player XP for player " + player);
			MagicJutsus.handleException(e);
		}
	}
	
}
