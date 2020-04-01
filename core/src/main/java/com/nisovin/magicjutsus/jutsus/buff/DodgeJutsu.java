package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.ProjectileTracker;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.ParticleProjectileHitEvent;

import de.slikey.effectlib.util.RandomUtils;

public class DodgeJutsu extends BuffJutsu {

	private Set<UUID> entities;

	private float distance;

	private JutsuFilter filter;

	private Ninjutsu jutsuBeforeDodge;
	private Ninjutsu jutsuAfterDodge;
	private String jutsuBeforeDodgeName;
	private String jutsuAfterDodgeName;

	public DodgeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		entities = new HashSet<>();

		distance = getConfigFloat("distance", 2);

		jutsuBeforeDodgeName = getConfigString("jutsu-before-dodge", "");
		jutsuAfterDodgeName = getConfigString("jutsu-after-dodge", "");

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> jutsuTags = getConfigStringList("jutsu-tags", null);
		List<String> deniedJutsuTags = getConfigStringList("denied-jutsu-tags", null);

		filter = new JutsuFilter(jutsus, deniedJutsus, jutsuTags, deniedJutsuTags);
	}

	@Override
	public void initialize() {
		super.initialize();

		jutsuBeforeDodge = new Ninjutsu(jutsuBeforeDodgeName);
		if (!jutsuBeforeDodge.process() || !jutsuBeforeDodge.isTargetedLocationJutsu()) {
			if (!jutsuBeforeDodgeName.isEmpty()) MagicJutsus.error("DodgeJutsu '" + internalName + "' has an invalid jutsu-before-dodge defined!");
			jutsuBeforeDodge = null;
		}

		jutsuAfterDodge = new Ninjutsu(jutsuAfterDodgeName);
		if (!jutsuAfterDodge.process() || !jutsuAfterDodge.isTargetedLocationJutsu()) {
			if (!jutsuAfterDodgeName.isEmpty()) MagicJutsus.error("DodgeJutsu '" + internalName + "' has an invalid jutsu-after-dodge defined!");
			jutsuAfterDodge = null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}
		entities.clear();
	}

	@EventHandler
	public void onProjectileHit(ParticleProjectileHitEvent e) {
		LivingEntity target = e.getTarget();
		if (!isActive(target)) return;

		Jutsu jutsu = e.getJutsu();
		if (jutsu != null && !filter.check(jutsu)) return;

		ProjectileTracker tracker = e.getTracker();
		if (tracker == null) return;
		if (tracker.getCaster().equals(target)) return;

		e.setCancelled(true);
		tracker.getImmune().add(target);
		dodge(target, tracker.getCurrentLocation());
		playJutsuEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
	}

	private void dodge(LivingEntity entity, Location location) {
		Location targetLoc = location.clone();
		Location entityLoc = entity.getLocation().clone();
		playJutsuEffects(EffectPosition.SPECIAL, entityLoc);

		Vector v = RandomUtils.getRandomCircleVector().multiply(distance);

		targetLoc.add(v);
		targetLoc.setDirection(entity.getLocation().getDirection());

		if (jutsuBeforeDodge != null) jutsuBeforeDodge.castAtLocation(entity, entityLoc, 1F);

		if (!BlockUtils.isPathable(targetLoc.getBlock().getType()) || !BlockUtils.isPathable(targetLoc.getBlock().getRelative(BlockFace.UP))) return;
		entity.teleport(targetLoc);
		addUseAndChargeCost(entity);
		playJutsuEffectsTrail(entityLoc, targetLoc);
		playJutsuEffects(EffectPosition.DELAYED, targetLoc);
		if (jutsuAfterDodge != null) jutsuAfterDodge.castAtLocation(entity, targetLoc, 1F);
	}

}
