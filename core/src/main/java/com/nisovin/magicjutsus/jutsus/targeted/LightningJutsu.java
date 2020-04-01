package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class LightningJutsu extends TargetedJutsu implements TargetedLocationJutsu {

	private double additionalDamage;

	private boolean zapPigs;
	private boolean noDamage;
	private boolean checkPlugins;
	private boolean chargeCreepers;
	private boolean requireEntityTarget;
	
	public LightningJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		additionalDamage = getConfigFloat("additional-damage", 0F);

		zapPigs = getConfigBoolean("zap-pigs", true);
		noDamage = getConfigBoolean("no-damage", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		chargeCreepers = getConfigBoolean("charge-creepers", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Block target;
			LivingEntity entityTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo != null) {
					entityTarget = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
				if (checkPlugins) {
					MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, entityTarget, DamageCause.ENTITY_ATTACK, 1 + additionalDamage);
					EventUtil.call(event);
					if (event.isCancelled()) entityTarget = null;
				}
				if (entityTarget != null) {
					target = entityTarget.getLocation().getBlock();
					if (additionalDamage > 0) entityTarget.damage(additionalDamage * power, livingEntity);
				} else return noTarget(livingEntity);
			} else {
				try {
					target = getTargetedBlock(livingEntity, power);
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
					target = null;
				}
				if (target != null) {
					JutsuTargetLocationEvent event = new JutsuTargetLocationEvent(this, livingEntity, target.getLocation(), power);
					EventUtil.call(event);
					if (event.isCancelled()) {
						target = null;
					} else target = event.getTargetLocation().getBlock();
				}
			}
			if (target != null) {
				lightning(target.getLocation());
				playJutsuEffects(livingEntity, target.getLocation());
				if (entityTarget != null) {
					sendMessages(livingEntity, entityTarget);
					return PostCastAction.NO_MESSAGES;
				}
			} else return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		lightning(target);
		playJutsuEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		lightning(target);
		playJutsuEffects(EffectPosition.CASTER, target);
		return true;
	}
	
	private void lightning(Location target) {
		if (noDamage) target.getWorld().strikeLightningEffect(target);
		else {
			LightningStrike strike = target.getWorld().strikeLightning(target);
			strike.setMetadata("MS" + internalName, new FixedMetadataValue(MagicJutsus.plugin, new ChargeOption(chargeCreepers, zapPigs)));
		}
	}
	
	@EventHandler
	public void onCreeperCharge(CreeperPowerEvent event) {
		LightningStrike strike = event.getLightning();
		if (strike == null) return;
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data == null || data.isEmpty()) return;
		for (MetadataValue val: data) {
			ChargeOption option = (ChargeOption)val.value();
			if (!option.chargeCreeper) event.setCancelled(true);
			break;
		}
	}
	
	@EventHandler
	public void onPigZap(PigZapEvent event) {
		LightningStrike strike = event.getLightning();
		if (strike == null) return;
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data == null || data.isEmpty()) return;
		for (MetadataValue val: data) {
			ChargeOption option = (ChargeOption) val.value();
			if (!option.changePig) event.setCancelled(true);
		}
	}
	
	private static class ChargeOption {

		private boolean changePig;
		private boolean chargeCreeper;

		private ChargeOption(boolean creeper, boolean pigmen) {
			changePig = pigmen;
			chargeCreeper = creeper;
		}
		
	}
	
}
