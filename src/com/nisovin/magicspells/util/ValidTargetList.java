package com.nisovin.magicspells.util;

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

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;

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
	
	boolean targetSelf = false;
	boolean targetPlayers = false;
	boolean targetInvisibles = false;
	boolean targetNonPlayers = false;
	boolean targetMonsters = false;
	boolean targetAnimals = false;
	boolean targetNonLivingEntities = false; // This will be kept as false for now during restructuring
	Set<EntityType> types = new HashSet<>();
	
	public ValidTargetList(Spell spell, String list) {
		if (list != null) {
			String[] ss = list.replace(" ", "").split(",");
			init(spell, Arrays.asList(ss));
		}
	}
	
	public void enforce(TargetingElement element, boolean value) {
		switch (element) {
		case TARGET_SELF:
			this.targetSelf = value;
			break;
		case TARGET_ANIMALS:
			this.targetAnimals = value;
			break;
		case TARGET_INVISIBLES:
			this.targetInvisibles = value;
			break;
		case TARGET_MONSTERS:
			this.targetMonsters = value;
			break;
		case TARGET_NONLIVING_ENTITIES:
			this.targetNonLivingEntities = value;
			break;
		case TARGET_NONPLAYERS:
			this.targetNonPlayers = value;
			break;
		case TARGET_PLAYERS:
			this.targetPlayers = value;
			break;
		}
	}
	
	public void enforce(TargetingElement[] elements, boolean value) {
		for (TargetingElement e : elements) {
			enforce(e, value);
		}
	}
	
	public ValidTargetList(Spell spell, List<String> list) {
		if (list != null) {
			init(spell, list);
		}
	}
	
	void init(Spell spell, List<String> list) {
		for (String s : list) {
			s = s.trim();
			
			switch (s.toLowerCase()) {
				case "self":
				case "caster":
					this.targetSelf = true;
					break;
				case "player":
				case "players":
					this.targetPlayers = true;
					break;
				case "invisible":
				case "invisibles":
					this.targetInvisibles = true;
					break;
				case "nonplayer":
				case "nonplayers":
					this.targetNonPlayers = true;
					break;
				case "monster":
				case "monsters":
					this.targetMonsters = true;
					break;
				case "animal":
				case "animals":
					this.targetAnimals = true;
					break;
				default:
					EntityType type = Util.getEntityType(s);
					if (type != null) {
						this.types.add(type);
					} else {
						MagicSpells.error("Invalid target type '" + s + "' on spell '" + spell.getInternalName() + '\'');
					}
			}
		}
	}
	
	public ValidTargetList(boolean targetPlayers, boolean targetNonPlayers) {
		this.targetPlayers = targetPlayers;
		this.targetNonPlayers = targetNonPlayers;
	}
	
	public boolean canTarget(Player caster, Entity target) {
		return canTarget(caster, target, targetPlayers);
	}
	
	public boolean canTarget(Player caster, Entity target, boolean targetPlayers) {
		if (!(target instanceof LivingEntity) && !this.targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		if (targetIsPlayer && ((Player)target).getGameMode() == GameMode.CREATIVE) return false;
		if (this.targetSelf && target.equals(caster)) return true;
		if (!this.targetSelf && target.equals(caster)) return false;
		if (!this.targetInvisibles && targetIsPlayer && !caster.canSee((Player)target)) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (this.targetNonPlayers && !targetIsPlayer) return true;
		if (this.targetMonsters && target instanceof Monster) return true;
		if (this.targetAnimals && target instanceof Animals) return true;
		if (this.types.contains(target.getType())) return true;
		return false;
	}
	
	public boolean canTarget(Entity target) {
		if (!(target instanceof LivingEntity) && !this.targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		if (targetIsPlayer && ((Player)target).getGameMode() == GameMode.CREATIVE) return false;
		if (this.targetPlayers && targetIsPlayer) return true;
		if (this.targetNonPlayers && !targetIsPlayer) return true;
		if (this.targetMonsters && target instanceof Monster) return true;
		if (this.targetAnimals && target instanceof Animals) return true;
		if (this.types.contains(target.getType())) return true;
		return false;
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(Player caster, List<Entity> targets) {
		return filterTargetListCastingAsLivingEntities(caster, targets, this.targetPlayers);
	}
	
	public List<LivingEntity> filterTargetListCastingAsLivingEntities(Player caster, List<Entity> targets, boolean targetPlayers) {
		List<LivingEntity> realTargets = new ArrayList<>();
		for (Entity e : targets) {
			if (canTarget(caster, e, targetPlayers)) {
				realTargets.add((LivingEntity)e);
			}
		}
		return realTargets;
	}
	
	public boolean canTargetPlayers() {
		return this.targetPlayers;
	}

	public boolean canTargetAnimals() {
		return this.targetAnimals;
	}

	public boolean canTargetMonsters() {
		return this.targetMonsters;
	}

	public boolean canTargetNonPlayers() {
		return this.targetNonPlayers;
	}

	public boolean canTargetInvisibles() {
		return this.targetInvisibles;
	}

	public boolean canTargetSelf() {
		return this.targetSelf;
	}

	public boolean canTargetLivingEntities() {
		return this.targetNonPlayers || this.targetMonsters || this.targetAnimals;
	}

	public boolean canTargetNonLivingEntities() {
		return this.targetNonLivingEntities;
	}
	
	@Override
	public String toString() {
		return "ValidTargetList:["
			+ "targetSelf=" + this.targetSelf
			+ ",targetPlayers=" + this.targetPlayers
			+ ",targetInvisibles=" + this.targetInvisibles
			+ ",targetNonPlayers=" + this.targetNonPlayers
			+ ",targetMonsters=" + this.targetMonsters
			+ ",targetAnimals=" + this.targetAnimals
			+ ",types=" + this.types
			+ ",targetNonLivingEntities=" + this.targetNonLivingEntities
			+ ']';
	}
	
}
