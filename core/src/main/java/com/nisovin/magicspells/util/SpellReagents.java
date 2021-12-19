package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemData.MagicItemAttribute;

public class SpellReagents {
	
	private Set<ReagentItem> items;
	private int mana;
	private int hunger;
	private int experience;
	private int levels;
	private int durability;
	private float money;
	private double health;
	private Map<String, Double> variables;
	
	public SpellReagents() {
		items = null;
		mana = 0;
		health = 0;
		hunger = 0;
		experience = 0;
		levels = 0;
		durability = 0;
		money = 0;
		variables = null;
	}
	
	public SpellReagents(SpellReagents other) {
		if (other.items != null) {
			items = new HashSet<>();
			other.items.forEach(item -> items.add(item.clone()));
		}
		mana = other.mana;
		health = other.health;
		hunger = other.hunger;
		experience = other.experience;
		levels = other.levels;
		durability = other.durability;
		money = other.money;
		if (other.variables != null) {
			variables = new HashMap<>();
			variables.putAll(other.variables);
		}
	}
	
	public Set<ReagentItem> getItems() {
		return items;
	}
	
	public ReagentItem[] getItemsAsArray() {
		if (items == null || items.isEmpty()) return null;
		ReagentItem[] arr = new ReagentItem[items.size()];
		arr = items.toArray(arr);
		return arr;
	}
	
	public void setItems(Collection<ReagentItem> newItems) {
		if (newItems == null || newItems.isEmpty()) items = null;
		else items = new HashSet<>(newItems);
	}
	
	// TODO can this safely be varargs?
	public void setItems(ReagentItem[] newItems) {
		if (newItems == null || newItems.length == 0) items = null;
		else items = new HashSet<>(Arrays.asList(newItems));
	}
	
	public void addItem(ReagentItem item) {
		if (items == null) items = new HashSet<>();
		items.add(item);
	}
	
	public int getMana() {
		return mana;
	}
	
	public void setMana(int newMana) {
		mana = newMana;
	}
	
	public double getHealth() {
		return health;
	}
	
	public void setHealth(double newHealth) {
		health = newHealth;
	}
	
	public int getHunger() {
		return hunger;
	}
	
	public void setHunger(int newHunger) {
		hunger = newHunger;
	}
	
	public int getExperience() {
		return experience;
	}
	
	public void setExperience(int newExperience) {
		experience = newExperience;
	}
	
	public int getLevels() {
		return levels;
	}
	
	public void setLevels(int newLevels) {
		levels = newLevels;
	}
	
	public int getDurability() {
		return durability;
	}
	
	public void setDurability(int newDurability) {
		durability = newDurability;
	}
	
	public float getMoney() {
		return money;
	}
	
	public void setMoney(float newMoney) {
		money = newMoney;
	}
	
	public Map<String, Double> getVariables() {
		return variables;
	}
	
	public void addVariable(String var, double val) {
		if (variables == null) variables = new HashMap<>();
		variables.put(var, val);
	}
	
	public void setVariables(Map<String, Double> newVariables) {
		if (newVariables == null || newVariables.isEmpty()) variables = null;
		else {
			variables = new HashMap<>();
			variables.putAll(newVariables);
		}
	}
	
	@Override
	public SpellReagents clone() {
		SpellReagents other = new SpellReagents();
		if (items != null) {
			other.items = new HashSet<>();
			for (ReagentItem item : items) {
				other.items.add(item.clone());
			}
		}
		other.mana = mana;
		other.health = health;
		other.hunger = hunger;
		other.experience = experience;
		other.levels = levels;
		other.durability = durability;
		other.money = money;
		if (variables != null) {
			other.variables = new HashMap<>();
			other.variables.putAll(variables);
		}
		return other;
	}
	
	public SpellReagents multiply(float x) {
		SpellReagents other = new SpellReagents();
		if (items != null) {
			other.items = new HashSet<>();
			for (ReagentItem item : items) {
				ReagentItem i = item.clone();
				i.setAmount(Math.round(i.getAmount() * x));
				other.items.add(i);
			}
		}
		other.mana = Math.round(mana * x);
		other.health = health * x;
		other.hunger = Math.round(hunger * x);
		other.experience = Math.round(experience * x);
		other.levels = Math.round(levels * x);
		other.durability = Math.round(durability * x);
		other.money = money * x;
		if (variables != null) {
			other.variables = new HashMap<>();
			for (Map.Entry<String, Double> entry : variables.entrySet()) {
				other.variables.put(entry.getKey(), entry.getValue() * x);
			}
		}
		return other;
	}
	
	@Override
	public String toString() {
		return "SpellReagents:["
			+ "items=" + items
			+ ",mana=" + mana
			+ ",health=" + health
			+ ",hunger=" + hunger
			+ ",experience=" + experience
			+ ",levels=" + levels
			+ ",durability=" + durability
			+ ",money=" + money
			+ ",variables=" + variables
			+ ']';
	}

	public static class ReagentItem {

		private MagicItemData magicItemData;
		private int amount;

		public ReagentItem(MagicItemData magicItemData, int amount) {
			this.magicItemData = magicItemData.clone();
			this.magicItemData.getIgnoredAttributes().add(MagicItemAttribute.AMOUNT);

			this.amount = amount;
		}

		public MagicItemData getMagicItemData() {
			return magicItemData;
		}

		public int getAmount() {
			return amount;
		}

		public void setItemData(MagicItemData magicItemData) {
			this.magicItemData = magicItemData;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		@Override
		public ReagentItem clone() {
			return new ReagentItem(magicItemData.clone(), amount);
		}

	}
	
}
