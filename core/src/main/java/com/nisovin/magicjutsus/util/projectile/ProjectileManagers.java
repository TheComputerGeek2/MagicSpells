package com.nisovin.magicjutsus.util.projectile;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import com.nisovin.magicjutsus.MagicJutsus;

public class ProjectileManagers {

	private static Map<String, Class<? extends ProjectileManager>> managerKeys = new HashMap<>();
	
	private static <E extends ProjectileManager> E constructProjectileManager(Class<E> clazz) {
		E ret = null;
		try {
			ret = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException exception) {
			MagicJutsus.debug(1, "Failed to instantiate ProjectileManager from class: " + clazz.getCanonicalName());
			MagicJutsus.debug(1, "Cause of failure: " + exception.getClass().getSimpleName() + ", " + exception.getMessage());
		}
		return ret;
	}
	
	private static <E extends ProjectileManager> void tryRegister(Class<E> clazz, String... names) {
		// Clazz isn't allowed to be null
		if (clazz == null) throw new NullPointerException("clazz");
		
		// Make sure the projectile manager in question is usable
		ProjectileManager test = constructProjectileManager(clazz);
		
		// Make sure it isn't null
		if (test == null) {
			// It's null, so let's log it
			MagicJutsus.debug(1, "Could not register ProjectileManager class: " + clazz.getCanonicalName());
			MagicJutsus.debug(1, "Failure at this point is unexpected, please ensure you're using the latest version of MagicJutsus available.");
			// And we're done here
			return;
		}
		
		// Let's run the handler's own compat test
		boolean safe = test.canBeUsed();
		if (!safe) {
			// The handler's test determined incompatibility
			// Log this
			MagicJutsus.debug(1, "Could not register ProjectileManager class: " + clazz.getCanonicalName());
			MagicJutsus.debug(1, "This is because the handler detected this version of Spigot/Minecraft is unsupported for its operations.");
			// And exit
			return;
		}
		
		// Ok, NOW we can register the class
		Arrays.stream(names).forEachOrdered(name -> managerKeys.put(name.toLowerCase(), clazz));
	}
	
	static {
		tryRegister(ProjectileManagerArrow.class, "arrow");
		tryRegister(ProjectileManagerDragonFireball.class, "dragonfireball");
		tryRegister(ProjectileManagerEgg.class, "egg");
		tryRegister(ProjectileManagerEnderPearl.class, "enderpearl");
		tryRegister(ProjectileManagerFireball.class, "fireball");
		tryRegister(ProjectileManagerFishHook.class, "fishhook");
		tryRegister(ProjectileManagerLargeFireball.class, "largefireball");
		tryRegister(ProjectileManagerLingeringPotion.class, "lingeringpotion");
		tryRegister(ProjectileManagerLlamaSpit.class, "llamaspit");
		tryRegister(ProjectileManagerShulkerBullet.class, "shulkerbullet");
		tryRegister(ProjectileManagerSmallFireball.class, "smallfireball");
		tryRegister(ProjectileManagerSnowball.class, "snowball");
		tryRegister(ProjectileManagerSpectralArrow.class, "spectralarrow");
		tryRegister(ProjectileManagerSplashPotion.class, "splashpotion");
		tryRegister(ProjectileManagerThrownExpBottle.class, "thrownexpbottle");
		tryRegister(ProjectileManagerThrownPotion.class, "thrownpotion");
		tryRegister(ProjectileManagerTippedArrow.class, "tippedarrow");
		tryRegister(ProjectileManagerTrident.class, "trident");
		tryRegister(ProjectileManagerWitherSkull.class, "witherskull");
	}
	
	// Defaults to arrow since that seems safe in all versions
	public static ProjectileManager getManager(String key) {
		Class<? extends ProjectileManager> clazz = managerKeys.getOrDefault(key.toLowerCase(), ProjectileManagerArrow.class);
		return constructProjectileManager(clazz);
	}
	
}
