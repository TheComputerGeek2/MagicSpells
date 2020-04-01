package com.nisovin.magicjutsus.util;

import java.util.Arrays;

import org.bukkit.entity.Player;

/**
 * Original credit to desht
 *
 */
public class ExperienceUtils {
	
	private static final int MAX_LEVEL_SUPPORTED = 500;

	private static final int xpRequiredForNextLevel[] = new int[MAX_LEVEL_SUPPORTED];
	private static final int xpTotalToReachLevel[] = new int[MAX_LEVEL_SUPPORTED];

	static {
		xpTotalToReachLevel[0] = 0;
		for (int i = 1; i < 17; i++) {
			xpRequiredForNextLevel[i - 1] = 17;
			xpTotalToReachLevel[i] = i * 17;
		}
		for (int i = 17; i < MAX_LEVEL_SUPPORTED; i++) {
			xpRequiredForNextLevel[i - 1] = 3 * i - 31;
			xpTotalToReachLevel[i] = xpTotalToReachLevel[i - 1] + xpRequiredForNextLevel[i - 1];
		}
	}
	
	public static void changeExp(Player player, int amt) {
		int xp = getCurrentExp(player) + amt;
		if (xp < 0) xp = 0;
		
		int newLvl = getLevelFromExp(xp);
		if (newLvl >= MAX_LEVEL_SUPPORTED) return;
		if (player.getLevel() != newLvl) player.setLevel(newLvl);
		
		float pct = (float) (xp - xpTotalToReachLevel[newLvl]) / (float) xpRequiredForNextLevel[newLvl];
		player.setExp(pct);
	}
	
	public static int getCurrentExp(Player player) {
		int lvl = player.getLevel();
		return xpTotalToReachLevel[lvl] + (int) (xpRequiredForNextLevel[lvl] * player.getExp());
	}
	
	public static boolean hasExp(Player player, int amt) {
		if (player.getLevel() >= MAX_LEVEL_SUPPORTED) return false;
		return getCurrentExp(player) >= amt;
	}
	
	public static int getLevelFromExp(int exp) {
		if (exp <= 0) return 0;
		int pos = Arrays.binarySearch(xpTotalToReachLevel, exp);
		return pos < 0 ? -pos - 2 : pos;
	}
	
}
