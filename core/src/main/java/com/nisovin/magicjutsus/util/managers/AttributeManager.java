package com.nisovin.magicjutsus.util.managers;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Collection;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.AttributeUtil;

public class AttributeManager {

	// get attribute operation from string
	public AttributeModifier.Operation getAttributeOperation(String str) {
		return AttributeUtil.AttributeOperation.getOperation(str);
	}

	// get attribute from string
	public Attribute getAttribute(String str) {
		return AttributeUtil.AttributeType.getAttribute(str);
	}

	// add attributes to item meta
	public ItemMeta addMetaAttribute(ItemMeta meta, Attribute attribute, AttributeModifier modifier) {
		if (meta == null) throw new NullPointerException("itemMeta");
		if (attribute == null) throw new NullPointerException("attribute");
		if (modifier == null) throw new NullPointerException("modifier");
		try {
			meta.addAttributeModifier(attribute, modifier);
		} catch (IllegalArgumentException exception) {
			MagicJutsus.log("That attribute has already been applied!");
			DebugHandler.debugIllegalArgumentException(exception);
		}
		return meta;
	}

	public ItemMeta addMetaAttribute(ItemMeta meta, AttributeInfo attributeInfo) {
		return addMetaAttribute(meta, attributeInfo.getAttribute(), attributeInfo.getAttributeModifier());
	}

	// add attributes to item stack
	public void addItemAttribute(ItemStack item, Attribute attribute, AttributeModifier modifier) {
		if (item == null) throw new NullPointerException("itemStack");
		item.setItemMeta(addMetaAttribute(item.getItemMeta(), attribute, modifier));
	}

	public void addItemAttribute(ItemStack item, AttributeInfo attributeInfo) {
		if (item == null) throw new NullPointerException("itemStack");
		item.setItemMeta(addMetaAttribute(item.getItemMeta(), attributeInfo));
	}

	public void addItemAttributes(ItemStack itemStack, Set<AttributeInfo> attributes) {
		attributes.forEach(attributeInfo -> addItemAttribute(itemStack, attributeInfo));
	}

	// add attributes to the living entity
	public void addEntityAttribute(LivingEntity livingEntity, Attribute attribute, AttributeModifier modifier) {
		if (livingEntity == null) throw new NullPointerException("livingEntity");
		if (attribute == null) throw new NullPointerException("attribute");
		if (modifier == null) throw new NullPointerException("modifier");
		if (livingEntity.getAttribute(attribute) == null) throw new NullPointerException("inapplicable attribute");
		try {
			livingEntity.getAttribute(attribute).addModifier(modifier);
		} catch (IllegalArgumentException exception) {
			MagicJutsus.log("That attribute has already been applied!");
			DebugHandler.debugIllegalArgumentException(exception);
		}
	}

	public void addEntityAttribute(LivingEntity livingEntity, AttributeInfo attributeInfo) {
		addEntityAttribute(livingEntity, attributeInfo.getAttribute(), attributeInfo.getAttributeModifier());
	}

	public void addEntityAttributes(LivingEntity livingEntity, Set<AttributeInfo> attributes) {
		attributes.forEach(attributeInfo -> addEntityAttribute(livingEntity, attributeInfo));
	}

	// has attribute modifier
	public boolean hasEntityAttribute(LivingEntity entity, Attribute attribute, AttributeModifier modifier) {
		return entity.getAttribute(attribute).getModifiers().contains(modifier);
	}

	public boolean hasEntityAttribute(LivingEntity entity, AttributeInfo attributeInfo) {
		return entity.getAttribute(attributeInfo.getAttribute()).getModifiers().contains(attributeInfo.getAttributeModifier());
	}

	// get attribute and attribute modifier from string
	// - [AttributeName] [Number] [Operation]
	public AttributeInfo getAttributeInfo(String str) {
		String[] args = str.split(" ");

		if (args.length < 3) return null;

		String attributeName = args[0];

		double number;
		try {
			number = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return null;
		}

		String attributeOperation = args[2];

		Attribute attribute = getAttribute(attributeName);
		if (attribute == null) {
			MagicJutsus.error("AttributeManager has an invalid attribute defined: " + attributeName);
			return null;
		}

		AttributeModifier.Operation operation = getAttributeOperation(attributeOperation);
		if (operation == null) {
			MagicJutsus.error("AttributeManager has an invalid attribute operation defined: " + attributeOperation);
			return null;
		}

		return new AttributeInfo(attribute, new AttributeModifier("MagicJutsus " + attributeName, number, operation));
	}

	// get attribute info from string list
	public Set<AttributeInfo> getAttributes(List<String> attributes) {
		if (attributes == null || attributes.isEmpty()) return null;
		Set<AttributeInfo> attributeMap = new HashSet<>();

		for (String str : attributes) {
			AttributeInfo attributeInfo = getAttributeInfo(str);
			if (attributeInfo == null) continue;
			attributeMap.add(attributeInfo);
		}

		return attributeMap;
	}

	// clear meta attributes
	public ItemMeta removeMetaAttributeModifier(ItemMeta meta, Attribute attribute, AttributeModifier modifier) {
		if (meta == null) throw new NullPointerException("itemMeta");
		if (attribute == null) throw new NullPointerException("attribute");
		if (modifier == null) throw new NullPointerException("modifier");
		meta.removeAttributeModifier(attribute, modifier);
		return meta;
	}

	public ItemMeta clearMetaAttributeModifiers(ItemMeta meta, Attribute attribute) {
		if (meta == null) throw new NullPointerException("itemMeta");
		if (attribute == null) throw new NullPointerException("attribute");
		Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
		modifiers.forEach(modifier -> meta.removeAttributeModifier(attribute, modifier));
		return meta;
	}

	public ItemMeta clearMetaAttributeModifiers(ItemMeta meta, Set<AttributeInfo> attributeInfos) {
		for (AttributeInfo attributeInfo : attributeInfos) {
			meta = removeMetaAttributeModifier(meta, attributeInfo.getAttribute(), attributeInfo.getAttributeModifier());
		}
		return meta;
	}

	// clear item stack attributes
	public void removeItemAttributeModifier(ItemStack item, Attribute attribute, AttributeModifier modifier) {
		item.setItemMeta(removeMetaAttributeModifier(item.getItemMeta(), attribute, modifier));
	}

	public void clearItemAttributeModifiers(ItemStack item, Attribute attribute) {
		item.setItemMeta(clearMetaAttributeModifiers(item.getItemMeta(), attribute));
	}

	public void clearItemAttributeModifiers(ItemStack item, Set<AttributeInfo> attributeInfos) {
		item.setItemMeta(clearMetaAttributeModifiers(item.getItemMeta(), attributeInfos));
	}

	// clear entity attributes
	public void removeEntityAttributeModifier(LivingEntity livingEntity, Attribute attribute, AttributeModifier modifier) {
		if (livingEntity == null) throw new NullPointerException("livingEntity");
		if (attribute == null) throw new NullPointerException("attribute");
		if (modifier == null) throw new NullPointerException("modifier");
		livingEntity.getAttribute(attribute).removeModifier(modifier);
	}

	public void clearEntityAttributeModifiers(LivingEntity livingEntity, Attribute attribute) {
		if (livingEntity == null) throw new NullPointerException("livingEntity");
		if (attribute == null) throw new NullPointerException("attribute");
		Collection<AttributeModifier> modifiers = livingEntity.getAttribute(attribute).getModifiers();
		modifiers.forEach(modifier -> livingEntity.getAttribute(attribute).removeModifier(modifier));
	}

	public void clearEntityAttributeModifiers(LivingEntity livingEntity, Set<AttributeInfo> attributeInfos) {
		attributeInfos.forEach(attributeInfo -> removeEntityAttributeModifier(livingEntity, attributeInfo.getAttribute(), attributeInfo.getAttributeModifier()));
	}

	public static class AttributeInfo {

		private Attribute attribute;
		private AttributeModifier attributeModifier;

		public AttributeInfo(Attribute attribute, AttributeModifier attributeModifier) {
			this.attribute = attribute;
			this.attributeModifier = attributeModifier;
		}

		public Attribute getAttribute() {
			return attribute;
		}

		public void setAttribute(Attribute attribute) {
			this.attribute = attribute;
		}

		public AttributeModifier getAttributeModifier() {
			return attributeModifier;
		}

		public void setAttributeModifier(AttributeModifier attributeModifier) {
			this.attributeModifier = attributeModifier;
		}

	}

}
