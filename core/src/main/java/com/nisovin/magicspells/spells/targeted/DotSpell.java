package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class DotSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final DeprecationNotice DAMAGE_TYPE_DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'damage-type' option of '.targeted.DotSpell' does not function properly",
		"Use '.targeted.DamageSpell', in combination with '.targeted.LoopSpell'.",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#targeteddotspell-damage-type"
	);

	private final Map<UUID, Dot> activeDots;

	private final ConfigData<Integer> delay;
	private final ConfigData<Integer> interval;
	private final ConfigData<Integer> duration;

	private final ConfigData<Double> damage;

	private final ConfigData<Boolean> ignoreArmor;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> avoidDamageModification;
	private final ConfigData<Boolean> tryAvoidingAntiCheatPlugins;

	private final ConfigData<String> spellDamageType;
	private final ConfigData<DamageCause> damageType;

	public DotSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigDataInt("delay", 1);
		interval = getConfigDataInt("interval", 20);
		duration = getConfigDataInt("duration", 200);

		damage = getConfigDataDouble("damage", 2);

		ignoreArmor = getConfigDataBoolean("ignore-armor", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigDataBoolean("try-avoiding-anticheat-plugins", false);

		spellDamageType = getConfigDataString("spell-damage-type", "");
		damageType = getConfigDataEnum("damage-type", DamageCause.class, DamageCause.ENTITY_ATTACK);

		activeDots = new HashMap<>();

		MagicSpells.getDeprecationManager().addDeprecation(this, DAMAGE_TYPE_DEPRECATION_NOTICE,
			!damageType.isConstant() || damageType.get() != DamageCause.ENTITY_ATTACK
		);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		Dot dot = activeDots.get(data.target().getUniqueId());

		if (dot != null) {
			dot.data = data;

			dot.init();
		} else {
			dot = new Dot(data);
			activeDots.put(data.target().getUniqueId(), dot);
		}

		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public boolean isActive(LivingEntity entity) {
		return activeDots.containsKey(entity.getUniqueId());
	}

	public void cancelDot(LivingEntity entity) {
		if (!isActive(entity)) return;
		Dot dot = activeDots.get(entity.getUniqueId());
		dot.cancel();
	}

	@EventHandler
	private void onDeath(PlayerDeathEvent event) {
		Dot dot = activeDots.get(event.getEntity().getUniqueId());
		if (dot != null) dot.cancel();
	}

	private class Dot implements Runnable {

		private SpellData data;

		private int taskId = -1;
		private int duration;
		private int interval;
		private int dur = 0;

		private boolean tryAvoidingAntiCheatPlugins;
		private boolean avoidDamageModification;
		private boolean powerAffectsDamage;
		private boolean checkPlugins;
		private boolean ignoreArmor;

		private String spellDamageType;
		private DamageCause damageType;

		private Dot(SpellData data) {
			this.data = data;

			init();
		}

		private void init() {
			if (taskId != -1) MagicSpells.cancelTask(taskId);

			interval = DotSpell.this.interval.get(data);
			duration = DotSpell.this.duration.get(data);
			dur = 0;

			tryAvoidingAntiCheatPlugins = DotSpell.this.tryAvoidingAntiCheatPlugins.get(data);
			avoidDamageModification = DotSpell.this.avoidDamageModification.get(data);
			powerAffectsDamage = DotSpell.this.powerAffectsDamage.get(data);
			checkPlugins = DotSpell.this.checkPlugins.get(data);
			ignoreArmor = DotSpell.this.ignoreArmor.get(data);

			spellDamageType = DotSpell.this.spellDamageType.get(data);
			damageType = DotSpell.this.damageType.get(data);

			taskId = MagicSpells.scheduleRepeatingTask(this, delay.get(data), interval);
		}

		@Override
		public void run() {
			dur += interval;
			if (dur > duration) {
				cancel();
				return;
			}

			if (!data.target().isValid()) {
				cancel();
				return;
			}

			double localDamage = damage.get(data);
			if (powerAffectsDamage) localDamage *= data.power();

			if (checkPlugins && data.hasCaster() && damageType != DamageCause.ENTITY_ATTACK) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(data.caster(), data.target(), damageType, localDamage, DotSpell.this);
				if (!event.callEvent()) return;

				if (!avoidDamageModification) localDamage = event.getDamage();
				data.target().setLastDamageCause(event);
			}

			if (ignoreArmor && checkPlugins && data.hasCaster()) {
				EntityDamageEvent damageEvent = createFakeDamageEvent(data.caster(), data.target(), DamageCause.ENTITY_ATTACK, localDamage);
				if (!damageEvent.callEvent()) return;

				if (!avoidDamageModification) localDamage = damageEvent.getDamage();
			}

			SpellApplyDamageEvent event = new SpellApplyDamageEvent(DotSpell.this, data.caster(), data.target(), localDamage, damageType, spellDamageType);
			event.callEvent();
			localDamage = event.getFinalDamage();

			if (ignoreArmor) {
				double maxHealth = Util.getMaxHealth(data.target());
				double health = data.target().getHealth();

				if (health > maxHealth) health = maxHealth;
				health -= localDamage;

				if (health < 0) {
					health = 0;
					if (data.caster() instanceof Player player) data.target().setKiller(player);
				}

				if (health > maxHealth) health = maxHealth;

				data.target().setHealth(health);
				data.target().setLastDamage(localDamage);
				Util.playHurtEffect(data.target(), data.caster());
			} else {
				if (tryAvoidingAntiCheatPlugins || !data.hasCaster()) data.target().damage(localDamage);
				else data.target().damage(localDamage, data.caster());
			}

			playSpellEffects(EffectPosition.DELAYED, data.target(), data);
			data.target().setNoDamageTicks(0);
		}

		private void cancel() {
			MagicSpells.cancelTask(taskId);
			activeDots.remove(data.target().getUniqueId());
		}

	}

}
