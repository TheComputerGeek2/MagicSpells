package com.nisovin.magicjutsus.castmodifiers;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.events.*;
import com.nisovin.magicjutsus.MagicJutsus;

public class ModifierSet {

	public static CastListener castListener = null;
	public static TargetListener targetListener = null;
	public static ManaListener manaListener = null;

	public static void initializeModifierListeners() {
		boolean modifiers = false;
		boolean targetModifiers = false;
		for (Jutsu jutsu : MagicJutsus.jutsus()) {
			if (jutsu.getModifiers() != null) modifiers = true;
			if (jutsu.getTargetModifiers() != null) targetModifiers = true;
			if (modifiers && targetModifiers) break;
		}

		if (modifiers) {
			castListener = new CastListener();
			MagicJutsus.registerEvents(castListener);
		}
		if (targetModifiers) {
			targetListener = new TargetListener();
			MagicJutsus.registerEvents(targetListener);
		}
		if (MagicJutsus.getManaHandler() != null && MagicJutsus.getManaHandler().getModifiers() != null) {
			manaListener = new ManaListener();
			MagicJutsus.registerEvents(manaListener);
		}
	}

	public static void unload() {
		if (castListener != null) {
			castListener.unload();
			castListener = null;
		}

		if (targetListener != null) {
			targetListener.unload();
			targetListener = null;
		}

		if (manaListener != null) {
			manaListener.unload();
			manaListener = null;
		}
	}

	private List<Modifier> modifiers;

	public ModifierSet(List<String> data) {
		modifiers = new ArrayList<>();
		for (String s : data) {
			Modifier m = Modifier.factory(s);
			if (m != null) {
				modifiers.add(m);
				MagicJutsus.debug(3, "    Modifier added: " + s);
			} else {
				MagicJutsus.error("Problem with modifier: " + s);
			}
		}
	}

	public void apply(JutsuCastEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				String msg = modifier.strModifierFailed != null ? modifier.strModifierFailed : event.getJutsu().getStrModifierFailed();
				MagicJutsus.sendMessage(msg, event.getCaster(), event.getJutsuArgs());
				break;
			}
		}
	}

	public void apply(ManaChangeEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) break;
		}
	}

	public void apply(JutsuTargetEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) {
				if (modifier.strModifierFailed != null) MagicJutsus.sendMessage(modifier.strModifierFailed, event.getCaster(), MagicJutsus.NULL_ARGS);
				break;
			}
		}
	}

	public void apply(MagicJutsusGenericPlayerEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) break;
		}
	}

	public void apply(JutsuTargetLocationEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (!cont) break;
		}
	}

	public boolean check(LivingEntity livingEntity) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity);
			if (!pass) return false;
		}
		return true;
	}

	public boolean check(LivingEntity livingEntity, LivingEntity entity) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity, entity);
			if (!pass) return false;
		}
		return true;
	}

	public boolean check(LivingEntity livingEntity, Location location) {
		for (Modifier modifier : modifiers) {
			boolean pass = modifier.check(livingEntity, location);
			if (!pass) return false;
		}
		return true;
	}

}
