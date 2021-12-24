package com.nisovin.magicspells.castmodifiers;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.VariableMod.VariableOwner;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomData;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.castmodifiers.customdata.CustomDataFloat;

public enum ModifierType {
	
	REQUIRED(false, "required", "require") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (!check) event.setNewAmount(event.getOldAmount());
			return check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			if (!check) event.setCancelled(true);
			return check;
		}
		
	},
	
	DENIED(false, "denied", "deny") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (check) event.setNewAmount(event.getOldAmount());
			return !check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			if (check) event.setCancelled(true);
			return !check;
		}
		
	},
	
	POWER(true, "power", "empower", "multiply") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.increasePower((CustomDataFloat.from(customData)));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (check) {
				int gain = event.getNewAmount() - event.getOldAmount();
				gain = Math.round(gain * CustomDataFloat.from(customData));
				int newAmt = event.getOldAmount() + gain;
				if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
				event.setNewAmount(newAmt);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (check) event.increasePower(CustomDataFloat.from(customData));
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}
		
	},
	
	ADD_POWER(true, "addpower", "add") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.setPower(event.getPower() + CustomDataFloat.from(customData));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (check) {
				int newAmt = event.getNewAmount() + (int) CustomDataFloat.from(customData);
				if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
				if (newAmt < 0) newAmt = 0;
				event.setNewAmount(newAmt);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (check) event.setPower(event.getPower() + CustomDataFloat.from(customData));
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}
		
	},
	
	COOLDOWN(true, "cooldown") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.setCooldown(CustomDataFloat.from(customData));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}
		
	},
	
	REAGENTS(true, "reagents") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.setReagents(event.getReagents().multiply(CustomDataFloat.from(customData)));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}
		
	},
	
	CAST_TIME(true, "casttime") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (check) event.setCastTime((int) CustomDataFloat.from(customData));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			return new CustomDataFloat(text);
		}
		
	},
	
	STOP(false, "stop") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			return !check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			return !check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			return !check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return !check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return !check;
		}
		
	},
	
	CONTINUE(false, "continue") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			return check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			return check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			return check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			return check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			return check;
		}
		
	},
	
	CAST(true, "cast") {

		class CastData extends CustomData {

			public String invalidText;

			public Subspell spell;

			@Override
			public boolean isValid() {
				return spell != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			CastData data = (CastData) customData;
			if (check && data.isValid()) {
				data.spell.cast(event.getCaster(), event.getPower());
			}
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			CastData data = (CastData) customData;
			if (check && data.isValid()) {
				data.spell.cast(event.getPlayer(), 1f);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			CastData data = (CastData) customData;
			if (check && data.isValid()) {
				if (data.spell.isTargetedEntitySpell()) data.spell.castAtEntity(event.getCaster(), event.getTarget(), event.getPower());
				else data.spell.cast(event.getCaster(), event.getPower());
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			CastData data = (CastData) customData;
			if (check && data.isValid()) {
				if (data.spell.isTargetedLocationSpell()) data.spell.castAtLocation(event.getCaster(), event.getTargetLocation(), event.getPower());
				else data.spell.cast(event.getCaster(), event.getPower());
			}
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			CastData data = (CastData) customData;
			if (check && data.isValid()) {
				data.spell.cast(event.getPlayer(), 1f);
			}
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			CastData data = new CastData();
			if (text == null) {
				data.invalidText = "No spell defined.";
				return data;
			}

			Subspell spell = new Subspell(text);
			if (spell.process()) data.spell = spell;
			else data.invalidText = "Spell '" + text + "' does not exist.";

			return data;
		}

	},
	
	CAST_INSTEAD(true, "castinstead") {

		class CustomInsteadData extends CustomData {

			public String invalidText;

			public Subspell spell;

			@Override
			public boolean isValid() {
				return spell != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			CustomInsteadData data = (CustomInsteadData) customData;
			if (check && data.isValid()) {
				data.spell.cast(event.getCaster(), event.getPower());
				event.setCancelled(true);
			}
			return !check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			CustomInsteadData data = (CustomInsteadData) customData;
			if (check && data.isValid()) {
				data.spell.cast(event.getPlayer(), 1f);
			}
			return !check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			CustomInsteadData data = (CustomInsteadData) customData;
			if (check && data.isValid()) {
				if (data.spell.isTargetedEntitySpell()) data.spell.castAtEntity(event.getCaster(), event.getTarget(), event.getPower());
				else data.spell.cast(event.getCaster(), event.getPower());
			}
			return !check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			CustomInsteadData data = (CustomInsteadData) customData;
			if (check && data.isValid()) {
				if (data.spell.isTargetedLocationSpell()) data.spell.castAtLocation(event.getCaster(), event.getTargetLocation(), event.getPower());
				else data.spell.cast(event.getCaster(), event.getPower());
				event.setCancelled(true);
			}
			return !check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			CustomInsteadData data = (CustomInsteadData) customData;
			if (check && data.isValid()) {
				if (data.spell != null) data.spell.cast(event.getPlayer(), 1f);
			}
			return !check;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			CustomInsteadData data = new CustomInsteadData();
			if (text == null) {
				data.invalidText = "No spell defined.";
				return data;
			}

			Subspell spell = new Subspell(text);
			if (spell.process()) data.spell = spell;
			else data.invalidText = "Spell '" + text + "' does not exist.";

			return data;
		}

	},

	VARIABLE_MODIFY(true, "variable") {
		
		class VariableModData extends CustomData {

			private String invalidText = "Variable action is invalid.";
			
			public VariableOwner variableOwner;
			public Variable variable;
			public VariableMod mod;

			@Override
			public boolean isValid() {
				return variable != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}

		private void modifyVariable(VariableModData data, Player caster, Player target, float power, String[] args) {
			if (!data.isValid()) return;
			boolean needsTarget = data.variableOwner == VariableOwner.TARGET || (data.mod.getVariableOwner() == VariableOwner.TARGET && !data.mod.isConstantValue());
			if (needsTarget && target == null) return;
			Player owner = data.variableOwner == VariableOwner.CASTER ? caster : target;
			double amount = data.mod.getValue(caster, target, power, args);
			Variable variable = data.variable;
			double newAmount = data.mod.getOperation().applyTo(variable.getValue(owner), amount);
			MagicSpells.getVariableManager().set(variable, owner.getName(), newAmount);
		}
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check) modifyVariable((VariableModData) customData, caster, null, event.getPower(), event.getSpellArgs());
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (check) modifyVariable((VariableModData) customData, event.getPlayer(), null, 1f, null);
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check && event.getTarget() instanceof Player target) {
				modifyVariable((VariableModData) customData, caster, target, event.getPower(), event.getSpellArgs());
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check) modifyVariable((VariableModData) customData, caster, null, event.getPower(), event.getSpellArgs());
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			if (check) modifyVariable((VariableModData) customData, event.getPlayer(), null, 1f, null);
			return true;
		}

		@Override
		public CustomData buildCustomActionData(String text) {
			//input format
			//[<caster|target>:]<variableToModify>;[=|+|*|/][-]<amount|[<caster|target>:]<modifyingVariableName>>
			VariableModData data = new VariableModData();
			if (text == null) {
				data.invalidText = "No data action data defined.";
				return data;
			}

			if (!text.contains(";")) {
				data.invalidText = "Data is invalid.";
				return data;
			}

			String[] splits = text.split(";");
			if (splits.length < 2) {
				data.invalidText = "VarMod is not defined.";
				return data;
			}

			String varData = splits[0];
			VariableOwner variableOwner = VariableOwner.CASTER;
			String variableName;
			if (varData.contains(":")) {
				String[] varDataSplits = varData.split(":");
				if (varDataSplits[0].startsWith("target")) variableOwner = VariableOwner.TARGET;
				variableName = varDataSplits[1];
			}
			else variableName = varData;

			data.variableOwner = variableOwner;
			data.mod = new VariableMod(splits[1]);
			data.variable = MagicSpells.getVariableManager().getVariable(variableName);
			if (data.variable == null) data.invalidText = "Variable does not exist.";
			return data;
		}
		
	},
	
	STRING(true, "string") {
		
		class StringData extends CustomData {

			public String invalidText;
			
			public Variable variable;
			public String value;

			@Override
			public boolean isValid() {
				return variable != null && value != null;
			}

			@Override
			public String getInvalidText() {
				return invalidText;
			}

		}
		
		private void setVariable(Player player, StringData data) {
			data.variable.parseAndSet(player, data.value);
		}
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check) setVariable(caster, (StringData) customData);
			return true;
		}
		
		@Override
		public boolean apply(ManaChangeEvent event, boolean check, CustomData customData) {
			if (check) setVariable(event.getPlayer(), (StringData) customData);
			return true;
		}
		
		@Override
		public boolean apply(SpellTargetEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check) setVariable(caster, (StringData) customData);
			return true;
		}
		
		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData) {
			if (!(event.getCaster() instanceof Player caster)) return false;
			if (check) setVariable(caster, (StringData) customData);
			return true;
		}
		
		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData) {
			if (check) setVariable(event.getPlayer(), (StringData) customData);
			return true;
		}
		
		@Override
		public CustomData buildCustomActionData(String text) {
			StringData data = new StringData();
			if (text == null || text.trim().isEmpty() || !text.contains(" ")) {
				data.invalidText = "Data is invalid.";
				return data;
			}
			
			String[] splits = text.split(" ", 2);
			data.variable = MagicSpells.getVariableManager().getVariable(splits[0]);
			if (data.variable == null) data.invalidText = "Variable does not exist.";
			data.value = splits[1];
			return data;
		}
		
	}
	
	;
	
	private final String[] keys;
	private static boolean initialized = false;
	
	private final boolean usesCustomData;
	
	ModifierType(boolean usesCustomData, String... keys) {
		this.keys = keys;
		this.usesCustomData = usesCustomData;
	}
	
	public boolean usesCustomData() {
		return usesCustomData;
	}
	
	public abstract boolean apply(SpellCastEvent event, boolean check, CustomData customData);
	public abstract boolean apply(ManaChangeEvent event, boolean check, CustomData customData);
	public abstract boolean apply(SpellTargetEvent event, boolean check, CustomData customData);
	public abstract boolean apply(SpellTargetLocationEvent event, boolean check, CustomData customData);
	public abstract boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, CustomData customData);
	
	public CustomData buildCustomActionData(String text) {
		return null;
	}
	
	static Map<String, ModifierType> nameMap;
	
	static void initialize() {
		nameMap = new HashMap<>();
		for (ModifierType type : ModifierType.values()) {
			for (String key : type.keys) {
				nameMap.put(key.toLowerCase(), type);
			}
		}
		initialized = true;
	}
	
	public static ModifierType getModifierTypeByName(String name) {
		if (!initialized) initialize();
		return nameMap.get(name.toLowerCase());
	}
	
}
