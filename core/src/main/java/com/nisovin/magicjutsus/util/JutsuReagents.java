package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import org.bukkit.inventory.ItemStack;

public class JutsuReagents {
	
	private Set<ItemStack> items;
	private int mana;
	private int health;
	private int hunger;
	private int experience;
	private int levels;
	private int durability;
	private float money;
	private Map<String, Double> variables;
	
	public JutsuReagents() {
		items = null;
		mana = 0;
		health = 0;
		hunger = 0;
		experience = 0;
		levels = 0;
		money = 0;
		variables = null;
	}
	
	public JutsuReagents(JutsuReagents other) {
		if (other.items != null) {
			items = new HashSet<>();
			other.items.forEach(item -> items.add(item.clone()));
		}
		mana = other.mana;
		health = other.health;
		hunger = other.hunger;
		experience = other.experience;
		levels = other.levels;
		money = other.money;
		if (other.variables != null) {
			variables = new HashMap<>();
			variables.putAll(other.variables);
		}
	}
	
	public Set<ItemStack> getItems() {
		return items;
	}
	
	public ItemStack[] getItemsAsArray() {
		if (items == null || items.isEmpty()) return null;
		ItemStack[] arr = new ItemStack[items.size()];
		arr = items.toArray(arr);
		return arr;
	}
	
	public void setItems(Collection<ItemStack> newItems) {
		if (newItems == null || newItems.isEmpty()) items = null;
		else items = new HashSet<>(newItems);
	}
	
	// TODO can this safely be varargs?
	public void setItems(ItemStack[] newItems) {
		if (newItems == null || newItems.length == 0) items = null;
		else items = new HashSet<>(Arrays.asList(newItems));
	}
	
	public void addItem(ItemStack item) {
		if (items == null) items = new HashSet<>();
		items.add(item);
	}
	
	public int getMana() {
		return mana;
	}
	
	public void setMana(int newMana) {
		mana = newMana;
	}
	
	public int getHealth() {
		return health;
	}
	
	public void setHealth(int newHealth) {
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
	public JutsuReagents clone() {
		JutsuReagents other = new JutsuReagents();
		if (items != null) {
			other.items = new HashSet<>();
			for (ItemStack item : items) {
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
			for (Map.Entry<String, Double> entry : variables.entrySet()) {
				other.variables.put(entry.getKey(), entry.getValue());
			}
		}
		return other;
	}
	
	public JutsuReagents multiply(float x) {
		JutsuReagents other = new JutsuReagents();
		if (items != null) {
			other.items = new HashSet<>();
			for (ItemStack item : items) {
				ItemStack i = item.clone();
				i.setAmount(Math.round(i.getAmount() * x));
				other.items.add(i);
			}
		}
		other.mana = Math.round(mana * x);
		other.health = Math.round(health * x);
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
		return "JutsuReagents:["
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
	
}
