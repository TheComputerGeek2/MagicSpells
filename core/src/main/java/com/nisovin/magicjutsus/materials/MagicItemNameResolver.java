package com.nisovin.magicjutsus.materials;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.nisovin.magicjutsus.util.RegexUtil;
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

import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.MagicJutsus;

public class MagicItemNameResolver implements ItemNameResolver {
	
	private static final Pattern DIGITS = Pattern.compile("[0-9]+");
	private static final Pattern BLOCK_BYTE_DATA_PATTERN = Pattern.compile("^[0-9]+$");
	
	Map<String, Material> materialMap = new HashMap<>();
	Map<String, MaterialData> materialDataMap = new HashMap<>();
	Random rand = new Random();
	
	public MagicItemNameResolver() {
		for (Material mat : Material.values()) {
			this.materialMap.put(mat.name().toLowerCase(), mat);
		}
		
		File file = new File(MagicJutsus.getInstance().getDataFolder(), "itemnames.yml");
		if (!file.exists()) {
			MagicJutsus.getInstance().saveResource("itemnames.yml", false);
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
	public ItemTypeAndData resolve(String string) {
		if (string == null || string.isEmpty()) return null;
		ItemTypeAndData item = new ItemTypeAndData();
		if (string.contains(":")) {
			String[] split = string.split(":");
			if (RegexUtil.matches(DIGITS, split[0])) {
				throw new RuntimeException("Numerical ids are no longer supported");
			} else {
				Material mat = Material.getMaterial(split[0].toUpperCase());
				if (mat == null) return null;
				item.material = mat;
			}
			if (RegexUtil.matches(DIGITS, split[1])) {
				throw new RuntimeException("Numerical ids are no longer supported");
				//item.data = Short.parseShort(split[1]);
			} else {
				return null;
			}
		} else {
			if (RegexUtil.matches(DIGITS, string)) {
				throw new RuntimeException("Numerical ids are no longer supported");
				//item.material = MaterialHelper.getFromNumericalId(Integer.parseInt(string));
			} else {
				Material mat = Material.getMaterial(string.toUpperCase());
				if (mat == null) return null;
				item.material = mat;
			}
		}
		return item;
	}
	
	@Override
	public MagicMaterial resolveItem(String string) {
		if (string == null || string.isEmpty()) return null;
		
		// first check for predefined material datas
		MaterialData matData = this.materialDataMap.get(string.toLowerCase());
		if (matData != null) {
			if (matData.getItemType().isBlock()) return new MagicBlockMaterial(matData);
			return new MagicItemMaterial(matData);
		}
		
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
		if (type == null) return resolveUnknown(stype, sdata);
		
		if (type.isBlock()) {
			return new MagicBlockMaterial(resolveBlockData(type, sdata));
		} else {
			if (sdata.equals("*")) return new MagicItemAnyDataMaterial(type);
			MaterialData itemData = resolveItemData(type, sdata);
			if (itemData != null) return new MagicItemMaterial(itemData);
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
		if (type == null) {
			return resolveUnknown(stype, sdata);
		}
		
		if (type.isBlock()) {
			if (sdata.equals("*")) {
				return new MagicBlockAnyDataMaterial(new MaterialData(type));
			} else {
				return new MagicBlockMaterial(resolveBlockData(type, sdata));
			}
		} else {
			return null;
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
	
	private MaterialData resolveBlockData(Material type, String sdata) {
		if (type == Material.LEGACY_LOG || type == Material.LEGACY_SAPLING || type == Material.LEGACY_WOOD) {
			return getTree(sdata);
		} else if (type == Material.LEGACY_LEAVES) {
			return getLeaves(sdata);
		} else if (type == Material.LEGACY_WOOL) {
			return getWool(sdata);
		} else if (RegexUtil.matches(BLOCK_BYTE_DATA_PATTERN, sdata)) {
			return type.getNewData(Byte.parseByte(sdata));
		} else {
			return new MaterialData(type);
		}
	}
	
	private MaterialData resolveItemData(Material type, String sdata) {
		if (type == Material.LEGACY_INK_SACK) {
			return getDye(sdata);
		} else {
			return null;
		}
	}
	
	private MagicMaterial resolveUnknown(String stype, String sdata) {
		try {
			Material material = Material.matchMaterial(stype, true);
			if (sdata.equals("*")) {
				return new MagicUnknownAnyDataMaterial(material);
			} else {
				short data = sdata.isEmpty() ? 0 : Short.parseShort(sdata);
				return new MagicUnknownMaterial(material, data);
			}
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return null;
		}
	}
	
	private Dye getDye(String data) {
		Dye dye = new Dye();
		dye.setColor(getDyeColor(data));
		return dye;
	}
	
	private Wool getWool(String data) {
		return new Wool(getDyeColor(data));
	}
	
	private DyeColor getDyeColor(String data) {
		if (data != null && data.equalsIgnoreCase("random")) {
			return DyeColor.values()[rand.nextInt(DyeColor.values().length)];
		} else {
			DyeColor color = DyeColor.WHITE;
			if (data != null && !data.isEmpty()) {
				data = data.replace("_", "").replace(" ", "").toLowerCase();
				for (DyeColor c : DyeColor.values()) {
					if (data.equals(c.name().replace("_", "").toLowerCase())) {
						color = c;
						break;
					}
				}
			}
			return color;
		}
	}
	
	/*
	 * Data format
	 * species direction
	 * species = birch, jungle, redwood, random
	 * direction = east, west, north, south, random
	 */
	private Tree getTree(String data) {
		TreeSpecies species = TreeSpecies.GENERIC;
		BlockFace dir = BlockFace.UP;
		if (data != null && !data.isEmpty()) {
			String[] split = data.split("[: ]");
			if (split.length >= 1) {
				species = getTreeSpecies(split[0]);
			}
			if (split.length >= 2) {
				if (split[1].equalsIgnoreCase("east")) {
					dir = BlockFace.EAST;
				} else if (split[1].equalsIgnoreCase("west")) {
					dir = BlockFace.WEST;
				} else if (split[1].equalsIgnoreCase("north")) {
					dir = BlockFace.NORTH;
				} else if (split[1].equalsIgnoreCase("south")) {
					dir = BlockFace.SOUTH;
				} else if (split[1].equalsIgnoreCase("random")) {
					int r = rand.nextInt(3);
					if (r == 0) {
						dir = BlockFace.EAST;
					} else if (r == 1) {
						dir = BlockFace.NORTH;
					}
				}
			}
		}
		return new Tree(species, dir);
	}
	
	private Leaves getLeaves(String data) {
		return new Leaves(getTreeSpecies(data));
	}
	
	// Data can be
	// birch, jungle, redwood, random
	private TreeSpecies getTreeSpecies(String data) {
		if (data.equalsIgnoreCase("birch")) {
			return TreeSpecies.BIRCH;
		} else if (data.equalsIgnoreCase("jungle")) {
			return TreeSpecies.JUNGLE;
		} else if (data.equalsIgnoreCase("redwood")) {
			return TreeSpecies.REDWOOD;
		} else if (data.equalsIgnoreCase("random")) {
			return TreeSpecies.values()[rand.nextInt(TreeSpecies.values().length)];
		} else {
			return TreeSpecies.GENERIC;
		}
	}

}
