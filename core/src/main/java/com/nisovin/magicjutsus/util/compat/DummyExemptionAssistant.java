package com.nisovin.magicjutsus.util.compat;

import java.util.Collection;
import java.util.function.Supplier;

import org.bukkit.entity.Player;

public class DummyExemptionAssistant implements ExemptionAssistant {
	
	@Override
	public <T> T exemptRunnable(Supplier<T> runnable, Player player, Collection<?> nodes) {
		return runnable.get();
	}
	
	@Override
	public Collection<Object> optimizeNodes(Object[] nodes) {
		return null;
	}
	
}
