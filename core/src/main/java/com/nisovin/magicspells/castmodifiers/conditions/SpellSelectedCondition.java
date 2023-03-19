package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.castmodifiers.Condition;

public class SpellSelectedCondition extends Condition {

	private SpellFilter filter;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;

		List<String> spells = new ArrayList<>();
		List<String> deniedSpells = new ArrayList<>();
		List<String> tagList = new ArrayList<>();
		List<String> deniedTagList = new ArrayList<>();

		String[] split = var.split(",");
		for (String s : split) {
			boolean denied = false;
			s = s.trim();

			if (s.startsWith("!")) {
				s = s.substring(1);
				denied = true;
			}

			if (s.toLowerCase().startsWith("tag:")) {
				if (denied) deniedTagList.add(s.substring(4));
				else tagList.add(s.substring(4));
			} else {
				if (denied) deniedSpells.add(s);
				else spells.add(s);
			}
		}

		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return spellSelected(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return spellSelected(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return spellSelected(caster);
	}

	private boolean spellSelected(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		Spellbook spellbook = MagicSpells.getSpellbook(pl);
		ItemStack item = pl.getInventory().getItemInMainHand();

		return filter != null && filter.check(spellbook.getActiveSpell(item));
	}

}
