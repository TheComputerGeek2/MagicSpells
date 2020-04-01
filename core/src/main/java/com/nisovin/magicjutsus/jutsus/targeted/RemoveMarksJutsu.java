package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.MagicLocation;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.instant.MarkJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class RemoveMarksJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private float radius;

	private boolean pointBlank;

	private MarkJutsu markJutsu;
	private String markJutsuName;

	public RemoveMarksJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		radius = getConfigFloat("radius", 10F);

		pointBlank = getConfigBoolean("point-blank", false);

		markJutsuName = getConfigString("mark-jutsu", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(markJutsuName);
		if (jutsu instanceof MarkJutsu) {
			markJutsu = (MarkJutsu) jutsu;
			return;
		}

		MagicJutsus.error("RemoveMarksJutsu '" + internalName + "' has an invalid mark-jutsu defined!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = livingEntity.getLocation();
			else {
				Block b = getTargetedBlock(livingEntity, power);
				if (b != null && !BlockUtils.isAir(b.getType())) loc = b.getLocation();
			}
			if (loc == null) return noTarget(livingEntity);
			removeMarks(livingEntity, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		removeMarks(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		removeMarks(null, target, power);
		return true;
	}

	private void removeMarks(LivingEntity caster, Location loc, float power) {
		float rad = radius * power;
		float radSq = rad * rad;

		Map<UUID, MagicLocation> marks = markJutsu.getMarks();
		Iterator<UUID> iter = marks.keySet().iterator();
		World locWorld = loc.getWorld();

		while (iter.hasNext()) {
			MagicLocation l = marks.get(iter.next());
			if (!l.getWorld().equals(locWorld.getName())) continue;
			if (l.getLocation().distanceSquared(loc) < radSq) iter.remove();
		}

		markJutsu.setMarks(marks);
		playJutsuEffects(EffectPosition.TARGET, loc);
		if (caster != null) playJutsuEffects(EffectPosition.CASTER, caster);
	}
	
}
