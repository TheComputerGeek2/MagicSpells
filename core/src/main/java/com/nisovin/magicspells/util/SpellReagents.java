package com.nisovin.magicspells.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

public class SpellReagents {
	
	private HashSet<ItemStack> items;
	private int mana;
	private int health;
	private int hunger;
	private int experience;
	private int levels;
	private int durability;
	private float money;
	private int copper;
	private HashMap<String, Double> variables;
	
	public SpellReagents() {
		this.items = null;
		this.mana = 0;
		this.health = 0;
		this.hunger = 0;
		this.experience = 0;
		this.levels = 0;
		this.money = 0;
		this.copper = 0;
		this.variables = null;
	}
	
	public SpellReagents(SpellReagents other) {
		if (other.items != null) {
			this.items = new HashSet<>();
			other.items.forEach(item -> this.items.add(item.clone()));
		}
		this.mana = other.mana;
		this.health = other.health;
		this.hunger = other.hunger;
		this.experience = other.experience;
		this.levels = other.levels;
		this.money = other.money;
		this.copper = other.copper;
		if (other.variables != null) {
			this.variables = new HashMap<>();
			this.variables.putAll(other.variables);
		}
	}
	
	public HashSet<ItemStack> getItems() {
		return this.items;
	}
	
	public ItemStack[] getItemsAsArray() {
		if (this.items == null || this.items.isEmpty()) return null;
		ItemStack[] arr = new ItemStack[this.items.size()];
		arr = this.items.toArray(arr);
		return arr;
	}
	
	public void setItems(Collection<ItemStack> newItems) {
		if (newItems == null || newItems.isEmpty()) {
			this.items = null;
		} else {
			this.items = new HashSet<>(newItems);
		}
	}
	
	// TODO can this safely be varargs?
	public void setItems(ItemStack[] newItems) {
		if (newItems == null || newItems.length == 0) {
			this.items = null;
		} else {
			this.items = new HashSet<>(Arrays.asList(newItems));
		}
	}
	
	public void addItem(ItemStack item) {
		if (this.items == null) this.items = new HashSet<>();
		this.items.add(item);
	}
	
	public int getMana() {
		return this.mana;
	}
	
	public void setMana(int newMana) {
		this.mana = newMana;
	}
	
	public int getHealth() {
		return this.health;
	}
	
	public void setHealth(int newHealth) {
		this.health = newHealth;
	}
	
	public int getHunger() {
		return this.hunger;
	}
	
	public void setHunger(int newHunger) {
		this.hunger = newHunger;
	}
	
	public int getExperience() {
		return this.experience;
	}
	
	public void setExperience(int newExperience) {
		this.experience = newExperience;
	}
	
	public int getLevels() {
		return this.levels;
	}
	
	public void setLevels(int newLevels) {
		this.levels = newLevels;
	}
	
	public int getDurability() {
		return this.durability;
	}
	
	public void setDurability(int newDurability) {
		this.durability = newDurability;
	}
	
	public float getMoney() {
		return this.money;
	}
	
	public void setMoney(float newMoney) {
		this.money = newMoney;
	}

	public int getCopper() {
		return this.copper;
	}

	public void setCopper(int newCopper) {
		this.copper = newCopper;
	}

	public HashMap<String, Double> getVariables() {
		return this.variables;
	}
	
	public void addVariable(String var, double val) {
		if (this.variables == null) this.variables = new HashMap<>();
		this.variables.put(var, val);
	}
	
	public void setVariables(Map<String, Double> newVariables) {
		if (newVariables == null || newVariables.isEmpty()) {
			this.variables = null;
		} else {
			this.variables = new HashMap<>();
			this.variables.putAll(newVariables);
		}
	}
	
	@Override
	public SpellReagents clone() {
		SpellReagents other = new SpellReagents();
		if (this.items != null) {
			other.items = new HashSet<>();
			for (ItemStack item : this.items) {
				other.items.add(item.clone());
			}
		}
		other.mana = this.mana;
		other.health = this.health;
		other.hunger = this.hunger;
		other.experience = this.experience;
		other.levels = this.levels;
		other.durability = this.durability;
		other.money = this.money;
		other.copper = this.copper;
		if (this.variables != null) {
			other.variables = new HashMap<>();
			for (Map.Entry<String, Double> entry : this.variables.entrySet()) {
				other.variables.put(entry.getKey(), entry.getValue());
			}
		}
		return other;
	}
	
	public SpellReagents multiply(float x) {
		SpellReagents other = new SpellReagents();
		if (this.items != null) {
			other.items = new HashSet<>();
			for (ItemStack item : this.items) {
				ItemStack i = item.clone();
				i.setAmount(Math.round(i.getAmount() * x));
				other.items.add(i);
			}
		}
		other.mana = Math.round(this.mana * x);
		other.health = Math.round(this.health * x);
		other.hunger = Math.round(this.hunger * x);
		other.experience = Math.round(this.experience * x);
		other.levels = Math.round(this.levels * x);
		other.durability = Math.round(this.durability * x);
		other.money = this.money * x;
		other.copper = Math.round(this.copper * x);
		if (this.variables != null) {
			other.variables = new HashMap<>();
			for (Map.Entry<String, Double> entry : this.variables.entrySet()) {
				other.variables.put(entry.getKey(), entry.getValue() * x);
			}
		}
		return other;
	}
	
	@Override
	public String toString() {
		return "SpellReagents:["
			+ "items=" + this.items
			+ ",mana=" + this.mana
			+ ",health=" + this.health
			+ ",hunger=" + this.hunger
			+ ",experience=" + this.experience
			+ ",levels=" + this.levels
			+ ",durability=" + this.durability
			+ ",money=" + this.money
			+ ",copper=" + this.copper
			+ ",variables=" + this.variables
			+ ']';
	}
	
}
