package com.nisovin.magicspells.spells.instant;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ItemProjectileSpell extends InstantSpell {

	float speed;
	boolean vertSpeedUsed;
	float hitRadius;
	float vertSpeed;
	float yOffset;
	boolean projectileHasGravity;
	ItemStack item;
	Subspell spellOnHitEntity;
	Subspell spellOnHitGround;
	
	public ItemProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		speed = getConfigFloat("speed", 1);
		vertSpeedUsed = configKeyExists("vert-speed");
		vertSpeed = getConfigFloat("vert-speed", 0);
		hitRadius = getConfigFloat("hit-radius", 1);
		yOffset = getConfigFloat("y-offset", 0);
		projectileHasGravity = getConfigBoolean("gravity", true);
		
		if (configKeyExists("spell-on-hit-entity")) {
			spellOnHitEntity = new Subspell(getConfigString("spell-on-hit-entity", ""));
		}
		if (configKeyExists("spell-on-hit-ground")) {
			spellOnHitGround = new Subspell(getConfigString("spell-on-hit-ground", ""));
		}
		
		item = Util.getItemStackFromString(getConfigString("item", "iron_sword"));
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (spellOnHitEntity != null && !spellOnHitEntity.process()) {
			spellOnHitEntity = null;
			MagicSpells.error("Invalid spell-on-hit-entity for " + internalName);
		}
		if (spellOnHitGround != null && !spellOnHitGround.process()) {
			spellOnHitGround = null;
			MagicSpells.error("Invalid spell-on-hit-ground for " + internalName);
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new ItemProjectile(player, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class ItemProjectile implements Runnable {
		
		Player caster;
		float power;
		Item entity;
		Location lastLocation;
		Vector vel;
		int taskId;
		int groundCount = 0;
		
		public ItemProjectile(Player caster, float power) {
			this.caster = caster;
			this.power = power;
			Location location = caster.getEyeLocation().add(0, yOffset, 0);
			location.setPitch(0f);
			if (vertSpeedUsed) {
				vel = caster.getLocation().getDirection().setY(0).multiply(speed).setY(vertSpeed);
			} else {
				vel = caster.getLocation().getDirection().multiply(speed);
			}
			entity = caster.getWorld().dropItem(location, item.clone());
			entity.setGravity(projectileHasGravity);
			playSpellEffects(EffectPosition.PROJECTILE, entity);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), entity.getLocation(), caster, entity);
			entity.teleport(location);
			entity.setPickupDelay(1000000);
			entity.setVelocity(vel);
			
			taskId = MagicSpells.scheduleRepeatingTask(this, 3, 3);
		}
		
		@Override
		public void run() {
			for (Entity e : entity.getNearbyEntities(hitRadius, hitRadius + 0.5, hitRadius)) {
				if (e instanceof LivingEntity && validTargetList.canTarget(caster, (LivingEntity)e)) {
					SpellTargetEvent event = new SpellTargetEvent(ItemProjectileSpell.this, caster, (LivingEntity)e, power);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						if (spellOnHitEntity != null) {							
							spellOnHitEntity.castAtEntity(caster, (LivingEntity)e, event.getPower());
						}
						stop();
						return;
					}
				}
			}
			if (entity.isOnGround()) {
				groundCount++;
			} else {
				groundCount = 0;
			}
			if (groundCount >= 2) {
				if (spellOnHitGround != null) {
					spellOnHitGround.castAtLocation(caster, entity.getLocation(), power);
				}
				stop();
			}
		}
		
		void stop() {
			entity.remove();
			MagicSpells.cancelTask(taskId);
		}
	}

}
