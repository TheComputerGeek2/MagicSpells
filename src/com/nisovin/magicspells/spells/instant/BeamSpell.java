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

	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	double hitRadius;
	double verticalHitRadius;
	float rotation;
	float beamHorizOffset;
	float beamVertOffset;
	float maxDistance;
	float interval;
	float yOffset;
	String spellNameToCast;
	Subspell spell;
	Vector relativeOffset;

	public BeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigFloat("y-offset", 0);
		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		relativeOffset.setY(yOffset);
		hitRadius = getConfigDouble("hit-radius", 2);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);
		rotation = getConfigFloat("rotation", 0);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0);
		maxDistance = getConfigFloat("max-distance", 50);
		interval = getConfigFloat("interval", 0.25F);
		spellNameToCast = getConfigString("spell", "");
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		if (interval < 0.01) interval = 0.01F;
	}

	@Override
	public void initialize() {
		super.initialize();
		spell = new Subspell(spellNameToCast);
		if (!spell.process()) MagicSpells.error("Beam Spell '" + internalName + "' has invalid spell defined");
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
			this.caster = caster;
			this.startLoc = from.clone();
			this.power = power;
			BoundingBox box;

			playSpellEffects(EffectPosition.CASTER, this.caster);

			if (BeamSpell.this.beamVertOffset != 0)
				this.startLoc.setPitch(this.startLoc.getPitch() - BeamSpell.this.beamVertOffset);
			if (BeamSpell.this.beamHorizOffset != 0)
				this.startLoc.setYaw(this.startLoc.getYaw() + BeamSpell.this.beamHorizOffset);

			Vector startDir;
			Vector horizOffset;

			//apply relative offset
			startDir = from.clone().getDirection().normalize();
			horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
			this.startLoc.add(horizOffset.multiply(BeamSpell.this.relativeOffset.getZ())).getBlock().getLocation();
			this.startLoc.add(this.startLoc.getDirection().clone().multiply(BeamSpell.this.relativeOffset.getX()));
			this.startLoc.setY(this.startLoc.getY() + BeamSpell.this.relativeOffset.getY());

			this.currentLoc = this.startLoc.clone();

			Vector dir = this.startLoc.getDirection().multiply(BeamSpell.this.interval);

			float d = 0;
			mainLoop:
			while (d < BeamSpell.this.maxDistance) {

				d += BeamSpell.this.interval;
				this.currentLoc.add(dir);

				if (BeamSpell.this.rotation != 0) {
					Util.rotateVector(dir, BeamSpell.this.rotation);
					this.currentLoc.setYaw(this.currentLoc.getYaw() + BeamSpell.this.rotation);
				}

				//check block collision
				if (!isTransparent(this.currentLoc.getBlock())) {
					//play effects when beam hits a block
					playSpellEffects(EffectPosition.DISABLED, this.currentLoc);
					if (BeamSpell.this.stopOnHitGround) break;
				}

				playSpellEffects(EffectPosition.SPECIAL, this.currentLoc);

				box = new BoundingBox(this.currentLoc, BeamSpell.this.hitRadius, BeamSpell.this.verticalHitRadius);

				//check entities in the beam range
                for (LivingEntity e : this.caster.getWorld().getLivingEntities()) {
                    if (e.equals(this.caster)) continue;
                    if (e.isDead()) continue;
                    if (this.immune.contains(e)) continue;
                    if (!box.contains(e)) continue;
                    if (BeamSpell.this.validTargetList != null && !BeamSpell.this.validTargetList.canTarget(e)) continue;

                    SpellTargetEvent event = new SpellTargetEvent(BeamSpell.this, this.caster, e, power);
                    EventUtil.call(event);
                    if (event.isCancelled()) continue;

                    BeamSpell.this.spell.castAtEntity(this.caster, event.getTarget(), event.getPower());
                    playSpellEffects(EffectPosition.TARGET, event.getTarget());
                    this.immune.add(e);
                    if (BeamSpell.this.stopOnHitEntity) break mainLoop;
                }
			}

		}
	}
}
