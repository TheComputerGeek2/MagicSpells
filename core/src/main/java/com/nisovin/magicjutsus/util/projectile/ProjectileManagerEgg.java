package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Egg;
import org.bukkit.entity.Projectile;

public class ProjectileManagerEgg extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return Egg.class;
	}
	
}
