package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.damage.DamageType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class LightningSpell extends TargetedSpell implements TargetedLocationSpell {

	//	private Map<UUID, ChargeOption> striking;
	private static LightningListener lightningListener;

	private final ConfigData<Double> additionalDamage;

	private final ConfigData<Boolean> zapPigs;
	private final ConfigData<Boolean> noDamage;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> chargeCreepers;
	private final ConfigData<Boolean> transformEntities;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> powerAffectsAdditionalDamage;

	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		additionalDamage = getConfigDataDouble("additional-damage", 0F);

		zapPigs = getConfigDataBoolean("zap-pigs", true);
		noDamage = getConfigDataBoolean("no-damage", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		chargeCreepers = getConfigDataBoolean("charge-creepers", true);
		transformEntities = getConfigDataBoolean("transform-entities", true);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		powerAffectsAdditionalDamage = getConfigDataBoolean("power-affects-additional-damage", true);

		if (lightningListener == null) {
			lightningListener = new LightningListener();
			MagicSpells.registerEvents(lightningListener);
		}
	}

	@Override
	protected void turnOff() {
		if (lightningListener != null) lightningListener = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();

			if (noDamage.get(data)) {
				data.target().getWorld().strikeLightningEffect(data.target().getLocation());
				playSpellEffects(data);
				return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
			}

			double additionalDamage = this.additionalDamage.get(data);
			if (powerAffectsAdditionalDamage.get(data)) additionalDamage *= data.power();

			if (checkPlugins.get(data) && checkFakeDamageEvent(data.caster(), data.target()))
				return noTarget(data);

			ChargeOption option = new ChargeOption(additionalDamage, chargeCreepers.get(data), zapPigs.get(data), transformEntities.get(data));
			LightningStrike strike = data.target().getWorld().strikeLightning(data.target().getLocation());
			lightningListener.striking.put(strike.getUniqueId(), option);

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data);
		if (info.noTarget()) return noTarget(info);

		return castAtLocation(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location target = data.location();

		if (noDamage.get(data)) target.getWorld().strikeLightningEffect(target);
		else {
			double additionalDamage = this.additionalDamage.get(data);
			if (powerAffectsAdditionalDamage.get(data)) additionalDamage *= data.power();

			ChargeOption option = new ChargeOption(additionalDamage, chargeCreepers.get(data), zapPigs.get(data), transformEntities.get(data));
			LightningStrike strike = target.getWorld().strikeLightning(target);
			lightningListener.striking.put(strike.getUniqueId(), option);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private record ChargeOption(double additionalDamage, boolean chargeCreeper, boolean changePig, boolean transformEntities) {

	}

	private static class LightningListener implements Listener {

		private final Map<UUID, ChargeOption> striking = new HashMap<>();

		@EventHandler
		public void onLightningDamage(EntityDamageByEntityEvent event) {
			if (!(event.getDamager() instanceof LightningStrike strike)) return;

			DamageSource source = event.getDamageSource();
			if (source.getDamageType() != DamageType.LIGHTNING_BOLT) return;

			ChargeOption option = striking.get(strike.getUniqueId());
			if (option == null || option.additionalDamage <= 0) return;

			event.setDamage(event.getDamage() + option.additionalDamage);
		}

		@EventHandler
		public void onCreeperCharge(CreeperPowerEvent event) {
			LightningStrike strike = event.getLightning();
			if (strike == null) return;

			ChargeOption option = striking.get(strike.getUniqueId());
			if (option == null || option.transformEntities && option.chargeCreeper) return;

			event.setCancelled(true);
		}

		@EventHandler
		public void onPigZap(PigZapEvent event) {
			ChargeOption option = striking.get(event.getLightning().getUniqueId());
			if (option == null || option.transformEntities && option.changePig) return;

			event.setCancelled(true);
		}

		@EventHandler
		public void onZap(EntityZapEvent event) {
			ChargeOption option = striking.get(event.getBolt().getUniqueId());
			if (option == null || option.transformEntities) return;

			event.setCancelled(true);
		}

		@EventHandler
		public void onRemove(EntityRemoveFromWorldEvent event) {
			striking.remove(event.getEntity().getUniqueId());
		}

	}

}
