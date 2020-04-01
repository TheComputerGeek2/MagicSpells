package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class OffsetLocationJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private Vector relativeOffset;
	private Vector absoluteOffset;
	
	private Ninjutsu jutsuToCast;
	private String jutsuToCastName;
	
	public OffsetLocationJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		absoluteOffset = getConfigVector("absolute-offset", "0,0,0");
		
		jutsuToCastName = getConfigString("jutsu", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuToCast = new Ninjutsu(jutsuToCastName);
		if (!jutsuToCast.process() || !jutsuToCast.isTargetedLocationJutsu()) {
			MagicJutsus.error("OffsetLocationJutsu '" + internalName + "' has an invalid jutsu defined!");
			jutsuToCast = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location baseTargetLocation;
			TargetInfo<LivingEntity> entityTargetInfo = getTargetedEntity(livingEntity, power);
			if (entityTargetInfo != null && entityTargetInfo.getTarget() != null) baseTargetLocation = entityTargetInfo.getTarget().getLocation();
			else baseTargetLocation = getTargetedBlock(livingEntity, power).getLocation();
			if (baseTargetLocation == null) return noTarget(livingEntity);
			Location loc = Util.applyOffsets(baseTargetLocation, relativeOffset, absoluteOffset);
			if (loc == null) return PostCastAction.ALREADY_HANDLED;

			if (jutsuToCast != null) jutsuToCast.castAtLocation(livingEntity, loc, power);
			playJutsuEffects(livingEntity, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (jutsuToCast != null) jutsuToCast.castAtLocation(caster, Util.applyOffsets(target, relativeOffset, absoluteOffset), power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}
	
}
