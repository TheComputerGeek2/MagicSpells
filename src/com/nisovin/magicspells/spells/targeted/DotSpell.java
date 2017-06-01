package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.magicspells.util.expression.NullOperation;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class DotSpell extends TargetedSpell implements TargetedEntitySpell, SpellDamageSpell {

	int delay;
	int interval;
	int duration;
	float damage;
	boolean preventKnockback;
	String spellDamageType;
	
	Map<Integer, Dot> activeDots = new HashMap<Integer, Dot>();
	
	public DotSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigInt("delay", 1);
		interval = getConfigInt("interval", 20);
		duration = getConfigInt("duration", 200);
		damage = getConfigFloat("damage", 2);
		preventKnockback = getConfigBoolean("prevent-knockback", false);
		spellDamageType = getConfigString("spell-damage-type", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) {
				return noTarget(player);
			}
			applyDot(player, targetInfo.getTarget(), targetInfo.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void applyDot(Player caster, LivingEntity target, float power) {
		Dot dot = activeDots.get(target.getEntityId());
		if (dot != null) {
			dot.dur = 0;
			dot.power = power;
		} else {
			dot = new Dot(caster, target, power);
			activeDots.put(target.getEntityId(), dot);
		}
		if (caster != null) {
			playSpellEffects(caster, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		applyDot(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		applyDot(null, target, power);
		return true;
	}
	
	@EventHandler
	void onDeath(PlayerDeathEvent event) {
		Dot dot = activeDots.get(event.getEntity().getEntityId());
		if (dot != null) {
			dot.cancel();
		}
	}
	
	class Dot implements Runnable {
		
		Player caster;
		LivingEntity target;
		float power;
		
		int taskId;
		int dur = 0;
		
		public Dot(Player caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			taskId = MagicSpells.scheduleRepeatingTask(this, delay, interval);
		}
		
		@Override
		public void run() {
			dur += interval;
			if (dur > duration) {
				cancel();
				return;
			}
			if (target.isDead() || !target.isValid()) {
				cancel();
				return;
			}
			double dam = damage * power;
			SpellApplyDamageEvent event = new SpellApplyDamageEvent(DotSpell.this, caster, target, dam, DamageCause.MAGIC, spellDamageType);
			Bukkit.getPluginManager().callEvent(event);
			dam = event.getFinalDamage();
			if (preventKnockback && target != null && caster != null && caster.getWorld().equals(target.getWorld())) {
				// bukkit doesn't call a damage event here, so we'll do it ourselves
				MagicSpellsEntityDamageByEntityEvent devent = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage);
				Bukkit.getPluginManager().callEvent(devent);
				if (!devent.isCancelled()) {
					target.damage(devent.getDamage());
				}
			} else {
				if (target != null) {
					target.damage(dam, caster);
				}else{
					target.damage(dam);
				}
			}
			target.setNoDamageTicks(0);
			playSpellEffects(EffectPosition.DELAYED, target);
		}
		
		public void cancel() {
			MagicSpells.cancelTask(taskId);
			activeDots.remove(target.getEntityId());
		}
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}

}
