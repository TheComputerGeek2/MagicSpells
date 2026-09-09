package com.nisovin.magicspells.spells.targeted.cleanse;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.spells.targeted.RewindSpell;
import com.nisovin.magicspells.spells.targeted.cleanse.util.SpellCleanser;

public class RewindSpellCleanser extends SpellCleanser<RewindSpell> {

	@Override
	protected @NotNull String getPrefix() {
		return "rewind";
	}

	@Override
	protected @NotNull Class<RewindSpell> getSpellClass() {
		return RewindSpell.class;
	}

	@Override
	protected @NotNull BiPredicate<RewindSpell, LivingEntity> getIsActive() {
		return RewindSpell::isRewinding;
	}

	@Override
	protected @NotNull BiConsumer<RewindSpell, LivingEntity> getCleanse() {
		return RewindSpell::rewindAll;
	}

}
