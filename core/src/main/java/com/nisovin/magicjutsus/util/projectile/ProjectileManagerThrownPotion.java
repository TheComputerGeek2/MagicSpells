package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;

public class ProjectileManagerThrownPotion extends ProjectileManager {

	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return ThrownPotion.class;
	}

}
