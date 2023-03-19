package com.nisovin.magicspells.castmodifiers;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;

public class ModifierSet {

	public static ModifierListener modifierListener;

	public static void initializeModifierListeners() {
		modifierListener = new ModifierListener();
		MagicSpells.registerEvents(modifierListener);
	}

	public static void unload() {
		if (modifierListener != null) {
			modifierListener.unload();
			modifierListener = null;
		}
	}

	private final List<Modifier> modifiers;

	public ModifierSet(List<String> data) {
		this(data, null, false);
	}

	public ModifierSet(List<String> data, Spell spell) {
		this(data, spell, false);
	}

	public ModifierSet(List<String> data, boolean isFromManaSystem) {
		this(data, null, isFromManaSystem);
	}

	private ModifierSet(List<String> data, Spell spell, boolean isFromManaSystem) {
		modifiers = new ArrayList<>();
		for (String s : data) {
			Modifier m = new Modifier();
			m.process(s);

			if (!m.isInitialized()) {
				String extra = "";
				if (m.getCustomActionData() != null) extra = ": " + m.getCustomActionData().getInvalidText();

				if (isFromManaSystem) MagicSpells.error("Mana system has a problem with modifier '" + s + "'" + extra);
				else if (spell != null) MagicSpells.error("Spell '" + spell.getInternalName() + "' has a problem with modifier '" + s + "'" + extra);
				else MagicSpells.error("Problem with modifier: " + s + "'" + extra);
				continue;
			}

			if (m.getStrModifierFailed() == null) m.setStrModifierFailed(spell.getStrModifierFailed());

			modifiers.add(m);

			if (isFromManaSystem) MagicSpells.debug(3, "    Modifier added for mana system: " + s);
			else if (spell != null) MagicSpells.debug(3, "    Modifier added for spell '" + spell.getInternalName() + "': " + s);
			else MagicSpells.debug(3, "    Modifier added: " + s);
		}
	}

	public void apply(SpellCastEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), event.getSpellArgs());
			break;
		}
	}

	public void apply(ManaChangeEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getPlayer(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public void apply(SpellTargetEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public void apply(MagicSpellsGenericPlayerEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getPlayer(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public void apply(SpellTargetLocationEvent event) {
		for (Modifier modifier : modifiers) {
			boolean cont = modifier.apply(event);
			if (cont) continue;

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), event.getCaster(), MagicSpells.NULL_ARGS);
			break;
		}
	}

	public ModifierResult apply(LivingEntity caster, SpellData data) {
		for (Modifier modifier : modifiers) {
		    ModifierResult result = modifier.apply(caster, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data().args());
			return result;
		}

		return new ModifierResult(data, true);
	}

	public ModifierResult apply(LivingEntity caster, LivingEntity target, SpellData data) {
		for (Modifier modifier : modifiers) {
			ModifierResult result = modifier.apply(caster, target, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data().args());
			return result;
		}

		return new ModifierResult(data, true);
	}

	public ModifierResult apply(LivingEntity caster, Location target, SpellData data) {
		for (Modifier modifier : modifiers) {
			ModifierResult result = modifier.apply(caster, target, data);
			if (result.check()) {
				data = result.data();
				continue;
			}

			if (modifier.getStrModifierFailed() != null) MagicSpells.sendMessage(modifier.getStrModifierFailed(), caster, result.data().args());
			return result;
		}

		return new ModifierResult(data, true);
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
