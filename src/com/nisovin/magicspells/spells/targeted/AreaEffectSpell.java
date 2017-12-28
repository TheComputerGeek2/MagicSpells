package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;

import org.apache.commons.math3.util.FastMath;

public class AreaEffectSpell extends TargetedSpell implements TargetedLocationSpell {

	private double radius;
	private double verticalRadius;
	private boolean pointBlank;
	private double cone;
	private boolean failIfNoTargets;
	private int maxTargets;
	private List<String> spellNames;
	private boolean spellSourceInCenter;
	private List<Subspell> spells;
	
	private ModifierSet locationTargetModifiers;
	private ModifierSet entityTargetModifiers;
	
	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigDouble("horizontal-radius", 10);
		verticalRadius = getConfigDouble("vertical-radius", 5);
		pointBlank = getConfigBoolean("point-blank", true);
		cone = getConfigDouble("cone", 0);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		maxTargets = getConfigInt("max-targets", 0);
		spellSourceInCenter = getConfigBoolean("spell-source-in-center", false);
		spellNames = getConfigStringList("spells", null);
		
		List<String> list = getConfigStringList("location-target-modifiers", null);
		if (list != null) locationTargetModifiers = new ModifierSet(list);
		list = getConfigStringList("entity-target-modifiers", null);
		if (list != null) entityTargetModifiers = new ModifierSet(list);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		spells = new ArrayList<>();
		
		if (spellNames != null && !spellNames.isEmpty()) {
			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (spell.process()) {
					if (spell.isTargetedEntityFromLocationSpell() || spell.isTargetedEntitySpell() || spell.isTargetedLocationSpell()) {
						spells.add(spell);
					} else {
						MagicSpells.error("AreaEffect spell '" + name + "' attempted to use non-targeted spell '" + spellName + '\'');
					}
				} else {
					MagicSpells.error("AreaEffect spell '" + name + "' attempted to use invalid spell '" + spellName + '\'');
				}
			}
			spellNames.clear();
			spellNames = null;
		}
		
		if (spells.isEmpty()) {
			MagicSpells.error("AreaEffect spell '" + name + "' has no spells!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// Get location for aoe
			Location loc = null;
			if (pointBlank) {
				loc = player.getLocation();
			} else {
				try {
					Block block = getTargetedBlock(player, power);
					if (block != null && block.getType() != Material.AIR) {
						loc = block.getLocation();
					}
				} catch (IllegalStateException e) {
					loc = null;
				}
			}
			if (loc != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc, power);
				EventUtil.call(event);
				if (locationTargetModifiers != null) {
					locationTargetModifiers.apply(event);
				}
				if (event.isCancelled()) {
					loc = null;
				} else {
					loc = event.getTargetLocation();
					power = event.getPower();
				}
			}
			if (loc == null) return noTarget(player);
			
			// Cast spells on nearby entities
			boolean done = doAoe(player, loc, power);
			
			// Check if no targets
			if (!done && failIfNoTargets) return noTarget(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean doAoe(Player player, Location location, float basePower) {
		int count = 0;
		
		Vector facing = player != null ? player.getLocation().getDirection() : location.getDirection();
		Vector vLoc = player != null ? player.getLocation().toVector() : location.toVector();
		
		BoundingBox box = new BoundingBox(location, radius, verticalRadius);
		List<Entity> entities = new ArrayList<>(location.getWorld().getEntitiesByClasses(LivingEntity.class));
		Collections.shuffle(entities);
		for (Entity e : entities) {
			if (e instanceof LivingEntity && box.contains(e)) {
				if (pointBlank && cone > 0) {
					Vector dir = e.getLocation().toVector().subtract(vLoc);
					if (FastMath.toDegrees(FastMath.abs(dir.angle(facing))) > cone) continue;
				}
				LivingEntity target = (LivingEntity)e;
				float power = basePower;
				if (!target.isDead() && ((player == null && validTargetList.canTarget(target)) || validTargetList.canTarget(player, target))) {
					if (player != null) {
						SpellTargetEvent event = new SpellTargetEvent(this, player, target, power);
						EventUtil.call(event);
						if (entityTargetModifiers != null) {
							entityTargetModifiers.apply(event);
						}
						if (event.isCancelled()) continue;
						target = event.getTarget();
						power = event.getPower();
					} else if (entityTargetModifiers != null) {
						SpellTargetEvent event = new SpellTargetEvent(this, player, target, power);
						entityTargetModifiers.apply(event);
						if (event.isCancelled()) continue;
					}
					for (Subspell spell : spells) {
						if (player != null) {
							if (spellSourceInCenter && spell.isTargetedEntityFromLocationSpell()) {
								spell.castAtEntityFromLocation(player, location, target, power);
							} else if (spell.isTargetedEntitySpell()) {
								spell.castAtEntity(player, target, power);
							} else if (spell.isTargetedLocationSpell()) {
								spell.castAtLocation(player, target.getLocation(), power);
							}
						} else {
							if (spell.isTargetedEntityFromLocationSpell()) {
								spell.castAtEntityFromLocation(null, location, target, power);
							} else if (spell.isTargetedEntitySpell()) {
								spell.castAtEntity(null, target, power);
							} else if (spell.isTargetedLocationSpell()) {
								spell.castAtLocation(null, target.getLocation(), power);
							}
						}
					}
					playSpellEffects(EffectPosition.TARGET, target);
					if (spellSourceInCenter) {
						playSpellEffectsTrail(location, target.getLocation());
					} else if (player != null) {
						playSpellEffectsTrail(player.getLocation(), target.getLocation());
					}
					count++;
					if (maxTargets > 0 && count >= maxTargets) break;
				}
			}
		}

		if (count > 0 || !failIfNoTargets) {
			if (player != null) playSpellEffects(EffectPosition.CASTER, player);
			playSpellEffects(EffectPosition.SPECIAL, location);
		}
		
		return count > 0;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return doAoe(null, target, power);
	}
	
}
