package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class GeyserSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<BlockData> geyserType;

	private final ConfigData<Double> damage;
	private final ConfigData<Double> velocity;

	private final ConfigData<Integer> geyserHeight;
	private final ConfigData<Integer> animationSpeed;

	private final ConfigData<Boolean> ignoreArmor;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> powerAffectsVelocity;
	private final ConfigData<Boolean> avoidDamageModification;

	public GeyserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigDataDouble("damage", 0);
		velocity = getConfigDataDouble("velocity", 10);

		geyserHeight = getConfigDataInt("geyser-height", 4);
		animationSpeed = getConfigDataInt("animation-speed", 2);

		ignoreArmor = getConfigDataBoolean("ignore-armor", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		powerAffectsVelocity = getConfigDataBoolean("power-affects-velocity", true);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", false);

		geyserType = getConfigDataBlockData("geyser-type", Material.WATER.createBlockData());
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();

		double damage = this.damage.get(data);
		if (powerAffectsDamage.get(data)) damage *= data.power();

		if (damage > 0) {
			boolean ignoreArmor = this.ignoreArmor.get(data);

			if (ignoreArmor && data.hasCaster() && checkPlugins.get(data)) {
				EntityDamageEvent event = createFakeDamageEvent(caster, target, DamageCause.ENTITY_ATTACK, damage);
				if (!event.callEvent()) return noTarget(data);

				if (!avoidDamageModification.get(data)) damage = event.getDamage();
			}

			SpellApplyDamageEvent spellEvent = new SpellApplyDamageEvent(this, caster, target, damage, "");
			spellEvent.callEvent();
			damage = spellEvent.getFinalDamage();

			if (ignoreArmor) {
				target.setHealth(Math.clamp(target.getHealth() - damage, 0, Util.getMaxHealth(target)));
				Util.playHurtEffect(target, caster);
			} else {
				if (data.hasCaster()) target.damage(damage, caster);
				else target.damage(damage);
			}
		}

		double velocity = this.velocity.get(data) / 10;
		if (powerAffectsVelocity.get(data)) velocity *= data.power();
		if (velocity > 0) target.setVelocity(new Vector(0, velocity, 0));

		int geyserHeight = this.geyserHeight.get(data);
		if (geyserHeight > 0) {
			List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
			allNearby.add(target);

			List<Player> playersNearby = new ArrayList<>();
			for (Entity e : allNearby) {
				if (!(e instanceof Player)) continue;
				playersNearby.add((Player) e);
			}

			int animationSpeed = this.animationSpeed.get(data);

			BlockData blockType = this.geyserType.get(data);
			new GeyserAnimation(blockType, target.getLocation(), playersNearby, animationSpeed, geyserHeight);
		}

		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private static class GeyserAnimation extends SpellAnimation {

		private final BlockData blockData;
		private final List<Player> nearby;
		private final int geyserHeight;
		private final Location start;

		private GeyserAnimation(BlockData blockData, Location start, List<Player> nearby, int animationSpeed, int geyserHeight) {
			super(0, animationSpeed, true, false);

			this.blockData = blockData;
			this.start = start;
			this.nearby = nearby;
			this.geyserHeight = geyserHeight;
		}

		@Override
		protected void onTick(int tick) {
			if (blockData == null) {
				stop();
				return;
			}

			if (tick > geyserHeight << 1) {
				stop();
				return;
			}

			if (tick < geyserHeight) {
				Block block = start.clone().add(0, tick, 0).getBlock();
				if (!block.getType().isAir()) return;
				for (Player p : nearby) p.sendBlockChange(block.getLocation(), blockData);
				return;
			}

			int n = geyserHeight - (tick - geyserHeight) - 1;
			Block block = start.clone().add(0, n, 0).getBlock();
			for (Player p : nearby) p.sendBlockChange(block.getLocation(), block.getBlockData());
		}

	}

}
