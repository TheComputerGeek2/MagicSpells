package com.nisovin.magicspells.util;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import org.bukkit.GameMode;
import org.bukkit.entity.*;

import java.util.*;

public class ValidTargetList {
	
	public enum TargetingElement {
		
		TARGET_SELF,
		TARGET_PLAYERS,
		TARGET_INVISIBLES,
		TARGET_NONPLAYERS,
		TARGET_MONSTERS,
		TARGET_ANIMALS,
		TARGET_NONLIVING_ENTITIES,
		TARGET_MOUNTS,
		TARGET_PASSENGERS,
		TARGET_CASTER_MOUNT,
		TARGET_CASTER_PASSENGER,
		TARGET_ENTITY_TARGET
		
	}

	private Set<EntityType> types = new HashSet<>();

	private boolean targetSelf = false;
	private boolean targetAnimals = false;
	private boolean targetPlayers = false;
	private boolean targetMonsters = false;
	private boolean targetInvisibles = false;
	private boolean targetNonPlayers = false;
	private boolean targetNonLivingEntities = false; // This will be kept as false for now during restructuring
	private boolean targetMounts = false;
	private boolean targetPassengers = false;
	private boolean targetCasterMount = false;
	private boolean targetCasterPassenger = false;
	private boolean targetEntityTarget = false;

	public ValidTargetList(Spell spell, String list) {
		if (list != null) {
			String[] ss = list.replace(" ", "").split(",");
			init(spell, Arrays.asList(ss));
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
			case TARGET_MOUNTS:
				targetMounts = value;
				break;
			case TARGET_PASSENGERS:
				targetPassengers = value;
				break;
			case TARGET_CASTER_MOUNT:
				targetCasterMount = value;
				break;
			case TARGET_CASTER_PASSENGER:
				targetCasterPassenger = value;
				break;
			case TARGET_ENTITY_TARGET:
				targetEntityTarget = value;
				break;
		}
	}
	
	public void enforce(TargetingElement[] elements, boolean value) {
		for (TargetingElement e : elements) {
			enforce(e, value);
		}
	}
	
	public ValidTargetList(Spell spell, List<String> list) {
		if (list != null) init(spell, list);
	}
	
	private void init(Spell spell, List<String> list) {
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
				case "mount":
				case "mounts":
					targetMounts = true;
					break;
				case "passengers":
				case "riders":
					targetPassengers = true;
					break;
				case "castermount":
				case "selfmount":
					targetCasterMount = true;
					break;
				case "casterpassenger":
				case "selfpassenger":
					targetCasterPassenger = true;
					break;
				case "entitytarget":
				case "mobtarget":
					targetEntityTarget = true;
					break;
				default:
					EntityType type = Util.getEntityType(s);
					if (type != null) types.add(type);
					else MagicSpells.error("Spell '" + spell.getInternalName() + "' has an invalid target type defined: " + s);
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
		// Todo, Make it optional for both CREATIVE and SPECTATOR to be no target
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.CREATIVE) return false;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
		if (targetSelf && target.equals(caster)) return true;
		if (!targetSelf && target.equals(caster)) return false;
		if (!targetInvisibles && targetIsPlayer && caster instanceof Player && !((Player) caster).canSee((Player) target)) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (targetNonPlayers && !targetIsPlayer) return true;
		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;
		// Lets target mounts
		if (targetPassengers && target instanceof LivingEntity && target.isInsideVehicle()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && !target.isInsideVehicle()) return true;
		if (targetCasterPassenger && target instanceof LivingEntity && target.isInsideVehicle() && target.getVehicle().equals(caster)) return true;
		if (targetCasterMount && target instanceof LivingEntity && caster.isInsideVehicle() && caster.getVehicle().equals(target)) return true;
		// Help with some mob targeting, this SPECIFICALLY targets the mobs ai target
		if (targetEntityTarget && (caster instanceof Creature) && ((Creature) caster).getTarget() != null && ((Creature) caster).getTarget().equals(target)) return true;
		if (types.contains(target.getType())) return true;
		return false;
	}
	
	public boolean canTarget(Entity target) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		// Todo, Make it optional for both CREATIVE and SPECTATOR to be no target
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.CREATIVE) return false;
		if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
		if (targetPlayers && targetIsPlayer) return true;
		if (targetNonPlayers && !targetIsPlayer) return true;
		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;
		// Lets target mounts (Again...)
		if (targetPassengers && target instanceof LivingEntity && !target.getVehicle().isEmpty()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && target.getVehicle().isEmpty()) return true;
		if (types.contains(target.getType())) return true;
		return false;
	}

	public boolean canTarget(Entity target, boolean ignoreGameMode) {
		if (!(target instanceof LivingEntity) && !targetNonLivingEntities) return false;
		boolean targetIsPlayer = target instanceof Player;
		if (!ignoreGameMode) {
			if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.CREATIVE) return false;
			if (targetIsPlayer && ((Player) target).getGameMode() == GameMode.SPECTATOR) return false;
		}
		if (targetPlayers && targetIsPlayer) return true;
		if (targetNonPlayers && !targetIsPlayer) return true;
		if (targetMonsters && target instanceof Monster) return true;
		if (targetAnimals && target instanceof Animals) return true;
		// Lets target mounts (Again...)
		if (targetPassengers && target instanceof LivingEntity && !target.getVehicle().isEmpty()) return true;
		if (targetMounts && target instanceof LivingEntity && !target.getPassengers().isEmpty() && target.getVehicle().isEmpty()) return true;
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

	public boolean canTargetMounts() {
		return targetMounts;
	}

	public boolean canTargetPassengers() {
		return targetPassengers;
	}

	public boolean canTargetCasterMount() {
		return targetCasterMount;
	}

	public boolean canTargetCasterPassenger() {
		return targetCasterPassenger;
	}

	public boolean canTargetEntityTarget() {
		return targetEntityTarget;
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
			+ ",targetMounts=" + targetMounts
			+ ",targetPassengers=" + targetPassengers
			+ ",targetCasterMount=" + targetCasterMount
			+ ",targetCasterPassenger=" + targetCasterPassenger
			+ ",targetEntityTarget=" + targetEntityTarget
			+ ",types=" + types
			+ ",targetNonLivingEntities=" + targetNonLivingEntities
			+ ']';
	}
	
}
