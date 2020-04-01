package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class LeapJutsu extends InstantJutsu {

	private Set<UUID> jumping;

	private float rotation;
	private float upwardVelocity;
	private float forwardVelocity;

	private boolean clientOnly;
	private boolean cancelDamage;
	private boolean addVelocityInstead;

	private Ninjutsu landJutsu;
	private String landJutsuName;

	public LeapJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		jumping = new HashSet();

		rotation = getConfigFloat("rotation", 0F);
		upwardVelocity = getConfigFloat("upward-velocity", 15F) / 10F;
		forwardVelocity = getConfigFloat("forward-velocity", 40F) / 10F;

		clientOnly = getConfigBoolean("client-only", false);
		cancelDamage = getConfigBoolean("cancel-damage", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);

		landJutsuName = getConfigString("land-jutsu", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		landJutsu = new Ninjutsu(landJutsuName);
		if (!landJutsu.process()) {
			if (!landJutsuName.isEmpty()) MagicJutsus.error("LeapJutsu '" + internalName + "' has an invalid land-jutsu defined!");
			landJutsu = null;
		}
	}

	public boolean isJumping(LivingEntity livingEntity) {
		return jumping.contains(livingEntity.getUniqueId());
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Vector v = livingEntity.getLocation().getDirection();
			v.setY(0).normalize().multiply(forwardVelocity * power).setY(upwardVelocity * power);
			if (rotation != 0) Util.rotateVector(v, rotation);
			if (clientOnly && livingEntity instanceof Player) MagicJutsus.getVolatileCodeHandler().setClientVelocity((Player) livingEntity, v);
			else {
				if (!addVelocityInstead) livingEntity.setVelocity(v);
				else livingEntity.setVelocity(livingEntity.getVelocity().add(v));
			}
			jumping.add(livingEntity.getUniqueId());
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
		LivingEntity livingEntity = (LivingEntity) e.getEntity();
		if (!jumping.remove(livingEntity.getUniqueId())) return;
		if (landJutsu != null) landJutsu.cast(livingEntity, 1F);
		playJutsuEffects(EffectPosition.TARGET, livingEntity.getLocation());
		if (cancelDamage) e.setCancelled(true);
	}

}
