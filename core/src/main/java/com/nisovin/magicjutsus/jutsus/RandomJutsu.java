package com.nisovin.magicjutsus.jutsus;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;

public class RandomJutsu extends InstantJutsu {

	private static Random random = new Random();

	private List<String> rawOptions;

	private RandomOptionSet options;

	private boolean pseudoRandom;
	private boolean checkIndividualCooldowns;
	private boolean checkIndividualModifiers;
	
	public RandomJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		rawOptions = getConfigStringList("jutsus", null);

		pseudoRandom = getConfigBoolean("pseudo-random", true);
		checkIndividualCooldowns = getConfigBoolean("check-individual-cooldowns", true);
		checkIndividualModifiers = getConfigBoolean("check-individual-modifiers", true);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		options = new RandomOptionSet();
		for (String s : rawOptions) {
			String[] split = s.split(" ");
			Ninjutsu jutsu = new Ninjutsu(split[0]);
			int weight = 0;
			try {
				weight = Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				// No op
			}

			if (jutsu.process() && weight > 0) options.add(new JutsuOption(jutsu, weight));
			else MagicJutsus.error("Invalid jutsu option on RandomJutsu '" + internalName + "': " + s);
		}
		
		rawOptions.clear();
		rawOptions = null;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			RandomOptionSet set = options;
			if (checkIndividualCooldowns || checkIndividualModifiers) {
				set = new RandomOptionSet();
				for (JutsuOption o : options.randomOptionSetOptions) {
					if (checkIndividualCooldowns && o.jutsu.getJutsu().onCooldown(livingEntity)) continue;
					if (checkIndividualModifiers) {
						ModifierSet modifiers = o.jutsu.getJutsu().getModifiers();
						if (modifiers != null && livingEntity instanceof Player && !modifiers.check((Player) livingEntity)) continue;
					}
					set.add(o);
				}
			}
			if (!set.randomOptionSetOptions.isEmpty()) {
				Ninjutsu jutsu = set.choose();
				if (jutsu != null) return jutsu.cast(livingEntity, power);
				return PostCastAction.ALREADY_HANDLED;
			}
			return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private static class JutsuOption {

		private Ninjutsu jutsu;
		private int weight;
		private int adjustedWeight;

		private JutsuOption(Ninjutsu jutsu, int weight) {
			this.jutsu = jutsu;
			this.weight = weight;
			adjustedWeight = weight;
		}
		
	}

	private class RandomOptionSet {

		private List<JutsuOption> randomOptionSetOptions = new ArrayList<>();
		private int total = 0;

		private void add(JutsuOption option) {
			randomOptionSetOptions.add(option);
			total += option.adjustedWeight;
		}

		private Ninjutsu choose() {
			int r = random.nextInt(total);
			int x = 0;
			Ninjutsu jutsu = null;
			for (JutsuOption o : randomOptionSetOptions) {
				if (r < o.adjustedWeight + x && jutsu == null) {
					jutsu = o.jutsu;
					if (pseudoRandom) o.adjustedWeight = 0;
					else break;
				} else {
					x += o.adjustedWeight;
					if (pseudoRandom) o.adjustedWeight += o.weight;
				}
			}
			return jutsu;
		}
		
	}
	
}
