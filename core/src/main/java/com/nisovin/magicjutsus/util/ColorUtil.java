package com.nisovin.magicjutsus.util;

import org.bukkit.Color;

import com.nisovin.magicjutsus.DebugHandler;

public class ColorUtil {
	
	public static Color getColorFromHexString(String hex) {
		if (hex == null) return null;
		String working = hex.replace("#", "");
		try {
			int value = Integer.parseInt(working, 16);
			return Color.fromRGB(value);
		} catch (IllegalArgumentException e) {
			DebugHandler.debugIllegalArgumentException(e);
			return null;
		}
	}
	
	public static Color getColorFromRGBString(String value) {
		if (value == null) return null;
		String[] splits = value.split(",");
		if (splits.length < 3) return null;
		
		int red;
		int green;
		int blue;
		try {
			red = Integer.parseInt(splits[0]);
			green = Integer.parseInt(splits[1]);
			blue = Integer.parseInt(splits[2]);
			return Color.fromRGB(red, green, blue);
		} catch (IllegalArgumentException e) {
			DebugHandler.debugIllegalArgumentException(e);
			//TODO determine an appropriate means of logging this
			return null;
		}
		
	}
	
}
