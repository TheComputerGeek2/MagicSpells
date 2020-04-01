package com.nisovin.magicjutsus.materials;

import org.bukkit.Material;

import java.util.Random;


public interface ItemNameResolver {

	Random rand = new Random();

	@Deprecated
	ItemTypeAndData resolve(String string);
	
	MagicMaterial resolveItem(String string);
	
	MagicMaterial resolveBlock(String string);
	
	class ItemTypeAndData {
		
		public Material material = Material.AIR;
		public short data = 0;
		
	}
	
}
