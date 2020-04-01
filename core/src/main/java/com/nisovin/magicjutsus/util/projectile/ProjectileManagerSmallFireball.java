package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;

public class ProjectileManagerSmallFireball extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return SmallFireball.class;
	}
	
}
