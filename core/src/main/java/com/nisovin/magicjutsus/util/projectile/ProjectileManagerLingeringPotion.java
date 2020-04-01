package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.LingeringPotion;

public class ProjectileManagerLingeringPotion extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return LingeringPotion.class;
	}
	
}
