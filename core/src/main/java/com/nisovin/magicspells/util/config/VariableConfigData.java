package com.nisovin.magicspells.util.config;

public interface VariableConfigData<T> extends ConfigData<T> {

	@Override
	default boolean isConstant() {
		return false;
	}

}
