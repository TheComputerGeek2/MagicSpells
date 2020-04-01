package com.nisovin.magicjutsus.util;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;

public class TxtUtil {
	
	public static String getStringNumber(double number, int places) {
		if (places < 0) return number + "";
		if (places == 0) return (int) Math.round(number) + "";
		int x = (int) Math.pow(10, places);
		return ((double) Math.round(number * x) / x) + "";
	}
	
	public static String getStringNumber(String textNumber, int places) {
		String ret;
		try {
			ret = getStringNumber(Double.parseDouble(textNumber), places);
		} catch (NumberFormatException nfe) {
			ret = textNumber;
		}
		return ret;
	}
	
	public static List<String> tabCompleteJutsuName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<>();
		if (sender instanceof Player) {
			Jutsubook jutsubook = MagicJutsus.getJutsubook((Player) sender);
			for (Jutsu jutsu : jutsubook.getJutsus()) {
				if (!jutsubook.canTeach(jutsu)) continue;
				if (jutsu.getName().toLowerCase().startsWith(partial)) {
					matches.add(jutsu.getName());
					continue;
				}
				String[] aliases = jutsu.getAliases();
				if (aliases == null || aliases.length <= 0) continue;
				for (String alias : aliases) {
					if (alias.toLowerCase().startsWith(partial)) matches.add(alias);
				}
			}
		} else if (sender.isOp()) {
			for (Jutsu jutsu : MagicJutsus.jutsus()) {
				if (jutsu.getName().toLowerCase().startsWith(partial)) {
					matches.add(jutsu.getName());
					continue;
				}
				String[] aliases = jutsu.getAliases();
				if (aliases == null || aliases.length <= 0) continue;
				for (String alias : aliases) {
					if (alias.toLowerCase().startsWith(partial)) matches.add(alias);
				}
			}
		}
		if (!matches.isEmpty()) return matches;
		return null;
	}
	
}
