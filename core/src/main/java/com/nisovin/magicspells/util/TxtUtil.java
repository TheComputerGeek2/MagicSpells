package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Collections;
import java.math.RoundingMode;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class TxtUtil {

	public static String getStringNumber(double number, int places) {
		if (places < 0 || !Double.isFinite(number)) return Double.toString(number);
		return new BigDecimal(number).setScale(places, RoundingMode.HALF_UP).toString();
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

	public static List<String> tabCompleteSpellName(CommandSender sender) {
		List<Spell> spells;
		if (sender instanceof ConsoleCommandSender) {
			spells = MagicSpells.getSpellsOrdered();
		} else if (sender instanceof Player player) {
			spells = new ArrayList<>(MagicSpells.getSpellbook(player).getSpells());
		} else return null;

		boolean added;
		List<String> options = new ArrayList<>();
		for (Spell spell : spells) {
			if (spell.isHelperSpell()) continue;
			if (!spell.canCastByCommand()) continue;

			added = false;
			if (MagicSpells.tabCompleteInternalNames()) {
				options.add(spell.getInternalName());
				added = true;
			}

			// Add spell name
			String name = spell.getName();
			if (!name.equals(spell.getInternalName())) {
				options.add('"' + Util.getPlainString(Util.getMiniMessage(spell.getName())) + '"');
				added = true;
			}

			if (!added) options.add(spell.getInternalName());

			String[] aliases = spell.getAliases();
			if (aliases != null) Collections.addAll(options, aliases);
		}
		return options;
	}

	public static List<String> tabCompletePlayerName(CommandSender sender) {
		return tabCompletePlayerName(sender, true);
	}

	public static List<String> tabCompletePlayerName(CommandSender sender, boolean allowSelf) {
		List<String> matches = new ArrayList<>();

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (!sender.isOp() && sender instanceof Player player && !player.canSee(p)) continue;
			if (!allowSelf && p.equals(sender)) continue;

			matches.add(p.getName());
		}

		return matches;
	}

	public static String getPossessiveName(String name) {
		name = name.trim();
		return name + "'" + (name.endsWith("s") ? "" : "s");
	}

}
