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
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
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

	private int removeDelay;

	private double health;
	private double hitRadius;
	private double maxDistance;
	private double verticalHitRadius;

	private float gravity;
	private float yOffset;
	private float interval;
	private float rotation;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float beamVertOffset;
	private float beamHorizOffset;

	private float beamSpread;
	private float beamVerticalSpread;
	private float beamHorizontalSpread;

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

		removeDelay = getConfigInt("remove-delay", 40);

		health = getConfigDouble("health", 2000);
		hitRadius = getConfigDouble("hit-radius", 2);
		maxDistance = getConfigDouble("max-distance", 30);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);

		gravity = getConfigFloat("gravity", 0F);
		yOffset = getConfigFloat("y-offset", 0F);
		interval = getConfigFloat("interval", 1F);
		rotation = getConfigFloat("rotation", 0F);
		rotationX = getConfigFloat("rotation-x", 0F);
		rotationY = getConfigFloat("rotation-y", 0F);
		rotationZ = getConfigFloat("rotation-z", 0F);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0F);

		beamSpread = getConfigFloat("beam-spread", 0F);
		beamVerticalSpread = getConfigFloat("beam-vertical-spread", beamSpread);
		beamHorizontalSpread = getConfigFloat("beam-horizontal-spread", beamSpread);

		small = getConfigBoolean("small", false);
		hpFix = getConfigBoolean("use-hp-fix", false);
		changePitch = getConfigBoolean("change-pitch", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");

		gravity *= -1;
		if (interval < 0.01) interval = 0.01F;
		if (yOffset != 0) relativeOffset.setY(yOffset);
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
		if (state == SpellCastState.NORMAL) {
			new BlockBeam(caster, caster.getLocation(), power, args);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		new BlockBeam(caster, caster.getLocation(), target, power, args);
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
	public boolean castAtLocation(LivingEntity livingEntity, Location location, float v, String[] args) {
		new BlockBeam(livingEntity, location, v, args);
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
		new BlockBeam(caster, from, target, power, args);
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

	public int getRemoveDelay() {
		return removeDelay;
	}

	public void setRemoveDelay(int removeDelay) {
		this.removeDelay = removeDelay;
	}

	public double getHealth() {
		return health;
	}

	public void setHealth(double health) {
		this.health = health;
	}

	public double getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(double hitRadius) {
		this.hitRadius = hitRadius;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public double getVerticalHitRadius() {
		return verticalHitRadius;
	}

	public void setVerticalHitRadius(double verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public float getGravity() {
		return gravity;
	}

	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	public float getYOffset() {
		return yOffset;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}

	public float getInterval() {
		return interval;
	}

	public void setInterval(float interval) {
		this.interval = interval;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public float getRotationX() {
		return rotationX;
	}

	public void setRotationX(float rotationX) {
		this.rotationX = rotationX;
	}

	public float getRotationY() {
		return rotationY;
	}

	public void setRotationY(float rotationY) {
		this.rotationY = rotationY;
	}

	public float getRotationZ() {
		return rotationZ;
	}

	public void setRotationZ(float rotationZ) {
		this.rotationZ = rotationZ;
	}

	public float getBeamVerticalOffset() {
		return beamVertOffset;
	}

	public void setBeamVerticalOffset(float beamVertOffset) {
		this.beamVertOffset = beamVertOffset;
	}

	public float getBeamHorizontalOffset() {
		return beamHorizOffset;
	}

	public void setBeamHorizontalOffset(float beamHorizOffset) {
		this.beamHorizOffset = beamHorizOffset;
	}

	public float getBeamSpread() {
		return beamSpread;
	}

	public void setBeamSpread(float beamSpread) {
		this.beamSpread = beamSpread;
	}

	public float getBeamVerticalSpread() {
		return beamVerticalSpread;
	}

	public void setBeamVerticalSpread(float beamVerticalSpread) {
		this.beamVerticalSpread = beamVerticalSpread;
	}

	public float getBeamHorizontalSpread() {
		return beamHorizontalSpread;
	}

	public void setBeamHorizontalSpread(float beamHorizontalSpread) {
		this.beamHorizontalSpread = beamHorizontalSpread;
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

	private class BlockBeam {

		private LivingEntity caster;
		private LivingEntity target;
		private float power;
		private Location startLoc;
		private Location currentLoc;
		private ItemStack helmet;

		private final Set<Entity> immune;
		private final List<LivingEntity> armorStandList;

		private BlockBeam(LivingEntity caster, Location from, float power, String[] args) {
			this.caster = caster;
			this.power = power;
			helmet = headItem;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);

			immune = new HashSet<>();
			armorStandList = new ArrayList<>();

			shootBeam();
		}

		private BlockBeam(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			helmet = headItem;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);

			immune = new HashSet<>();
			armorStandList = new ArrayList<>();

			shootBeam();
		}

		private void shootBeam() {
			if (helmet == null) return;
			playSpellEffects(EffectPosition.CASTER, caster);

			if (beamVertOffset != 0) startLoc.setPitch(startLoc.getPitch() - beamVertOffset);
			if (beamHorizOffset != 0) startLoc.setYaw(startLoc.getYaw() + beamHorizOffset);

			Vector startDir;
			if (target == null) startDir = startLoc.getDirection().normalize();
			else startDir = target.getLocation().toVector().subtract(startLoc.clone().toVector()).normalize();

			//apply relative offset
			Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
			startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLoc.add(startLoc.getDirection().clone().multiply(relativeOffset.getX()));
			startLoc.setY(startLoc.getY() + relativeOffset.getY());

			currentLoc = startLoc.clone();

			//apply target relative offset
			Location targetLoc = null;
			if (target != null) {
				targetLoc = target.getLocation().clone();
				startDir = targetLoc.clone().getDirection().normalize();
				horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
				targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
				targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
				targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());
			}

			Vector dir;
			if (target == null) dir = startLoc.getDirection().multiply(interval);
			else dir = targetLoc.toVector().subtract(startLoc.clone().toVector()).normalize().multiply(interval);

			if (beamVerticalSpread > 0 || beamHorizontalSpread > 0) {
				float rx = -1 + random.nextFloat() * 2;
				float ry = -1 + random.nextFloat() * 2;
				float rz = -1 + random.nextFloat() * 2;
				dir.add(new Vector(rx * beamHorizontalSpread, ry * beamVerticalSpread, rz * beamHorizontalSpread));
			}

			BoundingBox box = new BoundingBox(currentLoc, hitRadius, verticalHitRadius);

			float d = 0;
			mainLoop:
			while (d < maxDistance) {

				d += interval;
				currentLoc.add(dir);

				if (rotation != 0) Util.rotateVector(dir, rotation);
				if (gravity != 0) dir.add(new Vector(0, gravity,0));
				currentLoc.setDirection(dir);

				if (zoneManager.willFizzle(currentLoc, BlockBeamSpell.this)) {
					break;
				}

				//check block collision
				if (!isTransparent(currentLoc.getBlock())) {
					playSpellEffects(EffectPosition.DISABLED, currentLoc);
					if (groundSpell != null) groundSpell.castAtLocation(caster, currentLoc, power);
					if (stopOnHitGround) break;
				}

				double pitch = currentLoc.getPitch() * Math.PI / 180;

				ArmorStand armorStand;
				if (!small) armorStand = currentLoc.getWorld().spawn(currentLoc.clone().subtract(0, 1.7, 0), ArmorStand.class);
				else armorStand = currentLoc.getWorld().spawn(currentLoc.clone().subtract(0, 0.9, 0), ArmorStand.class);

				armorStand.getEquipment().setHelmet(helmet);
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

				playSpellEffects(EffectPosition.SPECIAL, currentLoc);

				box.setCenter(currentLoc);

				//check entities in the beam range
				for (LivingEntity e : startLoc.getWorld().getLivingEntities()) {
					if (e.equals(caster)) continue;
					if (e.isDead()) continue;
					if (immune.contains(e)) continue;
					if (!box.contains(e)) continue;
					if (validTargetList != null && !validTargetList.canTarget(e)) continue;

					SpellTargetEvent event = new SpellTargetEvent(BlockBeamSpell.this, caster, e, power);
					EventUtil.call(event);
					if (event.isCancelled()) continue;
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
			if (!zoneManager.willFizzle(currentLoc, BlockBeamSpell.this) && d >= maxDistance) {
				playSpellEffects(EffectPosition.DELAYED, currentLoc);
				if (endSpell != null) endSpell.castAtLocation(caster, currentLoc, power);
			}

			entities.add(armorStandList);

			MagicSpells.scheduleDelayedTask(() -> {
				for (LivingEntity entity : armorStandList) {
					entity.remove();
				}
				entities.remove(armorStandList);
			}, removeDelay);
		}

	}

}
