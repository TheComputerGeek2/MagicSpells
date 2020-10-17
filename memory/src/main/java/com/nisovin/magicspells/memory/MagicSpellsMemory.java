package com.nisovin.magicspells.memory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.nisovin.magicspells.util.compat.EventUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.MagicConfig;

// TODO should it be meaningful to define negative memory?
// TODO For example, learn a spell to free up space (such as a negative passive)
public class MagicSpellsMemory extends JavaPlugin {

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
		
		// Get spell mem requirements from mem config
		ConfigurationSection reqSec = config.getConfigurationSection("memory-requirements");
		if (reqSec != null) {
			MagicSpells.log("You should move your MagicSpellsMemory memory");
			MagicSpells.log("requirements to the main MagicSpells config file.");
			MagicSpells.log("You can add a memory option to each spell in the");
			MagicSpells.log("main MagicSpells config file.");
			Set<String> spells = reqSec.getKeys(false);
			if (spells != null) {
				for (String spell : spells) {
					int mem = reqSec.getInt(spell);
					this.memoryRequirements.put(spell, mem);
					MagicSpells.debug("Memory requirement for '" + spell + "' spell set to " + mem);
				}
			}
		}
		
		// Get spell mem requirements from magicspells config
		MagicConfig magicConfig = new MagicConfig();
		if (magicConfig.isLoaded()) {
			for (String spell : magicConfig.getSpellKeys()) {
				if (!magicConfig.contains("spells." + spell + ".memory")) continue;;
				int mem = magicConfig.getInt("spells." + spell + ".memory", 0);
				this.memoryRequirements.put(spell, mem);
				MagicSpells.debug("Memory requirement for '" + spell + "' spell set to " + mem);
			}
		}
		
		EventUtil.register(new MemorySpellListener(this), this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (args.length == 1 && sender.hasPermission("magicspells.memory.checkothers")) {
			List<Player> p = getServer().matchPlayer(args[0]);
			if (p.size() == 1) player = p.get(0);
		} else if (sender instanceof Player) {
			player = (Player)sender;
		}
		
		if (player != null) {
			int used = getUsedMemory(player);
			int max = getMaxMemory(player);
			if (sender instanceof Player) {
				MagicSpells.sendMessage(MagicSpells.formatMessage(this.strMemoryUsage, "%memory", used + "", "%total", max + ""), (Player)sender, (String[])null);
			} else {
				String s = this.strMemoryUsage.replace("%memory", used + "").replace("%total", max + "");
				sender.sendMessage(s);
			}
		}
		return true;
	}



	public int getRequiredMemory(Spell spell) {
		// FIXME this should be a get or default type thing
		if (this.memoryRequirements.containsKey(spell.getInternalName())) return this.memoryRequirements.get(spell.getInternalName());
		return 0;
	}
	
	public int getMemoryRemaining(Player player) {
		int max = getMaxMemory(player);
		int used = getUsedMemory(player);
		return max - used;
	}
	
	public int getMaxMemory(Player player) {
		for (int i = 0; i < this.maxMemoryPerms.size(); i++) {
			if (player.hasPermission("magicspells.rank." + this.maxMemoryPerms.get(i))) {
				return this.maxMemoryAmounts.get(i);
			}
		}
		return this.maxMemoryDefault;
	}
	
	public int getUsedMemory(Player player) {
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (spellbook != null) {
			int used = 0;
			for (Spell spell : spellbook.getSpells()) {
				if (this.memoryRequirements.containsKey(spell.getInternalName())) {
					used += this.memoryRequirements.get(spell.getInternalName());
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
