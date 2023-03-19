package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnFireCondition extends Condition {

	private final EnumSet<Material> fireTypes = EnumSet.of(Material.FIRE, Material.SOUL_FIRE);

	@Override
	public boolean initialize(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity caster) {
		return onFire(caster, null);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return onFire(target, null);
	}
	
	@Override
	public boolean check(LivingEntity caster, Location location) {
		return onFire(caster, location);
	}

	private boolean onFire(LivingEntity target, Location location) {
		if (location != null) {
			Block b = location.getBlock();
			return fireTypes.contains(b.getType()) || fireTypes.contains(b.getRelative(BlockFace.UP).getType());
		}
		return target.getFireTicks() > 0;
	}

}
