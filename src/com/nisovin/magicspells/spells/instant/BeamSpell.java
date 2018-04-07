package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class BeamSpell extends InstantSpell implements TargetedLocationSpell {

	double hitRadius;
	double verticalHitRadius;
	float rotation;
	float gravity;
	float beamHorizOffset;
	float beamVertOffset;
	float maxDistance;
	float interval;
	float yOffset;
	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	Vector relativeOffset;

	Subspell spell;
	Subspell endSpell;
	Subspell groundSpell;
	String endSpellName;
	String groundSpellName;
	String spellNameToCast;

	public BeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigFloat("y-offset", 0);
		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		if (yOffset != 0) relativeOffset.setY(yOffset);

		hitRadius = getConfigDouble("hit-radius", 2);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);
		gravity = getConfigFloat("gravity", 0);
		gravity *= -1;
		rotation = getConfigFloat("rotation", 0);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0);
		maxDistance = getConfigFloat("max-distance", 50);
		interval = getConfigFloat("interval", 0.25F);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		endSpellName = getConfigString("spell-on-end", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		spellNameToCast = getConfigString("spell", "");

		if (interval < 0.01) interval = 0.01F;
	}

	@Override
	public void initialize() {
		super.initialize();

		spell = new Subspell(spellNameToCast);
		if (!spell.process()) {
			MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell defined");
			spell = null;
		}

		endSpell = new Subspell(endSpellName);
		if (!endSpell.process()) {
			if (!endSpellName.isEmpty()) MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-end defined");
			endSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-hit-ground defined");
			groundSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new Beam(player, player.getLocation(), power);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player player, Location location, float v) {
		new Beam(player, location, v);
		return false;
	}

	@Override
	public boolean castAtLocation(Location location, float v) {
		new Beam(null, location, v);
		return false;
	}

	class Beam {

		Player caster;
		float power;
		Location startLoc;
		Location currentLoc;
		Set<Entity> immune;

		public Beam(Player caster, Location from, float power) {
			this.immune = new HashSet<>();
			this.startLoc = from.clone();
			this.caster = caster;
			this.power = power;

			playSpellEffects(EffectPosition.CASTER, this.caster);

			if (beamVertOffset != 0) this.startLoc.setPitch(this.startLoc.getPitch() - beamVertOffset);
			if (beamHorizOffset != 0) this.startLoc.setYaw(this.startLoc.getYaw() + beamHorizOffset);

			//apply relative offset
			Vector startDir = this.startLoc.getDirection().normalize();
			Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
			this.startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			this.startLoc.add(this.startLoc.getDirection().clone().multiply(relativeOffset.getX()));
			this.startLoc.setY(this.startLoc.getY() + relativeOffset.getY());

			this.currentLoc = this.startLoc.clone();

			Vector dir = this.startLoc.getDirection().multiply(interval);
			BoundingBox box = new BoundingBox(this.currentLoc, hitRadius, verticalHitRadius);

			float d = 0;
			mainLoop:
			while (d < maxDistance) {

				d += interval;
				this.currentLoc.add(dir);

				if (rotation != 0) Util.rotateVector(dir, rotation);
				if (gravity != 0) dir.add(new Vector(0, gravity,0));
				if (rotation != 0 || gravity != 0) this.currentLoc.setDirection(dir);

				//check block collision
				if (!isTransparent(this.currentLoc.getBlock())) {
					//play effects when beam hits a block
					playSpellEffects(EffectPosition.DISABLED, this.currentLoc);
					if (groundSpell != null && groundSpell.isTargetedLocationSpell()) groundSpell.castAtLocation(this.caster, this.currentLoc, this.power);
					if (stopOnHitGround) break;
				}

				playSpellEffects(EffectPosition.SPECIAL, this.currentLoc);

				box.setCenter(this.currentLoc);

				//check entities in the beam range
				for (LivingEntity e : this.caster.getWorld().getLivingEntities()) {
					if (e.equals(this.caster)) continue;
					if (e.isDead()) continue;
					if (this.immune.contains(e)) continue;
					if (!box.contains(e)) continue;
					if (validTargetList != null && !validTargetList.canTarget(e)) continue;

					SpellTargetEvent event = new SpellTargetEvent(BeamSpell.this, this.caster, e, power);
					EventUtil.call(event);
					if (event.isCancelled()) continue;

					if (spell != null && spell.isTargetedEntitySpell()) spell.castAtEntity(this.caster, event.getTarget(), event.getPower());
					playSpellEffects(EffectPosition.TARGET, event.getTarget());
					this.immune.add(e);
					if (stopOnHitEntity) break mainLoop;
				}
			}

			//end of the beam
			if (d >= maxDistance) {
				playSpellEffects(EffectPosition.DELAYED, this.currentLoc);
				if (endSpell != null && endSpell.isTargetedLocationSpell()) endSpell.castAtLocation(this.caster, this.currentLoc, this.power);
			}

		}
	}
}
