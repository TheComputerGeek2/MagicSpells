package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.NamespacedKey;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellPreImpactEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class BlockBeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private static final NamespacedKey MS_BLOCK_BEAM = new NamespacedKey(MagicSpells.getInstance(), "block_beam");

	private final Set<List<LivingEntity>> entities;

	private ItemStack headItem;

	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> targetRelativeOffset;

	private final ConfigData<Integer> removeDelay;

	private final ConfigData<Double> health;
	private final ConfigData<Double> hitRadius;
	private final ConfigData<Double> maxDistance;
	private final ConfigData<Double> verticalHitRadius;

	private final ConfigData<Float> gravity;
	private final ConfigData<Float> yOffset;
	private final ConfigData<Float> interval;
	private final ConfigData<Float> rotation;
	private final ConfigData<Float> rotationX;
	private final ConfigData<Float> rotationY;
	private final ConfigData<Float> rotationZ;
	private final ConfigData<Float> beamVertOffset;
	private final ConfigData<Float> beamHorizOffset;

	private final ConfigData<Float> beamVerticalSpread;
	private final ConfigData<Float> beamHorizontalSpread;

	private final ConfigData<Boolean> small;
	private final ConfigData<Boolean> hpFix;
	private final ConfigData<Boolean> changePitch;
	private final ConfigData<Boolean> stopOnHitEntity;
	private final ConfigData<Boolean> stopOnHitGround;

	private Subspell hitSpell;
	private Subspell endSpell;
	private Subspell groundSpell;

	private final String hitSpellName;
	private final String endSpellName;
	private final String groundSpellName;

	private NoMagicZoneManager zoneManager;

	public BlockBeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();

		String item = getConfigString("block-type", "stone");
		MagicItem magicItem = MagicItems.getMagicItemFromString(item);
		if (magicItem != null && magicItem.getItemStack() != null) headItem = magicItem.getItemStack();
		else MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid 'block-type' defined!");

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.5, 0));
		targetRelativeOffset = getConfigDataVector("target-relative-offset", new Vector(0, 0.5, 0));

		removeDelay = getConfigDataInt("remove-delay", 40);

		health = getConfigDataDouble("health", 2000);
		hitRadius = getConfigDataDouble("hit-radius", 2);
		maxDistance = getConfigDataDouble("max-distance", 30);
		verticalHitRadius = getConfigDataDouble("vertical-hit-radius", 2);

		gravity = getConfigDataFloat("gravity", 0F);
		yOffset = getConfigDataFloat("y-offset", 0F);
		interval = getConfigDataFloat("interval", 1F);
		rotation = getConfigDataFloat("rotation", 0F);
		rotationX = getConfigDataFloat("rotation-x", 0F);
		rotationY = getConfigDataFloat("rotation-y", 0F);
		rotationZ = getConfigDataFloat("rotation-z", 0F);
		beamVertOffset = getConfigDataFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigDataFloat("beam-horiz-offset", 0F);

		ConfigData<Float> beamSpread = getConfigDataFloat("beam-spread", 0F);
		beamVerticalSpread = getConfigDataFloat("beam-vertical-spread", beamSpread);
		beamHorizontalSpread = getConfigDataFloat("beam-horizontal-spread", beamSpread);

		small = getConfigDataBoolean("small", false);
		hpFix = getConfigDataBoolean("use-hp-fix", false);
		changePitch = getConfigDataBoolean("change-pitch", true);
		stopOnHitEntity = getConfigDataBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigDataBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "BlockBeamSpell '" + internalName + "' has an invalid '%s' defined!";
		hitSpell = initSubspell(hitSpellName,
				error.formatted("spell"),
				true);
		endSpell = initSubspell(endSpellName,
				error.formatted("spell-on-end"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		
		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (List<LivingEntity> entityList : entities) {
			for (LivingEntity entity : entityList) {
				entity.remove();
			}
		}
		entities.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		return castAtEntityFromLocation(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		if (!data.hasCaster() || headItem == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location loc = data.location();
		if (!changePitch.get(data)) {
			loc.setPitch(0);
			data = data.location(loc);
		}

		float beamVertOffset = this.beamVertOffset.get(data);
		if (beamVertOffset != 0) {
			loc.setPitch(loc.getPitch() - beamVertOffset);
			data = data.location(loc);
		}

		float beamHorizOffset = this.beamHorizOffset.get(data);
		if (beamHorizOffset != 0) {
			loc.setYaw(loc.getYaw() + beamHorizOffset);
			data = data.location(loc);
		}

		Vector direction;
		if (!data.hasTarget()) direction = loc.getDirection();
		else {
			direction = data.target().getLocation().subtract(loc).toVector();
			direction = direction.isZero() ? loc.getDirection() : direction.normalize();
		}

		//apply relative offset
		Vector relativeOffset = this.relativeOffset.get(data);

		double yOffset = this.yOffset.get(data);
		if (yOffset == 0) yOffset = relativeOffset.getY();

		Util.applyRelativeOffset(loc, relativeOffset.setY(0));
		loc.add(0, yOffset, 0);

		float interval = this.interval.get(data);
		if (interval < 0.01) interval = 0.01f;

		if (data.hasTarget()) {
			Vector targetRelativeOffset = this.targetRelativeOffset.get(data);
			double targetYOffset = targetRelativeOffset.getY();
			Location targetLoc = data.target().getLocation();

			Util.applyRelativeOffset(targetLoc, targetRelativeOffset.setY(0));
			targetLoc.add(0, targetYOffset, 0);

			direction = targetLoc.subtract(loc).toVector();
			direction = direction.isZero() ? loc.getDirection() : direction.normalize();
		}

		loc.setDirection(direction);

		Vector step = direction.clone().multiply(interval);

		float beamVerticalSpread = this.beamVerticalSpread.get(data);
		float beamHorizontalSpread = this.beamHorizontalSpread.get(data);
		if (beamVerticalSpread > 0 || beamHorizontalSpread > 0) {
			float rx = -1 + random.nextFloat() * 2;
			float ry = -1 + random.nextFloat() * 2;
			float rz = -1 + random.nextFloat() * 2;

			step.add(new Vector(rx * beamHorizontalSpread, ry * beamVerticalSpread, rz * beamHorizontalSpread));
		}

		double verticalHitRadius = this.verticalHitRadius.get(data);
		double maxDistance = this.maxDistance.get(data);
		double hitRadius = this.hitRadius.get(data);
		double health = this.health.get(data);

		float rotationX = this.rotationX.get(data);
		float rotationY = this.rotationY.get(data);
		float rotationZ = this.rotationZ.get(data);
		float rotation = this.rotation.get(data);
		float gravity = -this.gravity.get(data);

		boolean small = this.small.get(data);
		boolean hpFix = this.hpFix.get(data);
		boolean stopOnHitEntity = this.stopOnHitEntity.get(data);
		boolean stopOnHitGround = this.stopOnHitGround.get(data);

		Predicate<Location> transparent = isTransparent(data);
		List<LivingEntity> armorStandList = new ArrayList<>();
		HashSet<Entity> immune = new HashSet<>();
		float d = 0;

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		SpellData locData = data.noTargeting();

		mainLoop:
		while (d < maxDistance) {
			d += interval;
			loc.add(step);

			if (rotation != 0 || gravity != 0) {
				if (rotation != 0) Util.rotateVector(step, rotation);
				if (gravity != 0) step.add(new Vector(0, gravity, 0));

				loc.setDirection(step);
			}

			if (zoneManager.willFizzle(loc, this)) break;

			locData = locData.location(loc);

			//check block collision
			if (!transparent.test(loc)) {
				playSpellEffects(EffectPosition.DISABLED, loc, locData);
				if (groundSpell != null) groundSpell.subcast(locData);
				if (stopOnHitGround) break;
			}

			double pitch = loc.getPitch() * Math.PI / 180;

			loc.getWorld().spawn(loc.clone().subtract(0, small ? 0.9 : 1.7, 0), ArmorStand.class, stand -> {
				stand.getEquipment().setHelmet(headItem);
				stand.setSmall(small);
				stand.setGravity(false);
				stand.setVisible(false);
				stand.setCollidable(false);
				stand.setPersistent(false);
				stand.setInvulnerable(true);
				stand.setRemoveWhenFarAway(true);
				stand.setHeadPose(new EulerAngle(pitch + rotationX, rotationY, rotationZ));
				stand.getPersistentDataContainer().set(MS_BLOCK_BEAM, PersistentDataType.BOOLEAN, true);

				if (hpFix) {
					Util.setMaxHealth(stand, health);
					stand.setHealth(health);
				}

				armorStandList.add(stand);
			});

			playSpellEffects(EffectPosition.SPECIAL, loc, locData);

			//check entities in the beam range
			for (LivingEntity e : loc.getNearbyLivingEntities(hitRadius, verticalHitRadius)) {
				if (!e.isValid() || immune.contains(e)) continue;
				if (!validTargetList.canTarget(locData.caster(), e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(this, locData, e);
				if (!event.callEvent()) continue;

				SpellData subData = event.getSpellData();
				LivingEntity subTarget = event.getTarget();

				if (hitSpell != null) {
					SpellPreImpactEvent preImpact = new SpellPreImpactEvent(hitSpell.getSpell(), BlockBeamSpell.this, subData);
					if (preImpact.callEvent()) {
						subData = preImpact.getSpellData();

						if (preImpact.getRedirected()) {
							step.multiply(-1);
							locData = subData.caster(e);
							continue mainLoop;
						} else hitSpell.subcast(subData.noLocation());
					}
				}

				playSpellEffects(EffectPosition.TARGET, subTarget, subData);
				playSpellEffectsTrail(subData.caster().getLocation(), subTarget.getLocation(), subData);
				immune.add(e);

				if (stopOnHitEntity) break mainLoop;
			}
		}

		//end of the beam
		if (!zoneManager.willFizzle(loc, this) && d >= maxDistance) {
			playSpellEffects(EffectPosition.DELAYED, loc, locData.location(loc));
			if (endSpell != null) endSpell.subcast(locData);
		}

		entities.add(armorStandList);

		int removeDelay = this.removeDelay.get(data);
		MagicSpells.scheduleDelayedTask(() -> {
			for (LivingEntity entity : armorStandList) entity.remove();
			entities.remove(armorStandList);
		}, removeDelay);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent e) {
		LivingEntity target = e.getTarget();
		if (target.getPersistentDataContainer().has(MS_BLOCK_BEAM)) e.setCancelled(true);
	}

	public Subspell getHitSpell() {
		return hitSpell;
	}

	public void setHitSpell(Subspell hitSpell) {
		this.hitSpell = hitSpell;
	}

	public Subspell getEndSpell() {
		return endSpell;
	}

	public void setEndSpell(Subspell endSpell) {
		this.endSpell = endSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

}
