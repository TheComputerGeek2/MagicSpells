package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;
import com.nisovin.magicspells.util.trackers.ParticleProjectileTracker;

import de.slikey.effectlib.util.RandomUtils;

public class DodgeSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Float> distance;

	private SpellFilter filter;

	private Subspell spellBeforeDodge;
	private Subspell spellAfterDodge;

	private final String spellBeforeDodgeName;
	private final String spellAfterDodgeName;

	public DodgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashMap<>();

		distance = getConfigDataFloat("distance", 2);

		spellBeforeDodgeName = getConfigString("spell-before-dodge", "");
		spellAfterDodgeName = getConfigString("spell-after-dodge", "");

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> spellTags = getConfigStringList("spell-tags", null);
		List<String> deniedSpellTags = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellBeforeDodge = new Subspell(spellBeforeDodgeName);
		if (!spellBeforeDodge.process() || !spellBeforeDodge.isTargetedLocationSpell()) {
			if (!spellBeforeDodgeName.isEmpty()) MagicSpells.error("DodgeSpell '" + internalName + "' has an invalid spell-before-dodge defined!");
			spellBeforeDodge = null;
		}

		spellAfterDodge = new Subspell(spellAfterDodgeName);
		if (!spellAfterDodge.process() || !spellAfterDodge.isTargetedLocationSpell()) {
			if (!spellAfterDodgeName.isEmpty()) MagicSpells.error("DodgeSpell '" + internalName + "' has an invalid spell-after-dodge defined!");
			spellAfterDodge = null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		return true;
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

		Spell spell = e.getSpell();
		if (spell != null && !filter.check(spell)) return;

		ParticleProjectileTracker tracker = e.getTracker();
		if (tracker == null) return;
		if (tracker.getCaster().equals(target)) return;

		e.setCancelled(true);
		tracker.getImmune().add(target);
		dodge(target, tracker);
		playSpellEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
	}

	private void dodge(LivingEntity entity, ParticleProjectileTracker tracker) {
		Location targetLoc = tracker.getCurrentLocation().clone();
		Location entityLoc = entity.getLocation().clone();
		playSpellEffects(EffectPosition.SPECIAL, entityLoc);

		SpellData data = entities.get(entity.getUniqueId());
		Vector v = RandomUtils.getRandomCircleVector().multiply(distance.get(entity, tracker.getCaster(), data.power(), data.args()));

		targetLoc.add(v);
		targetLoc.setDirection(entity.getLocation().getDirection());

		if (spellBeforeDodge != null) spellBeforeDodge.castAtLocation(entity, entityLoc, 1F);

		if (!BlockUtils.isPathable(targetLoc.getBlock().getType()) || !BlockUtils.isPathable(targetLoc.getBlock().getRelative(BlockFace.UP))) return;
		entity.teleport(targetLoc);
		addUseAndChargeCost(entity);
		playSpellEffectsTrail(entityLoc, targetLoc);
		playSpellEffects(EffectPosition.DELAYED, targetLoc);
		if (spellAfterDodge != null) spellAfterDodge.castAtLocation(entity, targetLoc, 1F);
	}

	public Map<UUID, SpellData> getEntities() {
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

}
