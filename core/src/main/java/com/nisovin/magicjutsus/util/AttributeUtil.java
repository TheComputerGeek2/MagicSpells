package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

public class AttributeUtil {

	public enum AttributeOperation {

		ADD_NUMBER("add_number", "addnumber"),
		ADD_SCALAR("add_scalar", "addscalar"),
		MULTIPLY_SCALAR_1("multiply_scalar", "multiplyscalar");

		private String[] names;

		private static Map<String, AttributeModifier.Operation> nameMap = new HashMap<>();

		AttributeOperation(String... names) {
			this.names = names;
		}

		static {
			for (AttributeOperation op : AttributeOperation.values()) {
				AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(op.name());
				if (operation == null) continue;

				nameMap.put(op.name().toLowerCase(), operation);
				for (String s : op.names) {
					nameMap.put(s.toLowerCase(), operation);
				}

			}
		}

		public AttributeModifier.Operation toBukkitOperation() {
			return nameMap.get(names[0]);
		}

		public static AttributeModifier.Operation getOperation(String operation) {
			return nameMap.get(operation.toLowerCase());
		}

	}

	public enum AttributeType {

		GENERIC_ARMOR("generic.armor", "armor"),
		GENERIC_ARMOR_TOUGHNESS("generic.armortoughness", "armortoughness", "armor_toughness"),
		GENERIC_ATTACK_DAMAGE("generic.attackdamage", "attackdamage", "attack_damage"),
		GENERIC_ATTACK_SPEED("generic.attackspeed", "attackspeed", "attack_speed"),
		GENERIC_FLYING_SPEED("generic.flyingspeed", "flyingspeed", "flying_speed"),
		GENERIC_FOLLOW_RANGE("generic.followrange", "followrange", "follow_range"),
		GENERIC_KNOCKBACK_RESISTANCE("generic.knockbackresistance", "knockbackresistance", "knockback_resistance"),
		GENERIC_LUCK("generic.luck", "luck"),
		GENERIC_MAX_HEALTH("generic.maxhealth", "maxhealth", "max_health"),
		GENERIC_MOVEMENT_SPEED("generic.movementspeed", "movementspeed", "movement_speed"),
		HORSE_JUMP_STRENGTH("horsejumpstrength", "horse_jump_strength"),
		ZOMBIE_SPAWN_REINFORCEMENTS("zombiespawnreinforcements", "zombie_spawn_reinforcements");

		private String[] names;

		private static Map<String, Attribute> nameMap = new HashMap<>();

		AttributeType(String... names) {
			this.names = names;
		}

		static {
			for (AttributeType type : AttributeType.values()) {
				Attribute attribute = Attribute.valueOf(type.name());
				if (attribute == null) continue;

				nameMap.put(type.name().toLowerCase(), attribute);
				for (String s : type.names) {
					nameMap.put(s.toLowerCase(), attribute);
				}

			}
		}

		public Attribute toBukkitAttribute() {
			return nameMap.get(names[0]);
		}

		public static Attribute getAttribute(String attribute) {
			return nameMap.get(attribute.toLowerCase());
		}

	}

}
