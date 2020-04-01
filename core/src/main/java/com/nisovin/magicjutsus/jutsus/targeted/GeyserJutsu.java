package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuAnimation;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class GeyserJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Material blockType;
	private String blockTypeName;

	private double damage;
	private double velocity;

	private int geyserHeight;
	private int animationSpeed;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;

	public GeyserJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		blockTypeName = getConfigString("geyser-type", "water").toUpperCase();
		blockType = Material.getMaterial(blockTypeName);
		if (blockType == null || !blockType.isBlock()) {
			MagicJutsus.error("GeyserJutsu '" + internalName + "' has an invalid geyser-type defined!");
			blockType = null;
		}

		damage = getConfigFloat("damage", 0);
		velocity = getConfigInt("velocity", 10) / 10.0F;

		geyserHeight = getConfigInt("geyser-height", 4);
		animationSpeed = getConfigInt("animation-speed", 2);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean ok = geyser(livingEntity, target.getTarget(), target.getPower());
			if (!ok) return noTarget(livingEntity);

			playJutsuEffects(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean geyser(LivingEntity caster, LivingEntity target, float power) {
		double dam = damage * power;
		
		if (caster != null && checkPlugins && damage > 0) {
			MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, dam);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) dam = event.getDamage();
		}
		
		if (dam > 0) {
			if (ignoreArmor) {
				double health = target.getHealth() - dam;
				if (health < 0) health = 0;
				target.setHealth(health);
				target.playEffect(EntityEffect.HURT);
			} else {
				if (caster != null) target.damage(dam, caster);
				else target.damage(dam);
			}
		}
		
		if (velocity > 0) target.setVelocity(new Vector(0, velocity * power, 0));
		
		if (geyserHeight > 0) {
			List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
			allNearby.add(target);
			List<Player> playersNearby = new ArrayList<>();
			for (Entity e : allNearby) {
				if (!(e instanceof Player)) continue;
				playersNearby.add((Player) e);
			}
			new GeyserAnimation(target.getLocation(), playersNearby);
		}
		
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		geyser(caster, target, power);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		
		geyser(null, target, power);
		playJutsuEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	private class GeyserAnimation extends JutsuAnimation {

		private Location start;
		private List<Player> nearby;
		
		private GeyserAnimation(Location start, List<Player> nearby) {
			super(0, animationSpeed, true);

			this.start = start;
			this.nearby = nearby;
		}

		@Override
		protected void onTick(int tick) {
			if (tick > geyserHeight << 1) {
				stop();
				return;
			}

			if (tick < geyserHeight) {
				Block block = start.clone().add(0, tick, 0).getBlock();
				if (!BlockUtils.isAir(block.getType())) return;
				for (Player p : nearby) p.sendBlockChange(block.getLocation(), blockType.createBlockData());
				return;
			}

			int n = geyserHeight - (tick - geyserHeight) - 1;
			Block block = start.clone().add(0, n, 0).getBlock();
			for (Player p : nearby) p.sendBlockChange(block.getLocation(), block.getBlockData());
		}
		
	}

}
