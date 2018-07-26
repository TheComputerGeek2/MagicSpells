package com.nisovin.magicspells.materials;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.nisovin.magicspells.util.RegexUtil;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.Dye;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;

public class MagicItemNameResolver implements ItemNameResolver {
	Map<String, Material> materialMap = new HashMap<>();
	
	public MagicItemNameResolver() {
		for (Material mat : Material.values()) {
			this.materialMap.put(mat.name().toLowerCase(), mat);
		}
		
		File file = new File(MagicSpells.getInstance().getDataFolder(), "itemnames.yml");
		if (!file.exists()) {
			MagicSpells.getInstance().saveResource("itemnames.yml", false);
		}
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(file);
			for (String s : config.getKeys(false)) {
				Material m = this.materialMap.get(config.getString(s).toLowerCase());
				if (m == null) continue;
				this.materialMap.put(s.toLowerCase(), m);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<String, Material> toAdd = new HashMap<>();
		for (String s : this.materialMap.keySet()) {
			if (s.contains("_")) {
				toAdd.put(s.replace("_", ""), this.materialMap.get(s));
			}
		}
		this.materialMap.putAll(toAdd);
	}
	
	@Override
	public MagicMaterial resolveItem(String string) {
		if (string == null || string.isEmpty()) return null;
		
		// split type and data
		String stype;
		String sdata;
		if (string.contains(":")) {
			String[] split = string.split(":", 2);
			stype = split[0].toLowerCase();
			sdata = split[1].toLowerCase();
		} else if (string.contains(" ")) {
			String[] split = string.split(" ", 2);
			sdata = split[0].toLowerCase();
			stype = split[1].toLowerCase();
		} else {
			stype = string.toLowerCase();
			sdata = "";
		}
		
		Material type = this.materialMap.get(stype);
		if (type == null) type = Material.getMaterial(stype);
		if (type == null) {
			throw new RuntimeException("Invalid Material: " + string);
		}
		
		if (type.isBlock()) {
			return new MagicBlockMaterial(type);
		} else {
			if (sdata.equals("*")) return new MagicItemAnyDataMaterial(type);
			if (sdata.isEmpty()) return new MagicItemMaterial(type, (short) 0);
			short durability = 0;
			try {
				durability = Short.parseShort(sdata);
			} catch (NumberFormatException e) {
				//DebugHandler.debugNumberFormat(e);
			}
			return new MagicItemMaterial(type, durability);
		}
	}
	
	@Override
	public MagicMaterial resolveBlock(String string) {
		if (string == null || string.isEmpty()) return null;
		
		if (string.contains("|")) return resolveRandomBlock(string);
		
		String stype;
		String sdata;
		if (string.contains(":")) {
			String[] split = string.split(":", 2);
			stype = split[0].toLowerCase();
			sdata = split[1];
		} else {
			stype = string.toLowerCase();
			sdata = "";
		}
		
		Material type = this.materialMap.get(stype);
		if (type == null) type = Material.getMaterial(stype);
		if (type == null) {
			throw new RuntimeException("Invalid Block: " + string);
		}
		
		if (type.isBlock()) {
				return new MagicBlockMaterial(type);

		} else {
			throw new RuntimeException("Invalid Block: " + string);
		}
	}
	
	private MagicMaterial resolveRandomBlock(String string) {
		List<MagicMaterial> materials = new ArrayList<>();
		String[] strings = string.split("\\|");
		for (String s : strings) {
			MagicMaterial mat = resolveBlock(s.trim());
			if (mat == null) continue;
			materials.add(mat);
		}
		return new MagicBlockRandomMaterial(materials.toArray(new MagicMaterial[materials.size()]));
	}

}
