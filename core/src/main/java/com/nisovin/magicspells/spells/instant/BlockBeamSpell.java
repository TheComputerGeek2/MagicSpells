package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class BlockBeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final Set<List<LivingEntity>> entities;

	private ItemStack headItem;

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private ConfigData<Integer> removeDelay;

	private ConfigData<Double> health;
	private ConfigData<Double> hitRadius;
	private ConfigData<Double> maxDistance;
	private ConfigData<Double> verticalHitRadius;

	private ConfigData<Float> gravity;
	private ConfigData<Float> yOffset;
	private ConfigData<Float> interval;
	private ConfigData<Float> rotation;
	private ConfigData<Float> rotationX;
	private ConfigData<Float> rotationY;
	private ConfigData<Float> rotationZ;
	private ConfigData<Float> beamVertOffset;
	private ConfigData<Float> beamHorizOffset;

	private ConfigData<Float> beamVerticalSpread;
	private ConfigData<Float> beamHorizontalSpread;

	private boolean small;
	private boolean hpFix;
	private boolean changePitch;
	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;

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
		else {
			Material material = Util.getMaterial(item);
			if (material != null && material.isBlock()) headItem = new ItemStack(material);
			else MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid block-type defined!");
		}

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

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

		small = getConfigBoolean("small", false);
		hpFix = getConfigBoolean("use-hp-fix", false);
		changePitch = getConfigBoolean("change-pitch", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			if (!hitSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell defined!");
			hitSpell = null;
		}

		endSpell = new Subspell(endSpellName);
		if (!endSpell.process() || !endSpell.isTargetedLocationSpell()) {
			if (!endSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell-on-end defined!");
			endSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}

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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) shootBeam(caster, null, caster.getLocation(), power, args);
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		shootBeam(caster, target, caster.getLocation(), power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		shootBeam(caster, null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		shootBeam(caster, target, from, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return false;
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent e) {
		LivingEntity target = e.getTarget();
		if (target.hasMetadata("MSBlockBeam")) e.setCancelled(true);
	}

	private void shootBeam(LivingEntity caster, LivingEntity target, Location from, float power, String[] args) {
		if (headItem == null) return;

		playSpellEffects(EffectPosition.CASTER, caster);

		Location loc = from.clone();
		if (!changePitch) loc.setPitch(0);

		float beamVertOffset = this.beamVertOffset.get(caster, target, power, args);
		if (beamVertOffset != 0) loc.setPitch(loc.getPitch() - beamVertOffset);

		float beamHorizOffset = this.beamHorizOffset.get(caster, target, power, args);
		if (beamHorizOffset != 0) loc.setYaw(loc.getYaw() + beamHorizOffset);

		Vector startDir;
		if (target == null) startDir = loc.getDirection();
		else startDir = target.getLocation().toVector().subtract(loc.toVector()).normalize();

		//apply relative offset
		Vector relativeOffset;

		double yOffset = this.yOffset.get(caster, target, power, args);
		if (yOffset != 0) relativeOffset = this.relativeOffset.clone().setY(yOffset);
		else relativeOffset = this.relativeOffset;

		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
		loc.add(horizOffset.multiply(relativeOffset.getZ()));
		loc.add(loc.getDirection().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());

		float interval = this.interval.get(caster, target, power, args);
		if (interval < 0.01) interval = 0.01f;

		Vector dir;
		if (target == null) dir = loc.getDirection().multiply(interval);
		else {
			//apply target relative offset
			Location targetLoc = target.getLocation();
			Vector targetDir = targetLoc.getDirection();

			Vector targetHorizOffset = new Vector(-targetDir.getZ(), 0, targetDir.getX()).normalize();
			targetLoc.add(targetHorizOffset.multiply(targetRelativeOffset.getZ()));
			targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
			targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());

			dir = targetLoc.toVector().subtract(loc.toVector()).normalize().multiply(interval);
		}

		float beamVerticalSpread = this.beamVerticalSpread.get(caster, target, power, args);
		float beamHorizontalSpread = this.beamHorizontalSpread.get(caster, target, power, args);
		if (beamVerticalSpread > 0 || beamHorizontalSpread > 0) {
			float rx = -1 + random.nextFloat() * 2;
			float ry = -1 + random.nextFloat() * 2;
			float rz = -1 + random.nextFloat() * 2;
			dir.add(new Vector(rx * beamHorizontalSpread, ry * beamVerticalSpread, rz * beamHorizontalSpread));
		}

		double verticalHitRadius = this.verticalHitRadius.get(caster, target, power, args);
		double maxDistance = this.maxDistance.get(caster, target, power, args);
		double hitRadius = this.hitRadius.get(caster, target, power, args);
		double health = this.health.get(caster, target, power, args);

		float rotationX = this.rotationX.get(caster, target, power, args);
		float rotationY = this.rotationY.get(caster, target, power, args);
		float rotationZ = this.rotationZ.get(caster, target, power, args);
		float rotation = this.rotation.get(caster, target, power, args);
		float gravity = -this.gravity.get(caster, target, power, args);

		List<LivingEntity> armorStandList = new ArrayList<>();
		HashSet<Entity> immune = new HashSet<>();
		float d = 0;

		mainLoop:
		while (d < maxDistance) {
			d += interval;
			loc.add(dir);

			if (rotation != 0) Util.rotateVector(dir, rotation);
			if (gravity != 0) dir.add(new Vector(0, gravity, 0));
			if (rotation != 0 || gravity != 0) loc.setDirection(dir);

			if (zoneManager.willFizzle(loc, this)) break;

			//check block collision
			if (!isTransparent(loc.getBlock())) {
				playSpellEffects(EffectPosition.DISABLED, loc);
				if (groundSpell != null) groundSpell.castAtLocation(caster, loc, power);
				if (stopOnHitGround) break;
			}

			double pitch = loc.getPitch() * Math.PI / 180;

			ArmorStand armorStand;
			if (!small) armorStand = loc.getWorld().spawn(loc.clone().subtract(0, 1.7, 0), ArmorStand.class);
			else armorStand = loc.getWorld().spawn(loc.clone().subtract(0, 0.9, 0), ArmorStand.class);

			armorStand.getEquipment().setHelmet(headItem);
			armorStand.setGravity(false);
			armorStand.setVisible(false);
			armorStand.setCollidable(false);
			armorStand.setInvulnerable(true);
			armorStand.setRemoveWhenFarAway(true);
			armorStand.setHeadPose(new EulerAngle(pitch + rotationX, rotationY, rotationZ));
			armorStand.setMetadata("MSBlockBeam", new FixedMetadataValue(MagicSpells.getInstance(), "MSBlockBeam"));

			if (hpFix) {
				armorStand.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
				armorStand.setHealth(health);
			}
			armorStand.setSmall(small);
			armorStandList.add(armorStand);

			playSpellEffects(EffectPosition.SPECIAL, loc);

			//check entities in the beam range
			for (LivingEntity e : loc.getNearbyLivingEntities(hitRadius, verticalHitRadius)) {
				if (e == caster || !e.isValid() || immune.contains(e)) continue;
				if (validTargetList != null && !validTargetList.canTarget(e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(this, caster, e, power, args);
				if (!event.callEvent()) continue;

				LivingEntity entity = event.getTarget();

				if (hitSpell != null) {
					if (hitSpell.isTargetedEntitySpell()) hitSpell.castAtEntity(caster, entity, event.getPower());
					else if (hitSpell.isTargetedLocationSpell()) hitSpell.castAtLocation(caster, entity.getLocation(), event.getPower());
				}

				playSpellEffects(EffectPosition.TARGET, entity);
				playSpellEffectsTrail(caster.getLocation(), entity.getLocation());
				immune.add(e);

				if (stopOnHitEntity) break mainLoop;
			}
		}

		//end of the beam
		if (!zoneManager.willFizzle(loc, this) && d >= maxDistance) {
			playSpellEffects(EffectPosition.DELAYED, loc);
			if (endSpell != null) endSpell.castAtLocation(caster, loc, power);
		}

		entities.add(armorStandList);

		int removeDelay = this.removeDelay.get(caster, target, power, args);
		MagicSpells.scheduleDelayedTask(() -> {
			for (LivingEntity entity : armorStandList) entity.remove();
			entities.remove(armorStandList);
		}, removeDelay);
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public Vector getTargetRelativeOffset() {
		return targetRelativeOffset;
	}

	public void setTargetRelativeOffset(Vector targetRelativeOffset) {
		this.targetRelativeOffset = targetRelativeOffset;
	}

	public boolean isSmall() {
		return small;
	}

	public void setSmall(boolean small) {
		this.small = small;
	}

	public boolean shouldUseHpFix() {
		return hpFix;
	}

	public void setHpFix(boolean hpFix) {
		this.hpFix = hpFix;
	}

	public boolean shouldChangePitch() {
		return changePitch;
	}

	public void setChangePitch(boolean changePitch) {
		this.changePitch = changePitch;
	}

	public boolean shouldStopOnHitEntity() {
		return stopOnHitEntity;
	}

	public void setStopOnHitEntity(boolean stopOnHitEntity) {
		this.stopOnHitEntity = stopOnHitEntity;
	}

	public boolean shouldStopOnHitGround() {
		return stopOnHitGround;
	}

	public void setStopOnHitGround(boolean stopOnHitGround) {
		this.stopOnHitGround = stopOnHitGround;
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
