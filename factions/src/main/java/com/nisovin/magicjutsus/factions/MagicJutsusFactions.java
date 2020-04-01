package com.nisovin.magicjutsus.factions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.ps.PS;
import com.nisovin.magicjutsus.castmodifiers.ProxyCondition;
import com.nisovin.magicjutsus.events.MagicJutsusLoadedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;

public class MagicJutsusFactions extends MassivePlugin {

	@Override
	public void onEnableInner() {
		registerCustomConditions();
	}
	
	@EventHandler
	public void onJutsuTarget(JutsuTargetEvent event) {
		if (event.getCaster() == null) return;
		if (!(event.getTarget() instanceof Player)) return;
		
		boolean beneficial = event.getJutsu().isBeneficial();
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
	public void onMagicJutsusLoad(MagicJutsusLoadedEvent event) {
		registerCustomConditions();
	}
	
	public void registerCustomConditions() {
		ProxyCondition.loadBackends(FactionsConditions.conditions);
	}
	
}
