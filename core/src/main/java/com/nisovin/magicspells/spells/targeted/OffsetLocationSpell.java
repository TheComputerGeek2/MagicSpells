package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class OffsetLocationSpell extends TargetedSpell implements TargetedLocationSpell {

	private Vector relativeOffset;
	private Vector absoluteOffset;

	private float forcedPitch;
	private boolean forcePitch = false;

	private Subspell spellToCast;
	private String spellToCastName;

	public OffsetLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		absoluteOffset = getConfigVector("absolute-offset", "0,0,0");

		if (configKeyExists("forced-pitch")) {
			forcedPitch = getConfigFloat("forced-pitch", 0);
			forcePitch = true;
		}

		spellToCastName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process() || !spellToCast.isTargetedLocationSpell()) {
			MagicSpells.error("OffsetLocationSpell '" + internalName + "' has an invalid spell defined!");
			spellToCast = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location baseTargetLocation;
			TargetInfo<LivingEntity> info = getTargetedEntity(caster, power, args);
			if (info.cancelled()) return PostCastAction.ALREADY_HANDLED;

			if (!info.empty()) baseTargetLocation = info.target().getLocation();
			else baseTargetLocation = getTargetedBlock(caster, power, args).getLocation();

			Location loc = null;

			if (forcePitch) {
				loc = Util.applyOffsets(baseTargetLocation.clone(), relativeOffset, absoluteOffset, forcedPitch);
			} else {
				loc = Util.applyOffsets(baseTargetLocation.clone(), relativeOffset, absoluteOffset);
			}

			if (spellToCast != null) spellToCast.castAtLocation(caster, loc, power);
			playSpellEffects(caster, loc, power, args);

			if (!info.empty()) {
				sendMessages(caster, info.target(), args);
				return PostCastAction.NO_MESSAGES;
			}
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		if (spellToCast != null) {
			if (forcePitch) {
				spellToCast.castAtLocation(caster, Util.applyOffsets(target.clone(), relativeOffset, absoluteOffset, forcedPitch), power);
			} else {
				spellToCast.castAtLocation(caster, Util.applyOffsets(target.clone(), relativeOffset, absoluteOffset), power);
			}
		}
		playSpellEffects(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

}
