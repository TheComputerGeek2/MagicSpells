package com.nisovin.magicspells.volatilecode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.CompatBasics;

import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.Item;
import net.minecraft.server.v1_12_R1.PacketPlayOutSetCooldown;
import net.minecraft.server.v1_12_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_12_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_12_R1.PathfinderGoalSelector;

public class VolatileCodeEnabled_1_12_R1 extends VolatileCodeEnabledNMSBase {

	public VolatileCodeEnabled_1_12_R1(MagicConfig config) {
		super(config, "1_12_R1");
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
		Attribute attr = null;
		switch (attribute) {
		case "generic.maxHealth":
			attr = Attribute.GENERIC_MAX_HEALTH;
			break;
		case "generic.followRange":
			attr = Attribute.GENERIC_FOLLOW_RANGE;
			break;
		case "generic.knockbackResistance":
			attr = Attribute.GENERIC_KNOCKBACK_RESISTANCE;
			break;
		case "generic.movementSpeed":
			attr = Attribute.GENERIC_MOVEMENT_SPEED;
			break;
		case "generic.attackDamage":
			attr = Attribute.GENERIC_ATTACK_DAMAGE;
			break;
		case "generic.attackSpeed":
			attr = Attribute.GENERIC_ATTACK_SPEED;
			break;
		case "generic.armor":
			attr = Attribute.GENERIC_ARMOR;
			break;
		case "generic.luck":
			attr = Attribute.GENERIC_LUCK;
			break;
		}

		Operation oper = null;
		if (operation == 0) {
			oper = Operation.ADD_NUMBER;
		} else if (operation == 1) {
			oper = Operation.MULTIPLY_SCALAR_1;
		} else if (operation == 2) {
			oper = Operation.ADD_SCALAR;
		}
		if (attr != null && oper != null) {
			entity.getAttribute(attr).addModifier(new AttributeModifier("MagicSpells " + attribute, amount, oper));
		}
	}

	@Override
	public void resetEntityAttributes(LivingEntity entity) {
		try {
			EntityLiving e = ((CraftLivingEntity) entity).getHandle();
			// TODO this field should be calculated only once
			Field field = EntityLiving.class.getDeclaredField("bp");
			field.setAccessible(true);
			field.set(e, null);
			e.getAttributeMap();
			Method method = null;
			Class<?> clazz = e.getClass();
			while (clazz != null) {
				try {
					method = clazz.getDeclaredMethod("initAttributes");
					break;
				} catch (NoSuchMethodException e1) {
					clazz = clazz.getSuperclass();
				}
			}
			if (method != null) {
				method.setAccessible(true);
				method.invoke(e);
			} else {
				// FIXME shouldn't catch our own exception
				throw new Exception("No method initAttributes found on " + e.getClass().getName());
			}
		} catch (Exception e) {
			MagicSpells.handleException(e);
		}
	}

	@Override
	public void removeAI(LivingEntity entity) {
		try {
			EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();

			// TODO this field should be calculated only once
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			// TODO this field should be calculated only once
			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			Set<?> list = (Set<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (Set<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {

		try {
			EntityInsentient ev = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();

			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, range, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
