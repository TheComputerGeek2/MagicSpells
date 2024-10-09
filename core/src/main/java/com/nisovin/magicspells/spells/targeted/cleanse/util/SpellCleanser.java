package com.nisovin.magicspells.spells.targeted.cleanse.util;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellFilter;

public abstract class SpellCleanser<T extends Spell> implements Cleanser {

	private static final Pattern FILTER_CHARS = Pattern.compile("[*,!#:]");

	private final List<T> spells = new ArrayList<>();

	@Override
	public boolean add(@NotNull String string) {
		String prefix = getPrefix() + ":";
		if (!string.startsWith(prefix)) return false;
		string = string.substring(prefix.length());

		// If the string is a filter, we need to loop through all spells to find them.
		if (FILTER_CHARS.matcher(string).find()) {
			SpellFilter filter = SpellFilter.fromLegacyString(string);
			for (Spell spell : MagicSpells.getSpellsOrdered()) {
				if (!getSpellClass().isInstance(spell)) continue;
				if (!filter.check(spell)) continue;
				spells.add(getSpellClass().cast(spell));
			}
			return true;
		}

		Spell spell = MagicSpells.getSpellByInternalName(string);
		if (!getSpellClass().isInstance(spell)) return false;
		spells.add(getSpellClass().cast(spell));
		return true;
	}

	@Override
	public boolean isAnyActive(@NotNull LivingEntity entity) {
		for (T spell : spells) {
			if (!getIsActive().test(spell, entity)) continue;
			return true;
		}
		return false;
	}

	@Override
	public void cleanse(@NotNull LivingEntity entity) {
		spells.forEach(spell -> getCleanse().accept(spell, entity));
	}

	@NotNull
	protected abstract String getPrefix();

	@NotNull
	protected abstract Class<T> getSpellClass();

	@NotNull
	protected abstract BiPredicate<T, LivingEntity> getIsActive();

	@NotNull
	protected abstract BiConsumer<T, LivingEntity> getCleanse();

}
