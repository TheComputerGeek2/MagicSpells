package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

public class EntityTypeCondition extends Condition {

	private boolean player = false;
	private boolean monster = false;
	private boolean animal = false;
	private final Set<EntityType> types = new HashSet<>();
	
	@Override
	public boolean initialize(String var) {
		String[] vars = var.replace(" ", "").split(",");
		for (String v : vars) {
			switch (v.toLowerCase()) {
				case "player" -> player = true;
				case "monster" -> monster = true;
				case "animal" -> animal = true;
				default -> {
					EntityType type = MobUtil.getEntityType(v);
					if (type != null) types.add(type);
				}
			}
		}
		return player || monster || animal || !types.isEmpty();
	}

	@Override
	public boolean check(LivingEntity caster) {
		return entityType(caster);
	}
	
	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return entityType(target);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return false;
	}

	private boolean entityType(LivingEntity target) {
		if (player && target instanceof Player) return true;
		if (monster && target instanceof Monster) return true;
		if (animal && target instanceof Animals) return true;
		return types.contains(target.getType());
	}

}
