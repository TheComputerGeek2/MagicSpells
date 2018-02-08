package com.nisovin.magicspells.util.compat.nocheatplus;

import com.nisovin.magicspells.util.compat.ExemptionAssistant;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NoCheatPlusExemptionAid implements ExemptionAssistant{
	
	private Map<String, CheckType> checkNodes = null;
	
	public NoCheatPlusExemptionAid() {
		setupCheckNodes();
	}
	
	@Override
	public <T> T exemptRunnable(Supplier<T> runnable, Player player, Collection<?> nodes) {
		// Get the ones that are actually needed
		Collection<CheckType> toExempt = ((Collection<CheckType>)nodes)
			.stream()
			.filter(node -> doesntHaveYet(player, node))
			.collect(Collectors.toCollection(HashSet::new));
		
		// Exempt
		toExempt.forEach(check -> NCPExemptionManager.exemptPermanently(player, check));
		
		// Run
		T ret = runnable.get();
		
		// Unexempt
		toExempt.forEach(check -> NCPExemptionManager.unexempt(player, check));
		
		return ret;
	}
	
	@Override
	public Collection<Object> optimizeNodes(Object[] nodes) {
		if (!(nodes instanceof String[])) return null;
		Set<Object> ret = new HashSet<>();
		Arrays.stream((String[])nodes)
			.forEachOrdered(node -> ret.add(checkNodes.get(node)));
		return ret;
	}
	
	private void setupCheckNodes() {
		checkNodes = new HashMap<>();
		Arrays.stream(CheckType.values())
			.forEachOrdered(check -> checkNodes.put(check.getPermission().toString(), check));
	}
	
	private boolean doesntHaveYet(Player player, CheckType checkType) {
		return !NCPExemptionManager.isExempted(player, checkType, false);
	}
	
	@Override
	public Collection<?> getPainExemptions() {
		// TODO is the fight angle exemption needed here?
		// TODO figure out what to do about the self hit
		return EnumSet.of(
			CheckType.FIGHT_DIRECTION,
			CheckType.FIGHT_NOSWING,
			CheckType.FIGHT_REACH
		);
	}
	
}
