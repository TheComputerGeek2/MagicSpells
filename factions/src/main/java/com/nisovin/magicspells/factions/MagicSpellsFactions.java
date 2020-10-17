package com.nisovin.magicspells.factions;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.massivecraft.factions.Rel;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.massivecore.MassivePlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.ConditionsLoadingEvent;

public class MagicSpellsFactions extends MassivePlugin {

	@Override
	public void onEnableInner() {
		registerCustomConditions();
	}
	
	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.getCaster() == null) return;
		if (!(event.getTarget() instanceof Player)) return;
		
		boolean beneficial = event.getSpell().isBeneficial();
		MPlayer caster = MPlayer.get(event.getCaster());
		MPlayer target = MPlayer.get(event.getTarget());
		
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(event.getCaster().getLocation()));
		Faction targetFaction = BoardColl.get().getFactionAt(PS.valueOf(event.getTarget().getLocation()));
		
		Rel rel = caster.getRelationTo(target);
		
		// Make only check relations if friendly fire is disabled
		if (faction == null || !faction.getFlag(MFlag.ID_FRIENDLYFIRE) || targetFaction == null || !targetFaction.getFlag(MFlag.ID_FRIENDLYFIRE)) {
			if (rel.isFriend() && !beneficial) {
				event.setCancelled(true);
			} else if (!rel.isFriend() && beneficial) {
				event.setCancelled(true);
			}
		}
		
		if (faction != null && !faction.getFlag(MFlag.ID_PVP)) event.setCancelled(true);
		if (targetFaction != null && !targetFaction.getFlag(MFlag.ID_PVP)) event.setCancelled(true);
	}
	
	@EventHandler
	public void onConditionsLoad(ConditionsLoadingEvent event) {
		registerCustomConditions();
	}
	
	public void registerCustomConditions() {
		for (Map.Entry<String, Class<? extends Condition>> entry : FactionsConditions.conditions.entrySet()) {
			MagicSpells.getConditionManager().addCondition(entry.getKey(), entry.getValue());
		}
	}
	
}
