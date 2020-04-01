package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Trident;
import org.bukkit.entity.Projectile;

public class ProjectileManagerTrident extends ProjectileManager {

	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return Trident.class;
	}

}
