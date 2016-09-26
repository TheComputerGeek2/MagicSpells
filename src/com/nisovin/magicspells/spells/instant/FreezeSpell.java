package com.nisovin.magicspells.spells.instant;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class FreezeSpell extends InstantSpell implements SpellDamageSpell {

	private int snowballs;
	private double horizSpread;
	private double vertSpread;
	private int damage;
	private String spellDamageType;
	private int slowAmount;
	private int slowDuration;
	private boolean snowballGravity;
	
	private float identifier;
	
	public FreezeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		snowballs = getConfigInt("snowballs", 15);
		horizSpread = getConfigInt("horizontal-spread", 15) / 10.0;
		vertSpread = getConfigInt("vertical-spread", 15) / 10.0;
		damage = getConfigInt("damage", 3);
		spellDamageType = getConfigString("spell-damage-type", "");
		slowAmount = getConfigInt("slow-amount", 3);
		slowDuration = getConfigInt("slow-duration", 40);
		snowballGravity = getConfigBoolean("gravity", true);
		
		identifier = (float)Math.random() * 20F;
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Random rand = new Random();
			Vector mod;
			for (int i = 0; i < snowballs; i++) {
				Snowball snowball = player.launchProjectile(Snowball.class);
				snowball.setGravity(snowballGravity);
				playSpellEffects(EffectPosition.PROJECTILE, snowball);
				playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, player.getLocation(), snowball.getLocation(), player, snowball);
				snowball.setFallDistance(identifier); // tag the snowballs
				mod = new Vector((rand.nextDouble() - .5) * horizSpread, (rand.nextDouble() - .5) * vertSpread, (rand.nextDouble() - .5) * horizSpread);
				snowball.setVelocity(snowball.getVelocity().add(mod));
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (damage <= 0 || event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) return;
		
		if (!(event.getDamager() instanceof Snowball) || event.getDamager().getFallDistance() != identifier) return;
		
		LivingEntity entity = (LivingEntity)event.getEntity();
		
		if (validTargetList.canTarget(entity)) {
			float power = 1;
			SpellTargetEvent e = new SpellTargetEvent(this, (Player)((Snowball)event.getDamager()).getShooter(), entity, power);
			Bukkit.getPluginManager().callEvent(e);
			if (e.isCancelled()) {
				event.setCancelled(true);
			} else {
				event.setDamage(damage * e.getPower());
			}
		} else {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onEntityDamage2(EntityDamageByEntityEvent event) {
		if (slowAmount <= 0 || slowDuration <= 0) return;
		
		if (!(event.getDamager() instanceof Snowball) || event.getDamager().getFallDistance() != identifier) return;
		
		((LivingEntity)event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmount), true);
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}

}
