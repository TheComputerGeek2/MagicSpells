package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;

public class ProjectileManagerFireball extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return Fireball.class;
	}
	
}
