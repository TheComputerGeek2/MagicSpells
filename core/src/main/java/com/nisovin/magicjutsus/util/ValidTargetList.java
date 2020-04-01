package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;

public class ValidTargetList {
	
	public enum TargetingElement {
		
		TARGET_SELF,
		TARGET_PLAYERS,
		TARGET_INVISIBLES,
		TARGET_NONPLAYERS,
		TARGET_MONSTERS,
		TARGET_ANIMALS,
		TARGET_NONLIVING_ENTITIES
		
	}

	private Set<EntityType> types = new HashSet<>();

	private boolean targetSelf = false;
	private boolean targetAnimals = false;
	private boolean targetPlayers = false;
	private boolean targetMonsters = false;
	private boolean targetInvisibles = false;
	private boolean targetNonPlayers = false;
	private boolean targetNonLivingEntities = false; // This will be kept as false for now during restructuring

	public ValidTargetList(Jutsu jutsu, String list) {
		if (list != null) {
			String[] ss = list.replace(" ", "").split(",");
			init(jutsu, Arrays.asList(ss));
		}
	}
	
	public void enforce(TargetingElement element, boolean value) {
		switch (element) {
			case TARGET_SELF:
				targetSelf = value;
				break;
			case TARGET_ANIMALS:
				targetAnimals = value;
				break;
			case TARGET_INVISIBLES:
				targetInvisibles = value;
				break;
			case TARGET_MONSTERS:
				targetMonsters = value;
				break;
			case TARGET_NONLIVING_ENTITIES:
				targetNonLivingEntities = value;
				break;
			case TARGET_NONPLAYERS:
				targetNonPlayers = value;
				break;
			case TARGET_PLAYERS:
				targetPlayers = value;
				break;
		}
	}
	
	public void enforce(TargetingElement[] elements, boolean value) {
		for (TargetingElement e : elements) {
			enforce(e, value);
		}
	}
	
	public ValidTargetList(Jutsu jutsu, List<String> list) {
		if (list != null) init(jutsu, list);
	}
	
	private void init(Jutsu jutsu, List<String> list) {
		for (String s : list) {
			s = s.trim();
			
			switch (s.toLowerCase()) {
				case "self":
				case "caster":
					targetSelf = true;
					break;
				case "player":
				case "players":
					targetPlayers = true;
					break;
				case "invisible":
				case "invisibles":
					targetInvisibles = true;
					break;
				case "nonplayer":
				case "nonplayers":
					targetNonPlayers = true;
					break;
				case "monster":
				case "monsters":
					targetMonsters = true;
					break;
				case "animal":
				case "animals":
					targetAnimals = true;
					break;
				default:
					EntityType type = Util.getEntityType(s);
					if (type != null) types.add(type);
					else MagicJutsus.error("Jutsu '" + jutsu.getInternalName() + "' has an invalid target type defined: " + s);
			}
		}
	}
	
	public ValidTargetList(boolean targetPlayers, boolean targetNonPlayers) {
		this.targetPlayers = targetPlayers;
		this.targetNonPlayers = targetNonPlayers;
	}
	
	public boolean canTarget(LivingEntity caster, Entity target) {
		return canTarget(caster, target, targetPlayers);
	}
	
	public boolean canTarget(LivingEntity caster, Entity target, boolean targetPlayers) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.CREATIVE) return false;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
		if (targetSelf && target.equals(caster)) return true;
		if (!targetSelf && target.equals(caster)) return false;
		if (!targetInvisibles && targetIsPlayer && caster instanceof Player && !((Player) caster).canSee((Player) target)) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (targetNonPlayers && !targetIsPlayer) return true;
		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;
		if (types.contains(target.getType())) return true;
		return false;
	}
	
	public boolean canTarget(Entity target) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.CREATIVE) return false;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (targetNonPlayers && !targetIsPlayer) return true;
		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;
		if (types.contains(target.getType())) return true;
		return false;
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(LivingEntity caster, List<Entity> targets) {
		return filterTargetListCastingAsLivingEntities(caster, targets, targetPlayers);
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(LivingEntity caster, List<Entity> targets, boolean targetPlayers) {
		List<LivingEntity> realTargets = new ArrayList<>();
		for (Entity e : targets) {
			if (canTarget(caster, e, targetPlayers)) {
				realTargets.add((LivingEntity) e);
			}
		}
		return realTargets;
	}
	
	public boolean canTargetPlayers() {
		return targetPlayers;
	}

	public boolean canTargetAnimals() {
		return targetAnimals;
	}

	public boolean canTargetMonsters() {
		return targetMonsters;
	}

	public boolean canTargetNonPlayers() {
		return targetNonPlayers;
	}

	public boolean canTargetInvisibles() {
		return targetInvisibles;
	}

	public boolean canTargetSelf() {
		return targetSelf;
	}

	public boolean canTargetLivingEntities() {
		return targetNonPlayers || targetMonsters || targetAnimals;
	}

	public boolean canTargetNonLivingEntities() {
		return targetNonLivingEntities;
	}
	
	@Override
	public String toString() {
		return "ValidTargetList:["
			+ "targetSelf=" + targetSelf
			+ ",targetPlayers=" + targetPlayers
			+ ",targetInvisibles=" + targetInvisibles
			+ ",targetNonPlayers=" + targetNonPlayers
			+ ",targetMonsters=" + targetMonsters
			+ ",targetAnimals=" + targetAnimals
			+ ",types=" + types
			+ ",targetNonLivingEntities=" + targetNonLivingEntities
			+ ']';
	}
	
}
