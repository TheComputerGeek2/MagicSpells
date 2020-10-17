package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable is used here
public class ShootListener extends PassiveListener {

	@Override
	public void initialize(String var) {

	}
	
	@OverridePriority
	@EventHandler
	public void onShoot(final EntityShootBowEvent event) {
		LivingEntity shooter = event.getEntity();

		if (!hasSpell(shooter)) return;
		if (!canTrigger(shooter)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(shooter, event.getForce());
		if (!cancelDefaultAction(casted)) return;
		event.setCancelled(true);
		event.getProjectile().remove();
	}
	
}
