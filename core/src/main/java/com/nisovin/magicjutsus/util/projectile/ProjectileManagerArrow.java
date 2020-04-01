package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;

public class ProjectileManagerArrow extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return Arrow.class;
	}
	
}
