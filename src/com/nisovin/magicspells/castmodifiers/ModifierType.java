package com.nisovin.magicspells.castmodifiers;

import java.util.HashMap;

import com.nisovin.magicspells.variables.Variable;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.VariableMod;
import com.nisovin.magicspells.util.VariableMod.VariableOwner;

public enum ModifierType {
	
	REQUIRED(false, false, false, false, "required", "require") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (!check) event.setNewAmount(event.getOldAmount());
			return check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (!check) event.setCancelled(true);
			return check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (!check) event.setCancelled(true);
			return check;
		}
		
	},
	
	DENIED(false, false, false, false, "denied", "deny") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setNewAmount(event.getOldAmount());
			return !check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCancelled(true);
			return !check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCancelled(true);
			return !check;
		}
		
	},
	
	POWER(false, true, false, false, "power", "empower", "multiply") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.increasePower(modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				int gain = event.getNewAmount() - event.getOldAmount();
				gain = Math.round(gain * modifierVarFloat);
				int newAmt = event.getOldAmount() + gain;
				if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
				event.setNewAmount(newAmt);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.increasePower(modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}
		
	},
	
	ADD_POWER(false, true, false, false, "addpower", "add") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setPower(event.getPower() + modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				int newAmt = event.getNewAmount() + (int)modifierVarFloat;
				if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
				if (newAmt < 0) newAmt = 0;
				event.setNewAmount(newAmt);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setPower(event.getPower() + modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}
		
	},
	
	COOLDOWN(false, true, false, false, "cooldown") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCooldown(modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}
		
	},
	
	ADD_COOLDOWN(false, true, false, false, "addcooldown", "addcd", "+cd") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCooldown(event.getCooldown() + modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

	},

	MINUS_COOLDOWN(false, true, false, false, "minuscooldown", "minuscd", "-cd") {

		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCooldown(event.getCooldown() - modifierVarFloat);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

	},

	REAGENTS(false, true, false, false, "reagents") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setReagents(event.getReagents().multiply(modifierVarFloat));
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}
		
	},
	
	CAST_TIME(false, false, true, false, "casttime") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) event.setCastTime(modifierVarInt);
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return true;
		}
		
	},
	
	STOP(false, false, false, false, "stop") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return !check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return !check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return !check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return !check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return !check;
		}
		
	},
	
	CONTINUE(false, false, false, false, "continue") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return check;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return check;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return check;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return check;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			return check;
		}
		
	},
	
	CAST(true, false, false, false, "cast") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) spell.cast(event.getCaster(), event.getPower(), event.getSpellArgs());
			}
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) spell.cast(event.getPlayer(), 1, null);
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) {
					spell.cast(event.getCaster(), 1, null);
				}
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell instanceof TargetedLocationSpell) {
					((TargetedLocationSpell)spell).castAtLocation(event.getCaster(), event.getTargetLocation(), 1F);
				}
			}
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) spell.cast(event.getPlayer(), 1, null);
			}
			return true;
		}
		
	},
	
	CAST_INSTEAD(true, false, false, false, "castinstead") {
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) spell.cast(event.getCaster(), event.getPower(), event.getSpellArgs());
				event.setCancelled(true);
			}
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) {
					spell.cast(event.getPlayer(), 1, null);
				}
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) {
					if (spell instanceof TargetedEntitySpell) {
						((TargetedEntitySpell)spell).castAtEntity(event.getCaster(), event.getTarget(), 1F);
					} else {
						spell.castSpell(event.getCaster(), SpellCastState.NORMAL, 1F, null);
					}
				}
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell instanceof TargetedLocationSpell) {
					((TargetedLocationSpell)spell).castAtLocation(event.getCaster(), event.getTargetLocation(), 1F);
					event.setCancelled(true);
				}
			}
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				Spell spell = MagicSpells.getSpellByInternalName(modifierVar);
				if (spell != null) spell.cast(event.getPlayer(), 1, null);
			}
			return true;
		}
		
	},
	
	
	VARIABLE_MODIFY(false, false, false, true, "variable") {
		
		class CustomData {
			
			public VariableOwner modifiedVariableOwner;
			public String modifiedVariableName;
			public VariableMod mod;
			
			CustomData() {
				
			}
			
		}
		
		private void modifyVariable(String variableName, VariableOwner modifiedVariableOwner, Player caster, Player targetPlayer, VariableMod.Operation op, double amount) {
			Player varToModifiyOwnerPlayer = modifiedVariableOwner == VariableOwner.CASTER ? caster: targetPlayer;
			switch (op) {
			case SET:
				MagicSpells.getVariableManager().set(variableName, varToModifiyOwnerPlayer, amount);
				break;
			case ADD:
				MagicSpells.getVariableManager().modify(variableName, varToModifiyOwnerPlayer, amount);
				break;
			case MULTIPLY:
				MagicSpells.getVariableManager().multiplyBy(variableName, varToModifiyOwnerPlayer, amount);
				break;
			case DIVIDE:
				MagicSpells.getVariableManager().divideBy(variableName, varToModifiyOwnerPlayer, amount);
				break;
			}
		}
		
		boolean isDataOk(CustomData data, Player caster, Player target) {
			boolean needsTarget = data.modifiedVariableOwner == VariableOwner.TARGET || (data.mod.getVariableOwner() == VariableOwner.TARGET && !data.mod.isConstantValue());
			return !needsTarget || target != null;
		}
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				CustomData data = (CustomData)customData;
				if (isDataOk(data, event.getCaster(), null)) {
					double amount = data.mod.getValue(event.getCaster(), null);
					modifyVariable(data.modifiedVariableName, data.modifiedVariableOwner, event.getCaster(), null, data.mod.getOperation(), amount);
				}
			}
			return true;
		}

		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				CustomData data = (CustomData)customData;
				if (isDataOk(data, event.getPlayer(), null)) {
					double amount = data.mod.getValue(event.getPlayer(), null);
					modifyVariable(data.modifiedVariableName, data.modifiedVariableOwner, event.getPlayer(), null, data.mod.getOperation(), amount);
				}
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				CustomData data = (CustomData)customData;
				Player targetPlayer = event.getTarget() instanceof Player ? (Player)event.getTarget(): null;
				if (isDataOk(data, event.getCaster(), null)) {
					double amount = data.mod.getValue(event.getCaster(), targetPlayer);
					modifyVariable(data.modifiedVariableName, data.modifiedVariableOwner, event.getCaster(), targetPlayer, data.mod.getOperation(), amount);
				}
			}
			return true;
		}

		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				CustomData data = (CustomData)customData;
				if (isDataOk(data, event.getCaster(), null)) {
					double amount = data.mod.getValue(event.getCaster(), null);
					modifyVariable(data.modifiedVariableName, data.modifiedVariableOwner, event.getCaster(), null, data.mod.getOperation(), amount);
				}
			}
			return true;
		}

		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				CustomData data = (CustomData)customData;
				if (isDataOk(data, event.getPlayer(), null)) {
					double amount = data.mod.getValue(event.getPlayer(), null);
					modifyVariable(data.modifiedVariableName, data.modifiedVariableOwner, event.getPlayer(), null, data.mod.getOperation(), amount);
				}
			}
			return true;
		}
		
		@Override
		public Object buildCustomActionData(String text) {
			//input format
			//[<caster|target>:]<variableToModify>;[=|+|*|/][-]<amount|[<caster|target>:]<modifyingVariableName>>
			String[] splits = text.split(";");
			String modifiedVariableData = splits[0];
			VariableMod.VariableOwner modifiedVariableOwner;
			String modifiedVariableName;
			if (modifiedVariableData.contains(":")) {
				String[] modifiedVariableSplits = modifiedVariableData.split(":");
				if (modifiedVariableSplits[0].equalsIgnoreCase("target")) {
					modifiedVariableOwner = VariableMod.VariableOwner.TARGET;
				} else {
					modifiedVariableOwner = VariableMod.VariableOwner.CASTER;
				}
				modifiedVariableName = splits[1];
			} else {
				modifiedVariableOwner = VariableOwner.CASTER;
				modifiedVariableName = modifiedVariableData;
			}
			
			VariableMod variableModifier = new VariableMod(splits[1]);
			CustomData ret = new CustomData();
			ret.mod = variableModifier;
			ret.modifiedVariableName = modifiedVariableName;
			ret.modifiedVariableOwner = modifiedVariableOwner;
			return ret;
		}
		
	},
	
	STRING(false, false, false, true, "string") {
		
		class CustomData {
			
			public Variable variable;
			public String value;
			
		}
		
		private void setVariable(Player player, CustomData customData) {
			customData.variable.parseAndSet(player, customData.value);
		}
		
		@Override
		public boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				setVariable(event.getCaster(), (CustomData) customData);
			}
			return true;
		}
		
		@Override
		public boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				setVariable(event.getPlayer(), (CustomData) customData);
			}
			return true;
		}
		
		@Override
		public boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				setVariable(event.getCaster(), (CustomData) customData);
			}
			return true;
		}
		
		@Override
		public boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				setVariable(event.getCaster(), (CustomData) customData);
			}
			return true;
		}
		
		@Override
		public boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData) {
			if (check) {
				setVariable(event.getPlayer(), (CustomData) customData);
			}
			return true;
		}
		
		@Override
		public Object buildCustomActionData(String text) {
			if (text == null || text.trim().isEmpty() || !text.contains(" ")) throw new IllegalArgumentException("action \"string\" requires arguments.");
			
			String[] splits = text.split(" ", 2);
			Variable variable = MagicSpells.getVariableManager().getVariable(splits[0]);
			if (variable == null) throw new IllegalArgumentException(splits[0] + " is not a defined variable!");
			
			CustomData ret = new CustomData();
			ret.variable = variable;
			ret.value = splits[1];
			return ret;
		}
		
	}
	
	;
	
	private String[] keys;
	private static boolean initialized = false;
	
	private boolean usesCustomData = false;
	private boolean usesModifierVar = false;
	private boolean usesModifierVarFloat = false;
	private boolean usesModifierVarInt = false;
	
	ModifierType(boolean usesModVarString, boolean usesModVarFloat, boolean usesModVarInt, boolean usesCustomData, String... keys) {
		this.keys = keys;
		this.usesCustomData = usesCustomData;
		this.usesModifierVar = usesModVarString;
		this.usesModifierVarFloat = usesModVarFloat;
		this.usesModifierVarInt = usesModVarInt;
	}
	
	public boolean usesCustomData() {
		return usesCustomData;
	}
	
	public boolean usesModifierString() {
		return usesModifierVar;
	}
	
	public boolean usesModifierFloat() {
		return usesModifierVarFloat;
	}
	
	public boolean usesModifierInt() {
		return usesModifierVarInt;
	}
	
	public abstract boolean apply(SpellCastEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData);
	public abstract boolean apply(ManaChangeEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData);
	public abstract boolean apply(SpellTargetEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData);
	public abstract boolean apply(SpellTargetLocationEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData);
	public abstract boolean apply(MagicSpellsGenericPlayerEvent event, boolean check, String modifierVar, float modifierVarFloat, int modifierVarInt, Object customData);
	
	public Object buildCustomActionData(String text) {
		return null;
	}
	
	static HashMap<String, ModifierType> nameMap;
	
	static void initialize() {
		nameMap = new HashMap<>();
		for (ModifierType type: ModifierType.values()) {
			for (String key: type.keys) {
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
