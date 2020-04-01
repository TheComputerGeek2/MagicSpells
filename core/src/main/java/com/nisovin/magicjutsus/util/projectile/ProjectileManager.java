package com.nisovin.magicjutsus.util.projectile;

import org.bukkit.util.Vector;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import com.nisovin.magicjutsus.MagicJutsus;

public abstract class ProjectileManager {

	public boolean canBeUsed() {
		try {
			testUsability();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	protected void testUsability() throws Exception {
		// Force the class to load to trigger exceptions
		if (getProjectileClass().getName().contains("class names cannot contain spaces")) {
			MagicJutsus.error("MagicJutsus encountered a class with spaces in its name:  " + getProjectileClass().getCanonicalName());
		}
	}
	
	public abstract Class<? extends Projectile> getProjectileClass();
	
	public Projectile launchProjectile(ProjectileSource source, Vector velocity) {
		return source.launchProjectile(getProjectileClass(), velocity);
	}
	
	public Projectile launchProjectile(ProjectileSource source) {
		return source.launchProjectile(getProjectileClass());
	}
	
}
