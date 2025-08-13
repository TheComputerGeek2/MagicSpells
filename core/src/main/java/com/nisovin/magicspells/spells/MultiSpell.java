package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;

public final class MultiSpell extends InstantSpell {

	private static final Pattern RANGED_DELAY_PATTERN = Pattern.compile("DELAY \\d+ \\d+");
	private static final Pattern BASIC_DELAY_PATTERN = Pattern.compile("DELAY \\d+");

	private List<String> spellList;
	private final List<ActionChance> actions;

	private final ConfigData<Boolean> customSpellCastChance;
	private final ConfigData<Boolean> castRandomSpellInstead;
	private final ConfigData<Boolean> enableIndividualChances;

	public MultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		customSpellCastChance = getConfigDataBoolean("enable-custom-spell-cast-chance", false);
		castRandomSpellInstead = getConfigDataBoolean("cast-random-spell-instead", false);
		enableIndividualChances = getConfigDataBoolean("enable-individual-chances", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (spellList == null) return;

		for (String spellString : spellList) {
			int chanceIndex = spellString.lastIndexOf(':');

			double chance = 0d;
			if (chanceIndex != -1) {
				try {
					chance = Double.parseDouble(spellString.substring(chanceIndex + 1));
					spellString = spellString.substring(0, chanceIndex);
				} catch (NumberFormatException ignored) {
				}
			}

			if (RANGED_DELAY_PATTERN.asMatchPredicate().test(spellString)) {
				String[] splits = spellString.split(" ");
				int minDelay = Integer.parseInt(splits[1]);
				int maxDelay = Integer.parseInt(splits[2]);

				if (minDelay <= 0 || minDelay >= maxDelay) {
					MagicSpells.error("MultiSpell '" + internalName + "' has an invalid ranged DELAY interval specified in '" + spellString + "'");
					continue;
				}
				actions.add(new ActionChance(new Action(minDelay, maxDelay), chance));
			} else if (BASIC_DELAY_PATTERN.asMatchPredicate().test(spellString)) {
				int delay = Integer.parseInt(spellString.split(" ")[1]);
				actions.add(new ActionChance(new Action(delay), chance));
			} else {
				Subspell spell = new Subspell(spellString);
				if (spell.process()) actions.add(new ActionChance(new Action(spell), chance));
				else MagicSpells.error("MultiSpell '" + internalName + "' has an invalid spell '" + spellString + "' defined!");
			}
		}

		spellList = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!castRandomSpellInstead.get(data)) {
			int delay = 0;

			for (ActionChance actionChance : actions) {
				Action action = actionChance.action();

				if (action.isDelay()) delay += action.getDelay();
				else if (action.isSpell()) {
					Subspell spell = action.getSpell();

					if (delay == 0) spell.subcast(data);
					else MagicSpells.scheduleDelayedTask(new DelayedSpell(spell, data), delay);
				}
			}
		} else {
			if (customSpellCastChance.get(data)) {
				double total = 0;
				for (ActionChance actionChance : actions) total += actionChance.chance;

				double index = random.nextDouble(total);
				Action action = null;
				double subChance = 0;
				for (ActionChance actionChance : actions) {
					subChance += actionChance.chance;

					action = actionChance.action;
					if (subChance > index) break;
				}

				if (action != null && action.isSpell()) action.getSpell().subcast(data);
			} else if (enableIndividualChances.get(data)) {
				for (ActionChance actionChance : actions) {
					double chance = Math.random();
					if ((actionChance.chance() / 100 > chance) && actionChance.action().isSpell()) {
						Action action = actionChance.action();
						action.getSpell().subcast(data);
					}
				}
			} else if (!actions.isEmpty()) {
				Action action = actions.get(random.nextInt(actions.size())).action();
				action.getSpell().subcast(data);
			}
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, final String[] args) {
		SpellData subData = new SpellData(null, 1f, args);

		if (!castRandomSpellInstead.get(subData)) {
			int delay = 0;

			for (ActionChance actionChance : actions) {
				Action action = actionChance.action();

				if (action.isDelay()) delay += action.getDelay();
				else if (action.isSpell()) {
					Spell spell = action.getSpell().getSpell();

					if (delay == 0) spell.castFromConsole(sender, args);
					else MagicSpells.scheduleDelayedTask(() -> spell.castFromConsole(sender, args), delay);
				}
			}
		} else {
			if (customSpellCastChance.get(subData)) {
				double total = 0;
				for (ActionChance actionChance : actions) total += actionChance.chance;

				double index = random.nextDouble(total);
				Action action = null;
				double subChance = 0;
				for (ActionChance actionChance : actions) {
					subChance += actionChance.chance;

					action = actionChance.action;
					if (subChance > index) break;
				}

				if (action != null && action.isSpell()) action.getSpell().getSpell().castFromConsole(sender, args);
			} else if (enableIndividualChances.get(subData)) {
				for (ActionChance actionChance : actions) {
					double chance = Math.random();
					if ((actionChance.chance() / 100 > chance) && actionChance.action().isSpell()) {
						Action action = actionChance.action();
						action.getSpell().getSpell().castFromConsole(sender, args);
					}
				}
			} else if (!actions.isEmpty()) {
				Action action = actions.get(random.nextInt(actions.size())).action();
				action.getSpell().getSpell().castFromConsole(sender, args);
			}
		}

		return true;
	}

	private static class Action {

		private final Subspell spell;

		private final boolean isRangedDelay;
		private final int maxDelay;
		private final int delay;

		Action(Subspell spell) {
			this.spell = spell;

			isRangedDelay = false;
			maxDelay = 0;
			delay = 0;
		}

		Action(int delay) {
			spell = null;

			isRangedDelay = false;
			maxDelay = 0;
			this.delay = delay;
		}

		Action(int minDelay, int maxDelay) {
			spell = null;

			isRangedDelay = true;
			this.maxDelay = maxDelay;
			this.delay = minDelay;
		}

		public boolean isSpell() {
			return spell != null;
		}

		public Subspell getSpell() {
			return spell;
		}

		public boolean isDelay() {
			return delay > 0 || isRangedDelay;
		}

		private int getRandomDelay() {
			return random.nextInt(maxDelay - delay) + delay;
		}

		public int getDelay() {
			return isRangedDelay ? getRandomDelay() : delay;
		}

	}

	private record DelayedSpell(Subspell spell, SpellData data) implements Runnable {

		@Override
			public void run() {
				if (!data.caster().isValid()) return;
				spell.subcast(data);
			}

		}

	private record ActionChance(Action action, double chance) {
	}

}
