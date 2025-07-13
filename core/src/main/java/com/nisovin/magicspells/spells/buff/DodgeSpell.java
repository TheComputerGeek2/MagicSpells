package com.nisovin.magicspells.spells.buff;

import java.util.*;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

import de.slikey.effectlib.util.RandomUtils;

public class DodgeSpell extends BuffSpell {

	private final Map<UUID, DodgeData> entities;

	private final ConfigData<Boolean> constantDistance;

	private final ConfigData<Double> distance;

	private SpellFilter filter;

	private Subspell spellBeforeDodge;
	private Subspell spellAfterDodge;

	private final String spellBeforeDodgeName;
	private final String spellAfterDodgeName;

	public DodgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashMap<>();

		constantDistance = getConfigDataBoolean("constant-distance", true);

		distance = getConfigDataDouble("distance", 2);

		spellBeforeDodgeName = getConfigString("spell-before-dodge", "");
		spellAfterDodgeName = getConfigString("spell-after-dodge", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "DodgeSpell '" + internalName + "' has an invalid '%s' defined!";
		spellBeforeDodge = initSubspell(spellBeforeDodgeName,
				error.formatted("spell-before-dodge"),
				true);
		spellAfterDodge = initSubspell(spellAfterDodgeName,
				error.formatted("spell-after-dodge"),
				true);

		filter = getConfigSpellFilter();
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantDistance = this.constantDistance.get(data);
		entities.put(data.target().getUniqueId(), new DodgeData(data, constantDistance ? distance.get(data) : 0, constantDistance));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	@EventHandler
	public void onProjectileHit(ParticleProjectileHitEvent e) {
		LivingEntity target = e.getTarget();
		if (!isActive(target)) return;

		Spell spell = e.getSpell();
		if (spell != null && !filter.check(spell)) return;

		ParticleProjectileTracker tracker = e.getTracker();
		if (tracker == null) return;
		if (tracker.getCaster().equals(target)) return;

		e.setCancelled(true);
		tracker.getImmune().add(target);

		DodgeData dodgeData = entities.get(target.getUniqueId());
		SpellData subData = dodgeData.spellData.target(tracker.getCaster());
		dodge(target, tracker, subData, dodgeData);

		playSpellEffects(EffectPosition.TARGET, tracker.getCurrentLocation(), subData);
	}

	private void dodge(LivingEntity caster, ParticleProjectileTracker tracker, SpellData subData, DodgeData dodgeData) {
		Location targetLoc = tracker.getCurrentLocation().clone();
		Location casterLoc = caster.getLocation().clone();

		playSpellEffects(EffectPosition.SPECIAL, casterLoc, subData);

		double distance = dodgeData.constantDistance ? dodgeData.distance : this.distance.get(subData);
		Vector v = RandomUtils.getRandomCircleVector().multiply(distance);
		targetLoc.add(v);
		targetLoc.setDirection(caster.getLocation().getDirection());

		if (spellBeforeDodge != null) {
			SpellData castData = subData.builder().caster(caster).target(null).location(casterLoc).recipient(null).build();
			spellBeforeDodge.subcast(castData);
		}

		if (!targetLoc.getBlock().isPassable() || !targetLoc.getBlock().getRelative(BlockFace.UP).isPassable()) return;
		caster.teleportAsync(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
		addUseAndChargeCost(caster);

		playSpellEffectsTrail(casterLoc, targetLoc, subData);
		playSpellEffects(EffectPosition.DELAYED, targetLoc, subData);

		if (spellAfterDodge != null) {
			SpellData castData = subData.builder().caster(caster).target(null).location(targetLoc).recipient(null).build();
			spellAfterDodge.subcast(castData);
		}
	}

	public Map<UUID, DodgeData> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

	public Subspell getSpellBeforeDodge() {
		return spellBeforeDodge;
	}

	public void setSpellBeforeDodge(Subspell spellBeforeDodge) {
		this.spellBeforeDodge = spellBeforeDodge;
	}

	public Subspell getSpellAfterDodge() {
		return spellAfterDodge;
	}

	public void setSpellAfterDodge(Subspell spellAfterDodge) {
		this.spellAfterDodge = spellAfterDodge;
	}

	public record DodgeData(SpellData spellData, double distance, boolean constantDistance) {
	}

}
