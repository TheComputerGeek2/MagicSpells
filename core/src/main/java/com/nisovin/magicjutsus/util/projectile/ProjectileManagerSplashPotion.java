package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;

public class ProjectileManagerSplashPotion extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return SplashPotion.class;
	}
	
}
