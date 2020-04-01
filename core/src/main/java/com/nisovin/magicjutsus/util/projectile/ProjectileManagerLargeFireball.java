package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.LargeFireball;

public class ProjectileManagerLargeFireball extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return LargeFireball.class;
	}
	
}
