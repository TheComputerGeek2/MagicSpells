package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Projectile;

public class ProjectileManagerEnderPearl extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return EnderPearl.class;
	}
	
}
