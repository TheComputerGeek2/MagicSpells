package com.nisovin.magicjutsus.jutsus;

import java.util.UUID;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public final class MultiJutsu extends InstantJutsu {
	
	private static final Pattern RANGED_DELAY_PATTERN = Pattern.compile("DELAY [0-9]+ [0-9]+");
	private static final Pattern BASIC_DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private Random random;

	private List<String> jutsuList;
	private List<ActionChance> actions;

	private boolean castWithItem;
	private boolean castByCommand;
	private boolean customJutsuCastChance;
	private boolean castRandomJutsuInstead;
	private boolean enableIndividualChances;

	public MultiJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		random = new Random();

		actions = new ArrayList<>();
		jutsuList = getConfigStringList("jutsus", null);

		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		customJutsuCastChance = getConfigBoolean("enable-custom-jutsu-cast-chance", false);
		castRandomJutsuInstead = getConfigBoolean("cast-random-jutsu-instead", false);
		enableIndividualChances = getConfigBoolean("enable-individual-chances", false);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (jutsuList != null) {
			for (String s : jutsuList) {
				String[] parts = s.split(":");
				double chance = parts.length == 2 ? Double.parseDouble(parts[1]) : 0.0D;
				s = parts[0];
				if (RegexUtil.matches(RANGED_DELAY_PATTERN, s)) {
					String[] splits = s.split(" ");
					int minDelay = Integer.parseInt(splits[1]);
					int maxDelay = Integer.parseInt(splits[2]);
					actions.add(new ActionChance(new Action(minDelay, maxDelay), chance));
				} else if (RegexUtil.matches(BASIC_DELAY_PATTERN, s)) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new ActionChance(new Action(delay), chance));
				} else {
					Ninjutsu jutsu = new Ninjutsu(s);
					if (jutsu.process()) actions.add(new ActionChance(new Action(jutsu), chance));
					else MagicJutsus.error("MultiJutsu '" + internalName + "' has an invalid jutsu '" + s + "' defined!");
				}
			}
		}
		jutsuList = null;
	}

	@Override
	public Jutsu.PostCastAction castJutsu(LivingEntity livingEntity, Jutsu.JutsuCastState state, float power, String[] args) {
		if (state == Jutsu.JutsuCastState.NORMAL) {
			if (!castRandomJutsuInstead) {
				int delay = 0;
				for (ActionChance actionChance : actions) {
					Action action = actionChance.getAction();
					if (action.isDelay()) {
						delay += action.getDelay();
					} else if (action.isJutsu()) {
						Ninjutsu jutsu = action.getJutsu();
						if (delay == 0) jutsu.cast(livingEntity, power);
						else MagicJutsus.scheduleDelayedTask(new DelayedJutsu(jutsu, livingEntity, power), delay);
					}
				}
			} else {
				int index;
				if (customJutsuCastChance) {
					int total = 0;
					for (ActionChance actionChance : actions) {
						total = (int) Math.round(total + actionChance.getChance());
					}
					index = random.nextInt(total);
					int s = 0;
					int i = 0;
					while (s < index) {
						s = (int) Math.round(s + actions.get(i++).getChance());
					}
					Action action = actions.get(Math.max(0, i - 1)).getAction();
					if (action.isJutsu()) action.getJutsu().cast(livingEntity, power);
				} else if (enableIndividualChances) {
					for (ActionChance actionChance : actions) {
						double chance = Math.random();
						if ((actionChance.getChance() / 100.0D > chance) && actionChance.getAction().isJutsu()) {
							Action action = actionChance.getAction();
							action.getJutsu().cast(livingEntity, power);
						}
					}
				} else {
					Action action = actions.get(random.nextInt(actions.size())).getAction();
					action.getJutsu().cast(livingEntity, power);
				}
			}
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return Jutsu.PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(final CommandSender sender, final String[] args) {
		if (!castRandomJutsuInstead) {
			int delay = 0;
			for (ActionChance actionChance : actions) {
				Action action = actionChance.getAction();
				if (action.isJutsu()) {
					if (delay == 0) action.getJutsu().getJutsu().castFromConsole(sender, args);
					else {
						final Jutsu jutsu = action.getJutsu().getJutsu();
						MagicJutsus.scheduleDelayedTask(() -> jutsu.castFromConsole(sender, args), delay);
					}
				} else if (action.isDelay()) delay += action.getDelay();
			}
		} else {
			int index;
			if (customJutsuCastChance) {
				int total = 0;
				for (ActionChance actionChance : actions) {
					total = (int) Math.round(total + actionChance.getChance());
				}
				index = random.nextInt(total);
				int s = 0;
				int i = 0;
				while (s < index) {
					s = (int) Math.round(s + actions.get(i++).getChance());
				}
				Action action = actions.get(Math.max(0, i - 1)).getAction();
				if (action.isJutsu()) action.getJutsu().getJutsu().castFromConsole(sender, args);
			} else if (enableIndividualChances) {
				for (ActionChance actionChance : actions) {
					double chance = Math.random();
					if ((actionChance.getChance() / 100.0D > chance) && actionChance.getAction().isJutsu()) {
						actionChance.getAction().getJutsu().getJutsu().castFromConsole(sender, args);
					}
				}
			} else {
				Action action = actions.get(random.nextInt(actions.size())).getAction();
				if (action.isJutsu()) action.getJutsu().getJutsu().castFromConsole(sender, args);
			}
		}
		return true;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	private class Action {
		
		private Ninjutsu jutsu;
		private int delay; // Also gonna serve as minimum delay
		private boolean isRangedDelay = false;
		private int maxDelay;

		Action(Ninjutsu jutsu) {
			this.jutsu = jutsu;
			delay = 0;
		}

		Action(int delay) {
			this.delay = delay;
			jutsu = null;
		}
		
		Action(int minDelay, int maxDelay) {
			this.delay = minDelay;
			this.maxDelay = maxDelay;
			jutsu = null;
			isRangedDelay = true;
		}

		public boolean isJutsu() {
			return jutsu != null;
		}

		public Ninjutsu getJutsu() {
			return jutsu;
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

	private static class DelayedJutsu implements Runnable {
		
		private Ninjutsu jutsu;
		private UUID casterUUID;
		private float power;

		DelayedJutsu(Ninjutsu jutsu, LivingEntity livingEntity, float power) {
			this.jutsu = jutsu;
			this.casterUUID = livingEntity.getUniqueId();
			this.power = power;
		}

		@Override
		public void run() {
			Entity entity = Bukkit.getEntity(casterUUID);
			if (entity != null && entity.isValid() && entity instanceof LivingEntity) jutsu.cast((LivingEntity) entity, power);
		}
		
	}

	private static class ActionChance {
		
		private Action action;
		private double chance;

		ActionChance(Action action, double chance) {
			this.action = action;
			this.chance = chance;
		}

		public Action getAction() {
			return action;
		}

		public double getChance() {
			return chance;
		}
		
	}
	
}
