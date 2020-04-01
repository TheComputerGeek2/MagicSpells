package com.nisovin.magicjutsus.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper<E> {
	
	public static void makeAccessible(AccessibleObject accessibleObject) {
		try {
			accessibleObject.setAccessible(true);
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	// Field references
	private Map<String, Field> fields = new HashMap<>();
	
	// Reference to the target class
	private Class<? extends E> targetClass;
	public Class<? extends E> getTargetClass() { return this.targetClass; }
	
	// Due to the complexity of getting methods
	// This will map usage assigned keys rather than the method name as the key
	private Map<String, Method> methods = new HashMap<>();
	public Map<String, Method> getMethods() { return Collections.unmodifiableMap(this.methods); }
	public Method getMethod(String key) { return this.methods.get(key); }
	
	public ReflectionHelper(Class<? extends E> type, String... fields) {
		this.targetClass = type;
		for (String fieldName : fields) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				this.fields.put(fieldName, field);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Returns the old method assigned to this key
	public Method cacheMethod(String key, Method method) {
		makeAccessible(method);
		return this.methods.put(key, method);
	}
	
	public int getInt(E object, String field) {
		try {
			return this.fields.get(field).getInt(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setInt(E object, String field, int val) {
		try {
			this.fields.get(field).setInt(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public byte getByte(E object, String field) {
		try {
			return this.fields.get(field).getByte(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setByte(E object, String field, byte val) {
		try {
			this.fields.get(field).setByte(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public String getString(E object, String field) {
		try {
			return (String)this.fields.get(field).get(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setString(E object, String field, String val) {
		try {
			this.fields.get(field).set(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public Object get(E object, String field) {
		try {
			return this.fields.get(field).get(object);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void set(E object, String field, Object val) {
		try {
			this.fields.get(field).set(object, val);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public Object invoke(E object, String methodKey, Object... args) {
		try {
			Method method = this.methods.get(methodKey);
			if (method == null) return null;
			
			return method.invoke(object, args);
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
}
