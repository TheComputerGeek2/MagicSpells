package com.nisovin.magicjutsus.util.compat;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.bukkit.entity.Player;

public interface ExemptionAssistant {
	
	// The node objects should be the optimized nodes from optimizeNodes
	<T> T exemptRunnable(Supplier<T> runnable, Player player, Collection<?> nodes);
	
	Collection<Object> optimizeNodes(Object[] nodes);
	
	// There should be access to various sets of exemption nodes for common actions through implementations of this interface
	default Collection<?> getPainExemptions() { return Collections.emptySet(); };
	
}
