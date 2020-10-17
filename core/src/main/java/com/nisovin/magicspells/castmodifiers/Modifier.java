package com.nisovin.magicspells.castmodifiers;

import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class Modifier implements IModifier {

	private static final Pattern MODIFIER_STR_FAILED_PATTERN = Pattern.compile("\\$\\$");

	private boolean negated = false;
	private Condition condition;
	private ModifierType type;
	private String modifierVar;
	private float modifierVarFloat;
	private int modifierVarInt;
	private Object customActionData = null;
	String strModifierFailed = null;

	// Is this a condition that will want to access the events directly?
	private boolean alertCondition = false;

	public static Modifier factory(String s) {
		Modifier m = new Modifier();
		String[] s1 = RegexUtil.split(MODIFIER_STR_FAILED_PATTERN, s, 0);
		String[] data = s1[0].trim().split(" ", 4);
		//String[] data = Util.splitParams(s1[0].trim(), 4);
		if (data.length < 2) return null;

		// Get condition
		if (data[0].startsWith("!")) {
			m.negated = true;
			data[0] = data[0].substring(1);
		}
		m.condition = MagicSpells.getConditionManager().getConditionByName(data[0].replace("_", ""));
		if (m.condition == null) return null;

		// Get type and vars
		m.type = getTypeByName(data[1]);
		if (m.type == null && data.length > 2) {
			boolean init = m.condition.initialize(data[1]);
			if (!init) return null;
			m.type = getTypeByName(data[2]);
			if (data.length > 3) m.modifierVar = data[3];
		} else if (data.length > 2) {
			m.modifierVar = data[2];
		}

		// Check type
		if (m.type == null) return null;

		// Process modifiervar
		try {
			if (m.type.usesModifierFloat()) m.modifierVarFloat = Float.parseFloat(m.modifierVar);
			else if (m.type.usesModifierInt()) m.modifierVarInt = Integer.parseInt(m.modifierVar);
			else if (m.type.usesCustomData()) {
				m.customActionData = m.type.buildCustomActionData(m.modifierVar);
				if (m.customActionData == null) return null;
			}
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return null;
		}

		// Check for failed string
		if (s1.length > 1) m.strModifierFailed = s1[1].trim();

		// Check for the alert condition
		if (m.condition instanceof IModifier) m.alertCondition = true;

		// Done
		return m;
	}

	@Override
	public boolean apply(SpellCastEvent event) {
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster);
		if (negated) check = !check;
		return type.apply(event, check, modifierVar, modifierVarFloat, modifierVarInt, customActionData);
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		Player player = event.getPlayer();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(player);
		if (negated) check = !check;
		return type.apply(event, check, modifierVar, modifierVarFloat, modifierVarInt, customActionData);
	}

	@Override
	public boolean apply(SpellTargetEvent event) {
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster, event.getTarget());
		if (negated) check = !check;
		return type.apply(event, check, modifierVar, modifierVarFloat, modifierVarInt, customActionData);
	}

	@Override
	public boolean apply(SpellTargetLocationEvent event) {
		LivingEntity caster = event.getCaster();
		boolean check;
		if (alertCondition) check = ((IModifier) condition).apply(event);
		else check = condition.check(caster, event.getTargetLocation());
		if (negated) check = !check;
		return type.apply(event, check, modifierVar, modifierVarFloat, modifierVarInt, customActionData);
	}

	@Override
	public boolean apply(MagicSpellsGenericPlayerEvent event) {
		boolean check = condition.check(event.getPlayer());
		if (negated) check = !check;
		return type.apply(event, check, modifierVar, modifierVarFloat, modifierVarInt, customActionData);
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		boolean check = condition.check(livingEntity);
		if (negated) check = !check;
		if (!check && type == ModifierType.REQUIRED) return false;
		if (check && type == ModifierType.DENIED) return false;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity entity) {
		boolean check = condition.check(livingEntity, entity);
		if (negated) check = !check;
		if (!check && type == ModifierType.REQUIRED) return false;
		if (check && type == ModifierType.DENIED) return false;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		boolean check = condition.check(livingEntity, location);
		if (negated) check = !check;
		if (!check && type == ModifierType.REQUIRED) return false;
		if (check && type == ModifierType.DENIED) return false;
		return true;
	}

	private static ModifierType getTypeByName(String name) {
		return ModifierType.getModifierTypeByName(name);
	}

}
