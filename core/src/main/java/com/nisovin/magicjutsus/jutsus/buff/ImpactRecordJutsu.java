package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;

public class ImpactRecordJutsu extends BuffJutsu {
	
	private Set<UUID> recorders;

	private String variableName;
	private JutsuFilter recordFilter;

	private boolean recordCancelled;
	
	public ImpactRecordJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		variableName = getConfigString("variable-name", null);
		recordFilter = JutsuFilter.fromConfig(config, "jutsus." + internalName + ".filter");
		recordCancelled = getConfigBoolean("record-cancelled", false);

		recorders = new HashSet<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (variableName == null || MagicJutsus.getVariableManager().getVariable(variableName) == null) {
			MagicJutsus.error("ImpactRecordJutsu '" + internalName + "' has an invalid variable-name defined!");
			variableName = null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		recorders.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return recorders.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		recorders.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		recorders.clear();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJutsuTarget(JutsuTargetEvent event) {
		if (event.isCancelled() && !recordCancelled) return;
		
		LivingEntity target = event.getTarget();
		if (!(target instanceof Player)) return;

		Player playerTarget = (Player) target;
		if (!isActive(playerTarget)) return;
		
		Jutsu jutsu = event.getJutsu();
		if (!recordFilter.check(jutsu)) return;
		
		addUseAndChargeCost(playerTarget);
		MagicJutsus.getVariableManager().set(variableName, playerTarget, jutsu.getInternalName());
	}

}
