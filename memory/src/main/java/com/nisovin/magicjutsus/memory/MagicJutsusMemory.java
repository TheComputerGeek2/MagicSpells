package com.nisovin.magicjutsus.memory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.nisovin.magicjutsus.util.compat.EventUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.util.MagicConfig;

// TODO should it be meaningful to define negative memory?
// TODO For example, learn a jutsu to free up space (such as a negative passive)
public class MagicJutsusMemory extends JavaPlugin {

	private int maxMemoryDefault = 0;
	private ArrayList<String> maxMemoryPerms = new ArrayList<>();
	private ArrayList<Integer> maxMemoryAmounts = new ArrayList<>();
	
	protected String strOutOfMemory = "";
	private String strMemoryUsage = "";
	
	private HashMap<String,Integer> memoryRequirements = new HashMap<>();

	@Override
	public void onEnable() {
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) saveDefaultConfig();
		Configuration config = getConfig();
		
		this.strOutOfMemory = config.getString("str-out-of-memory");
		this.strMemoryUsage = config.getString("str-memory-usage");
		
		// Get max memory amounts
		this.maxMemoryDefault = config.getInt("max-memory-default");
		ConfigurationSection permsSec = config.getConfigurationSection("max-memory-perms");
		if (permsSec != null) {
			Set<String> perms = permsSec.getKeys(false);
			if (perms != null) {
				for (String perm : perms) {
					this.maxMemoryPerms.add(perm);
					this.maxMemoryAmounts.add(permsSec.getInt(perm));
				}
			}
		}
		
		// Get jutsu mem requirements from mem config
		ConfigurationSection reqSec = config.getConfigurationSection("memory-requirements");
		if (reqSec != null) {
			MagicJutsus.log("You should move your MagicJutsusMemory memory");
			MagicJutsus.log("requirements to the main MagicJutsus config file.");
			MagicJutsus.log("You can add a memory option to each jutsu in the");
			MagicJutsus.log("main MagicJutsus config file.");
			Set<String> jutsus = reqSec.getKeys(false);
			if (jutsus != null) {
				for (String jutsu : jutsus) {
					int mem = reqSec.getInt(jutsu);
					this.memoryRequirements.put(jutsu, mem);
					MagicJutsus.debug("Memory requirement for '" + jutsu + "' jutsu set to " + mem);
				}
			}
		}
		
		// Get jutsu mem requirements from magicjutsus config
		MagicConfig magicConfig = new MagicConfig(new File(MagicJutsus.plugin.getDataFolder(), "config.yml"));
		if (magicConfig.isLoaded()) {
			for (String jutsu : magicConfig.getJutsuKeys()) {
				if (!magicConfig.contains("jutsus." + jutsu + ".memory")) continue;;
				int mem = magicConfig.getInt("jutsus." + jutsu + ".memory", 0);
				this.memoryRequirements.put(jutsu, mem);
				MagicJutsus.debug("Memory requirement for '" + jutsu + "' jutsu set to " + mem);
			}
		}
		
		EventUtil.register(new MemoryJutsuListener(this), this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (args.length == 1 && sender.hasPermission("magicjutsus.memory.checkothers")) {
			List<Player> p = getServer().matchPlayer(args[0]);
			if (p.size() == 1) player = p.get(0);
		} else if (sender instanceof Player) {
			player = (Player)sender;
		}
		
		if (player != null) {
			int used = getUsedMemory(player);
			int max = getMaxMemory(player);
			if (sender instanceof Player) {
				MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strMemoryUsage, "%memory", used + "", "%total", max + ""), (Player)sender, (String[])null);
			} else {
				String s = this.strMemoryUsage.replace("%memory", used + "").replace("%total", max + "");
				sender.sendMessage(s);
			}
		}
		return true;
	}



	public int getRequiredMemory(Jutsu jutsu) {
		// FIXME this should be a get or default type thing
		if (this.memoryRequirements.containsKey(jutsu.getInternalName())) return this.memoryRequirements.get(jutsu.getInternalName());
		return 0;
	}
	
	public int getMemoryRemaining(Player player) {
		int max = getMaxMemory(player);
		int used = getUsedMemory(player);
		return max - used;
	}
	
	public int getMaxMemory(Player player) {
		for (int i = 0; i < this.maxMemoryPerms.size(); i++) {
			if (player.hasPermission("magicjutsus.rank." + this.maxMemoryPerms.get(i))) {
				return this.maxMemoryAmounts.get(i);
			}
		}
		return this.maxMemoryDefault;
	}
	
	public int getUsedMemory(Player player) {
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		if (jutsubook != null) {
			int used = 0;
			for (Jutsu jutsu : jutsubook.getJutsus()) {
				if (this.memoryRequirements.containsKey(jutsu.getInternalName())) {
					used += this.memoryRequirements.get(jutsu.getInternalName());
				}
			}
			return used;
		}
		return 0;
	}
	
	@Override
	public void onDisable() {
	}

}
