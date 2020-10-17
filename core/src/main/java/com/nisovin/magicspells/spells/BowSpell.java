package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

import org.apache.commons.math3.util.FastMath;

public class BowSpell extends Spell {

	private static final String METADATA_KEY = "MSBowSpell";

	private List<String> bowNames;
	private List<String> disallowedBowNames;

	private String bowName;
	private String spellOnShootName;
	private String spellOnHitEntityName;
	private String spellOnHitGroundName;

	private Subspell spellOnShoot;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	private boolean cancelShot;
	private boolean useBowForce;
	private boolean cancelShotOnFail;

	private float minimumForce;
	private float maximumForce;
	
	public BowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<String> names = getConfigStringList("bow-names", null);
		if (names != null) {
			bowNames = new ArrayList<>();
			names.forEach(str -> bowNames.add(Util.colorize(str)));
		} else bowName = Util.colorize(getConfigString("bow-name", ""));

		List<String> disallowedNames = getConfigStringList("disallowed-bow-names", null);
		if (disallowedNames != null) {
			disallowedBowNames = new ArrayList<>();
			disallowedNames.forEach(str -> disallowedBowNames.add(Util.colorize(str)));
		}

		spellOnShootName = getConfigString("spell", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");

		cancelShot = getConfigBoolean("cancel-shot", true);
		useBowForce = getConfigBoolean("use-bow-force", true);
		cancelShotOnFail = getConfigBoolean("cancel-shot-on-fail", true);

		minimumForce = getConfigFloat("minimum-force", 0F);
		maximumForce = getConfigFloat("maximum-force", 0F);

		if (minimumForce < 0F) minimumForce = 0F;
		else if (minimumForce > 1F) minimumForce = 1F;
		if (maximumForce < 0F) maximumForce = 0F;
		else if (maximumForce > 1F) maximumForce = 1F;
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnShoot = initSubspell(spellOnShootName, "BowSpell '" + internalName + "' has an invalid spell defined!");
		spellOnHitEntity = initSubspell(spellOnHitEntityName, "BowSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
		spellOnHitGround = initSubspell(spellOnHitGroundName, "BowSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");

		if (spellOnHitGround != null && !spellOnHitGround.isTargetedLocationSpell()) {
			MagicSpells.error("BowSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			spellOnHitGround = null;
		}
	}
	
	@Override
	public void turnOff() {
		super.turnOff();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}

	@EventHandler
	public void onArrowLaunch(EntityShootBowEvent event) {
		if (event.getEntity().getType() != EntityType.PLAYER) return;
		Player shooter = (Player) event.getEntity();
		ItemStack inHand = shooter.getEquipment().getItemInMainHand();
		if (inHand == null || inHand.getType() != Material.BOW) return;

		String name = inHand.getItemMeta().getDisplayName();
		if (bowNames != null && !bowNames.contains(name)) return;
		if (disallowedBowNames != null && disallowedBowNames.contains(name)) return;
		if (bowName != null && !bowName.isEmpty() && !bowName.equals(name)) return;

		float force = (float) (FastMath.floor(event.getForce() * 100F) / 100F);
		if (minimumForce != 0 && force < minimumForce) return;
		if (maximumForce != 0 && force > maximumForce) return;

		Spellbook spellbook = MagicSpells.getSpellbook(shooter);
		if (!spellbook.hasSpell(this)) return;
		if (!spellbook.canCast(this)) return;

		if (onCooldown(shooter)) {
			MagicSpells.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(shooter)) + ""), shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		if (!hasReagents(shooter)) {
			MagicSpells.sendMessage(strMissingReagents, shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		if (modifiers != null && !modifiers.check(shooter)) {
			MagicSpells.sendMessage(strModifierFailed, shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		SpellCastEvent castEvent = new SpellCastEvent(this, shooter, SpellCastState.NORMAL, useBowForce ? event.getForce() : 1.0F, null, cooldown, reagents, 0);
		EventUtil.call(castEvent);
		if (castEvent.isCancelled()) return;

		event.setCancelled(cancelShot);

		if (!cancelShot) {
			Entity projectile = event.getProjectile();
			projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicSpells.plugin, new ArrowData(castEvent.getPower(), spellOnHitEntity, spellOnHitGround, this)));
			playSpellEffects(EffectPosition.PROJECTILE, event.getProjectile());
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, shooter.getLocation(), projectile.getLocation(), shooter, projectile);
		}

		setCooldown(shooter, cooldown);
		removeReagents(shooter);
		if (spellOnShoot != null) spellOnShoot.cast(shooter, castEvent.getPower());

		SpellCastedEvent castedEvent = new SpellCastedEvent(this, shooter, SpellCastState.NORMAL, castEvent.getPower(), null, cooldown, reagents, PostCastAction.HANDLE_NORMALLY);
		EventUtil.call(castedEvent);
	}

	@EventHandler
	public void onArrowHitGround(ProjectileHitEvent event) {
		Projectile arrow = event.getEntity();
		if (arrow.getType() != EntityType.ARROW) return;
		List<MetadataValue> metas = arrow.getMetadata(METADATA_KEY);
		if (metas == null || metas.isEmpty()) return;
		Block block = event.getHitBlock();
		if (block == null) return;
		for (MetadataValue meta : metas) {
			ArrowData data = (ArrowData) meta.value();
			if (data == null) continue;
			if (data.groundSpell == null) continue;

			MagicSpells.scheduleDelayedTask(() -> {
				Player shooter = (Player) arrow.getShooter();
				if (data.casted) return;

				if (data.spell.getLocationModifiers() != null && !data.spell.getLocationModifiers().check(shooter, block.getLocation())) {
					MagicSpells.sendMessage(data.spell.getStrModifierFailed(), shooter, null);
					return;
				}

				data.groundSpell.castAtLocation(shooter, arrow.getLocation(), data.power);

				data.casted = true;
				arrow.removeMetadata(METADATA_KEY, MagicSpells.plugin);
			}, 0);
			break;
		}
		arrow.remove();
	}

	@EventHandler(ignoreCancelled=true)
	public void onArrowHitEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager().getType() != EntityType.ARROW) return;
		if (!(event.getEntity() instanceof LivingEntity)) return;
		Projectile arrow = (Projectile) event.getDamager();
		List<MetadataValue> metas = arrow.getMetadata(METADATA_KEY);
		if (metas == null || metas.isEmpty()) return;
		Player shooter = (Player) arrow.getShooter();
		LivingEntity target = (LivingEntity) event.getEntity();
		for (MetadataValue meta : metas) {
			ArrowData data = (ArrowData) meta.value();
			if (data == null) continue;
			if (data.casted) continue;
			if (data.entitySpell == null) continue;

			SpellTargetEvent targetEvent = new SpellTargetEvent(this, shooter, target, data.power);
			EventUtil.call(targetEvent);
			if (targetEvent.isCancelled()) {
				event.setCancelled(true);
				continue;
			}

			if (data.entitySpell.isTargetedEntityFromLocationSpell()) data.entitySpell.castAtEntityFromLocation(shooter, target.getLocation(), target, targetEvent.getPower());
			else if (data.entitySpell.isTargetedLocationSpell()) data.entitySpell.castAtLocation(shooter, target.getLocation(), targetEvent.getPower());
			else if (data.entitySpell.isTargetedEntitySpell()) data.entitySpell.castAtEntity(shooter, target, targetEvent.getPower());

			data.casted = true;
			break;
		}
		arrow.removeMetadata(METADATA_KEY, MagicSpells.plugin);
		arrow.remove();
	}

	private static class ArrowData {

		private float power;
		private boolean casted = false;

		private Spell spell;

		private Subspell entitySpell;
		private Subspell groundSpell;

		ArrowData(float power, Subspell entitySpell, Subspell groundSpell, Spell spell) {
			this.power = power;
			this.entitySpell = entitySpell;
			this.groundSpell = groundSpell;
			this.spell = spell;
		}
		
	}
	
}
