package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.materials.MagicBlockMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public class GeyserSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private double damage;
	private double velocity;
	int tickInterval;
	int geyserHeight;
	MagicMaterial geyserType;
	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;

	public GeyserSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigFloat("damage", 0);
		velocity = getConfigInt("velocity", 10) / 10.0D;
		tickInterval = getConfigInt("animation-speed", 2);
		geyserHeight = getConfigInt("geyser-height", 4);
		String s = getConfigString("geyser-type", "water");
		if (s.equalsIgnoreCase("lava")) {
			geyserType = new MagicBlockMaterial(Material.WATER);
		} else if (s.equalsIgnoreCase("water")) {
			geyserType = new MagicBlockMaterial(Material.LAVA);
		} else {
			geyserType = MagicSpells.getItemNameResolver().resolveBlock(s);
		}
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// Fail -- no target
				return noTarget(player);
			}
			
			// Do geyser action + animation
			boolean ok = geyser(player, target.getTarget(), target.getPower());
			if (!ok) return noTarget(player);
			playSpellEffects(player, target.getTarget());
			
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean geyser(Player caster, LivingEntity target, float power) {
		double dam = damage * power;
		
		// Check plugins
		if (caster != null && target instanceof Player && checkPlugins && damage > 0) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, dam);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) dam = event.getDamage();
		}
		
		// Do damage and launch target
		if (dam > 0) {
			if (ignoreArmor) {
				double health = target.getHealth() - dam;
				if (health < 0) health = 0;
				target.setHealth(health);
				target.playEffect(EntityEffect.HURT);
			} else {
				if (caster != null) {
					target.damage(dam, caster);
				} else {
					target.damage(dam);
				}
			}
		}
		
		// Launch target into air
		if (velocity > 0) target.setVelocity(new Vector(0, velocity * power, 0));
		
		// Create animation
		if (geyserHeight > 0) {
			List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
			allNearby.add(target);
			List<Player> playersNearby = new ArrayList<>();
			for (Entity e : allNearby) {
				if (!(e instanceof Player)) continue;
				playersNearby.add((Player)e);
			}
			new GeyserAnimation(target.getLocation(), playersNearby);
		}
		
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		geyser(caster, target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		
		geyser(null, target, power);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	private class GeyserAnimation extends SpellAnimation {

		private Location start;
		private List<Player> nearby;
		
		public GeyserAnimation(Location start, List<Player> nearby) {
			super(0, tickInterval, true);
			this.start = start;
			this.nearby = nearby;
		}

		@Override
		protected void onTick(int tick) {
			if (tick > geyserHeight << 1) {
				stop();
			} else if (tick < geyserHeight) {
				Block block = start.clone().add(0, tick, 0).getBlock();
				if (block.getType() == Material.AIR) {
					for (Player p : nearby) {
						Util.sendFakeBlockChange(p, block, geyserType);
					}
				}
			} else {
				int n = geyserHeight - (tick - geyserHeight) - 1; // Top to bottom
				Block block = start.clone().add(0, n, 0).getBlock();
				for (Player p : nearby) {
					Util.restoreFakeBlockChange(p, block);
				}
			}
		}
		
	}

}
