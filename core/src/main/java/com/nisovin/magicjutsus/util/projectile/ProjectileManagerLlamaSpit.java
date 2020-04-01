package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Projectile;

public class ProjectileManagerLlamaSpit extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return LlamaSpit.class;
	}
	
}
