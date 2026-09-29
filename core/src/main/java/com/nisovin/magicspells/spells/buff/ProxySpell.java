package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ProxySpell extends BuffSpell {

	private final Set<UUID> redirecting = new HashSet<>();
	private final Map<UUID, LivingEntity> proxies = new HashMap<>();

	private SpellListener spellListener;
	private DamageListener damageListener;

	private RedirectDamage redirectDamage = RedirectDamage.ALL;

	public ProxySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		boolean redirectSpells = getConfigBoolean("redirect-spells", true);

		String redirectDamageString = getConfigString("redirect-damage", null);
		if (redirectDamageString != null) {
			RedirectDamage redirect = Util.enumValueSafe(RedirectDamage.class, redirectDamageString);
			if (redirect == null) MagicSpells.error("ProxySpell '" + internalName + "' has an invalid 'redirect-damage' value '" + redirectDamageString + "'.");
			else redirectDamage = redirect;
		}

		if (redirectSpells) {
			spellListener = new SpellListener();
			registerEvents(spellListener);
		}
		if (redirectDamage != RedirectDamage.NONE) {
			damageListener = new DamageListener();
			registerEvents(damageListener);
		}

		if (redirectSpells || redirectDamage != RedirectDamage.NONE) return;
		MagicSpells.error("ProxySpell '" + internalName + "' has no redirection enabled.");
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (data.target().equals(data.caster())) return false;
		proxies.put(data.target().getUniqueId(), data.caster());
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		turnOffBuff(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return proxies.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		proxies.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return proxies.keySet();
	}

	@Override
	protected void turnOff() {
		super.turnOff();

		if (spellListener != null) {
			unregisterEvents(spellListener);
			spellListener = null;
		}
		if (damageListener != null) {
			unregisterEvents(damageListener);
			damageListener = null;
		}
	}

	private class SpellListener implements Listener {

		@EventHandler(ignoreCancelled = true)
		public void onSpellTarget(SpellTargetEvent event) {
			LivingEntity target = event.getTarget();
			if (target == null || !isActive(target)) return;

			LivingEntity proxyTarget = getProxyTarget(target);
			if (proxyTarget == null) return;

			event.setTarget(proxyTarget);
			playRedirectEffects(target, proxyTarget, event.getSpellData());

			addUseAndChargeCost(target);
		}

	}

	private class DamageListener implements Listener {

		@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
		public void onEntityDamage(EntityDamageEvent event) {
			if (!(event.getEntity() instanceof LivingEntity target) || !isActive(target)) return;

			LivingEntity proxyTarget = getProxyTarget(target);
			if (proxyTarget == null) return;
			if (!redirecting.add(proxyTarget.getUniqueId())) return;

			LivingEntity damager = null;
			if (event instanceof EntityDamageByEntityEvent byEvent)
				damager = byEvent.getDamager() instanceof LivingEntity e ? e : null;
			else if (redirectDamage == RedirectDamage.ENTITIES) return;

			SpellData subData = new SpellData(damager, proxyTarget);
			playRedirectEffects(target, proxyTarget, subData);

			event.setCancelled(true);
			try {
				proxyTarget.damage(event.getFinalDamage(), event.getDamageSource());
				addUseAndChargeCost(target);
			} finally {
				redirecting.remove(proxyTarget.getUniqueId());
			}
		}

	}

	private enum RedirectDamage {
		ALL,
		ENTITIES,
		NONE
	}

	private LivingEntity getProxyTarget(LivingEntity target) {
		LivingEntity proxyTarget = proxies.get(target.getUniqueId());
		if (proxyTarget != null && proxyTarget.isValid()) return proxyTarget;

		turnOff(target);
		return null;
	}

	private void playRedirectEffects(LivingEntity target, LivingEntity proxyTarget, SpellData data) {
		playSpellEffects(EffectPosition.START_POSITION, target, data);
		playSpellEffects(EffectPosition.END_POSITION, proxyTarget, data);
	}

}
