package com.nisovin.magicjutsus.jutsus;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.ChatColor;
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

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

import org.apache.commons.math3.util.FastMath;

public class BowJutsu extends Jutsu {

	private static final String METADATA_KEY = "MSBowJutsu";

	private List<String> bowNames;
	private List<String> disallowedBowNames;

	private String bowName;
	private String jutsuOnShootName;
	private String jutsuOnHitEntityName;
	private String jutsuOnHitGroundName;

	private Ninjutsu jutsuOnShoot;
	private Ninjutsu jutsuOnHitEntity;
	private Ninjutsu jutsuOnHitGround;

	private boolean cancelShot;
	private boolean useBowForce;
	private boolean cancelShotOnFail;

	private float minimumForce;
	private float maximumForce;
	
	public BowJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		List<String> names = getConfigStringList("bow-names", null);
		if (names != null) {
			bowNames = new ArrayList<>();
			names.forEach(str -> bowNames.add(ChatColor.translateAlternateColorCodes('&', str)));
		} else bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", ""));

		List<String> disallowedNames = getConfigStringList("disallowed-bow-names", null);
		if (disallowedNames != null) {
			disallowedBowNames = new ArrayList<>();
			disallowedNames.forEach(str -> disallowedBowNames.add(ChatColor.translateAlternateColorCodes('&', str)));
		}

		jutsuOnShootName = getConfigString("jutsu", "");
		jutsuOnHitEntityName = getConfigString("jutsu-on-hit-entity", "");
		jutsuOnHitGroundName = getConfigString("jutsu-on-hit-ground", "");

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

		jutsuOnShoot = initNinjutsu(jutsuOnShootName, "BowJutsu '" + internalName + "' has an invalid jutsu defined!");
		jutsuOnHitEntity = initNinjutsu(jutsuOnHitEntityName, "BowJutsu '" + internalName + "' has an invalid jutsu-on-hit-entity defined!");
		jutsuOnHitGround = initNinjutsu(jutsuOnHitGroundName, "BowJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");

		if (jutsuOnHitGround != null && !jutsuOnHitGround.isTargetedLocationJutsu()) {
			MagicJutsus.error("BowJutsu '" + internalName + "' has an invalid jutsu-on-hit-ground defined!");
			jutsuOnHitGround = null;
		}
	}
	
	@Override
	public void turnOff() {
		super.turnOff();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
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

		Jutsubook jutsubook = MagicJutsus.getJutsubook(shooter);
		if (!jutsubook.hasJutsu(this)) return;
		if (!jutsubook.canCast(this)) return;

		if (onCooldown(shooter)) {
			MagicJutsus.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(shooter)) + ""), shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		if (!hasReagents(shooter)) {
			MagicJutsus.sendMessage(strMissingReagents, shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		if (modifiers != null && !modifiers.check(shooter)) {
			MagicJutsus.sendMessage(strModifierFailed, shooter, null);
			event.setCancelled(cancelShotOnFail);
			return;
		}

		JutsuCastEvent castEvent = new JutsuCastEvent(this, shooter, JutsuCastState.NORMAL, useBowForce ? event.getForce() : 1.0F, null, cooldown, reagents, 0);
		EventUtil.call(castEvent);
		if (castEvent.isCancelled()) return;

		event.setCancelled(cancelShot);

		if (!cancelShot) {
			Entity projectile = event.getProjectile();
			projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicJutsus.plugin, new ArrowData(castEvent.getPower())));
			playJutsuEffects(EffectPosition.PROJECTILE, event.getProjectile());
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, shooter.getLocation(), projectile.getLocation(), shooter, projectile);
		}

		setCooldown(shooter, cooldown);
		removeReagents(shooter);
		if (jutsuOnShoot != null) jutsuOnShoot.cast(shooter, castEvent.getPower());

		JutsuCastedEvent castedEvent = new JutsuCastedEvent(this, shooter, JutsuCastState.NORMAL, castEvent.getPower(), null, cooldown, reagents, PostCastAction.HANDLE_NORMALLY);
		EventUtil.call(castedEvent);
	}

	@EventHandler
	public void onArrowHitGround(ProjectileHitEvent event) {
		Projectile arrow = event.getEntity();
		if (arrow.getType() != EntityType.ARROW) return;
		List<MetadataValue> metas = arrow.getMetadata(METADATA_KEY);
		if (metas == null || metas.isEmpty()) return;
		Block block = event.getHitBlock();
		if (block == null) return;;
		for (MetadataValue meta : metas) {
			ArrowData data = (ArrowData) meta.value();
			if (data == null) continue;
			if (jutsuOnHitGround == null) continue;

			MagicJutsus.scheduleDelayedTask(() -> {
				Player shooter = (Player) arrow.getShooter();
				if (data.casted) return;

				if (locationModifiers != null && !locationModifiers.check(shooter, block.getLocation())) {
					MagicJutsus.sendMessage(strModifierFailed, shooter, null);
					return;
				}

				jutsuOnHitGround.castAtLocation(shooter, arrow.getLocation(), data.power);

				data.casted = true;
				arrow.removeMetadata(METADATA_KEY, MagicJutsus.plugin);
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
			if (jutsuOnHitEntity == null) continue;

			JutsuTargetEvent targetEvent = new JutsuTargetEvent(this, shooter, target, data.power);
			EventUtil.call(targetEvent);
			if (targetEvent.isCancelled()) {
				event.setCancelled(true);
				continue;
			}

			if (jutsuOnHitEntity.isTargetedEntityFromLocationJutsu()) jutsuOnHitEntity.castAtEntityFromLocation(shooter, target.getLocation(), target, targetEvent.getPower());
			else if (jutsuOnHitEntity.isTargetedLocationJutsu()) jutsuOnHitEntity.castAtLocation(shooter, target.getLocation(), targetEvent.getPower());
			else if (jutsuOnHitEntity.isTargetedEntityJutsu()) jutsuOnHitEntity.castAtEntity(shooter, target, targetEvent.getPower());

			data.casted = true;
			break;
		}
		arrow.removeMetadata(METADATA_KEY, MagicJutsus.plugin);
		arrow.remove();
	}

	private static class ArrowData {

		private float power;
		private boolean casted = false;

		ArrowData(float power) {
			this.power = power;
		}
		
	}
	
}
