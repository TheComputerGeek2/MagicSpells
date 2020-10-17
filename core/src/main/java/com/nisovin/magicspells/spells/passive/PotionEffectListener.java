package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.*;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class PotionEffectListener extends PassiveListener {

	private PotionTrigger trigger;

	@Override
	public void initialize(String var) {
		List<PotionEffectType> types = new ArrayList<>();
		List<Action> actions = new ArrayList<>();
		List<Cause> causes = new ArrayList<>();

		if (var != null && !var.isEmpty()) {
			var = var.toUpperCase();
			String[] splits = var.split(" ");
			if (!splits[0].equals("*")) { //Asterisks are wildcards, for when you want the parameter to pass on *any* value
				for (String s : splits[0].split(",")) { //Each parameter can accept a list of options, separated by commas
					if (PotionEffectType.getByName(s) != null) { types.add(PotionEffectType.getByName(s)); }
					else MagicSpells.error("PotionEffect Passive " + passiveSpell.getInternalName() + " has an invalid effect defined: " + s + "!");
				}
			} else types = Arrays.asList(PotionEffectType.values()); //It's dirty, but it works. If a wildcard is used, dump every value into the list.

			if (splits.length > 1 && !splits[1].equals("*")) {
				for (String s : splits[1].split(",")) {
					try { actions.add(Action.valueOf(s)); }
					catch (IllegalArgumentException e) {MagicSpells.error("PotionEffect Passive " + passiveSpell.getInternalName() + " has an invalid action defined: " + s + "!");}
				}
			} else actions = Arrays.asList(Action.values());

			if (splits.length > 1 && !splits[2].equals("*")) {
				for (String s : splits[2].split(",")) {
					try { causes.add(Cause.valueOf(s)); }
					catch (IllegalArgumentException e) {MagicSpells.error("PotionEffect Passive " + passiveSpell.getInternalName() + " has an invalid cause defined: " + s + "!");}
				}
			} else causes = Arrays.asList(Cause.values());
		}

		trigger = new PotionTrigger(types, actions, causes);
	}

	@EventHandler
	public void onPotionEffect(EntityPotionEffectEvent event) {
		if (!(event.getEntity() instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) event.getEntity();
		if (!hasSpell(entity)) return;
		if (!canTrigger(entity)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		if (!trigger.actions.contains(event.getAction()) || !trigger.causes.contains(event.getCause())) return;

		PotionEffectType thisEffect = null;
		switch (event.getAction()) { //The effect used by the event is referenced differently based on the action, so unfortunately this is needed
			case ADDED:
				thisEffect = event.getNewEffect().getType();
				break;
			case CHANGED:
				thisEffect = event.getModifiedType();
				break;
			case REMOVED:
			case CLEARED:
				thisEffect = event.getOldEffect().getType();
				break;
		}

		if (thisEffect == null || !trigger.types.contains(thisEffect)) return;
		boolean casted = passiveSpell.activate(entity);
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
	}

	private static class PotionTrigger {

		private List<PotionEffectType> types;
		private List<Action> actions;
		private List<Cause> causes;

		PotionTrigger(List<PotionEffectType> types, List<Action> actions, List<Cause> causes) {
			this.types = types;
			this.actions = actions;
			this.causes = causes;
		}

	}

}
