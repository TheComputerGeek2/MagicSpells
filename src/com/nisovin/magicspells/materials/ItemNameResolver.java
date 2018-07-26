package com.nisovin.magicspells.materials;

import java.util.Random;


public interface ItemNameResolver {

	Random rand = new Random();

	MagicMaterial resolveItem(String string);
	
	MagicMaterial resolveBlock(String string);
	
}
