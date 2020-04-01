package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Projectile;

public class ProjectileManagerFishHook extends ProjectileManager {

	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return FishHook.class;
	}

}
