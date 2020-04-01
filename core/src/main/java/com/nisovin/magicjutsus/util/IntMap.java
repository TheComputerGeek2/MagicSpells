package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;

public class IntMap<T> {

	private Map<T, Integer> map = new HashMap<>();
	
	public int put(T key, int value) {
		Integer prev = map.put(key, Integer.valueOf(value));
		if (prev != null) return prev.intValue();
		return 0;
	}
	
	public void set(T key, int value) {
		put(key, value);
	}
	
	public int get(T key) {
		Integer value = map.get(key);
		if (value != null) return value.intValue();
		return 0;
	}
	
	public int remove(T key) {
		Integer value = map.remove(key);
		if (value != null) return value.intValue();
		return 0;
	}
	
	public int size() {
		return map.size();
	}
	
	public boolean contains(T key) {
		return map.containsKey(key);
	}
	
	public boolean containsKey(T key) {
		return map.containsKey(key);
	}
	
	public boolean containsValue(int value) {
		return map.containsValue(Integer.valueOf(value));
	}
	
	public int increment(T key) {
		return increment(key, 1);
	}
	
	public int increment(T key, int amount) {
		int value = get(key) + amount;
		put(key, value);
		return value;
	}
	
	public int decrement(T key) {
		return decrement(key, 1);
	}
	
	public int decrement(T key, int amount) {
		int value = get(key) - amount;
		put(key, value);
		return value;
	}
	
	public int multiply(T key, int amount) {
		int value = get(key) * amount;
		put(key, value);
		return value;
	}
	
	public Set<T> keySet() {
		return map.keySet();
	}
	
	public void clear() {
		map.clear();
	}
	
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public void putAll(IntMap<? extends T> otherMap) {
		map.putAll(otherMap.map);
	}
	
	public void putAll(Map<? extends T, Integer> otherMap) {
		map.putAll(otherMap);
	}
	
	public Map<T, Integer> getIntegerMap() {
		return map;
	}
	
	@Override
	public IntMap<T> clone() {
		IntMap<T> newMap = new IntMap<>();
		newMap.putAll(map);
		return newMap;
	}
	
	public void useTreeMap() {
		Map<T, Integer> newMap = new TreeMap<>();
		newMap.putAll(map);
		map.clear();
		map = newMap;
	}
	
}
