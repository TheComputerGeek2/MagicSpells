package com.nisovin.magicspells.spells.targeted.ext;

import java.util.Set;
import java.util.UUID;
import java.util.Collection;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import ru.xezard.glow.data.glow.Glow;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.buff.InvisibilitySpell;

// XGlow: https://github.com/Xezard/XGlow
@DependsOn({"ProtocolLib", "XGlow"})
public class GlowSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Multimap<UUID, GlowData> glowing;

	private final ConfigData<ChatColor> color;

	private final ConfigData<Integer> duration;

	private final ConfigData<Boolean> powerAffectsDuration;

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		glowing = LinkedListMultimap.create();

		duration = getConfigDataInt("duration", 0);

		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);

		color = getConfigDataEnum("color", ChatColor.class, ChatColor.WHITE);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		glow(caster, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		glow(caster, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public void turnOff() {
		glowing.values().forEach(glowData -> glowData.getGlow().destroy());
	}

	private void glow(Player caster, SpellData data) {
		int duration = this.duration.get(data);
		if (powerAffectsDuration.get(data)) duration = Math.round(duration * data.power());

		LivingEntity target = data.target();

		Collection<GlowData> glows = glowing.get(caster.getUniqueId());
		for (GlowData glowData : glows) {
			// That entity is glowing for the caster
			if (!glowData.getGlow().hasHolder(target)) continue;

			// If casted by the same spell, extend duration, otherwise fail
			if (!glowData.getInternalName().equals(internalName)) return;

			MagicSpells.cancelTask(glowData.getTask());
			glowData.setTask(MagicSpells.scheduleDelayedTask(() -> {
				// Make the target hidden if it has an invisibility spell active
				if (target instanceof Player targetPlayer) {
					Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(target);
					if (buffSpells != null) {
						for (BuffSpell buffSpell : buffSpells) {
							if (!(buffSpell instanceof InvisibilitySpell)) continue;
							if (!caster.canSee(targetPlayer)) continue;

							caster.hidePlayer(MagicSpells.getInstance(), targetPlayer);
						}
					}
				}

				glowData.getGlow().destroy();
				glowing.remove(caster.getUniqueId(), glowData);
			}, duration));

			return;
		}

		GlowData glowData;

		String name = target.getUniqueId() + caster.getUniqueId().toString() + internalName;
		ChatColor color = this.color.get(data);

		Glow glow = new Glow(color, name);
		glow.addHolders(target);
		glow.display(caster);

		glowData = new GlowData(glow, internalName);
		glowData.setTask(MagicSpells.scheduleDelayedTask(() -> {
			// Make the target hidden if it has an invisibility spell active
			if (target instanceof Player targetPlayer) {
				Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(targetPlayer);
				if (buffSpells != null) {
					for (BuffSpell buffSpell : buffSpells) {
						if (!(buffSpell instanceof InvisibilitySpell)) continue;
						if (!caster.canSee(targetPlayer)) continue;

						caster.hidePlayer(MagicSpells.getInstance(), targetPlayer);
					}
				}
			}

			glow.destroy();
			glowing.remove(caster.getUniqueId(), glowData);
		}, duration));

		// If target is a vanished player, make the caster see the target with vanish
		if (target instanceof Player targetPlayer) {
			Set<BuffSpell> buffSpells = MagicSpells.getBuffManager().getActiveBuffs(targetPlayer);
			if (buffSpells != null) {
				for (BuffSpell buffSpell : buffSpells) {
					if (!(buffSpell instanceof InvisibilitySpell)) continue;
					if (caster.canSee(targetPlayer)) continue;

					caster.showPlayer(MagicSpells.getInstance(), targetPlayer);
				}
			}
		}

		glowing.put(caster.getUniqueId(), glowData);
		playSpellEffects(data);
	}

	private static class GlowData {

		private Glow glow;
		private ScheduledTask task;
		private String internalName;

		private GlowData(Glow glow, String internalName) {
			this.glow = glow;
			this.internalName = internalName;
		}

		public Glow getGlow() {
			return glow;
		}

		public void setGlow(Glow glow) {
			this.glow = glow;
		}

		public ScheduledTask getTask() {
			return task;
		}

		public void setTask(ScheduledTask task) {
			this.task = task;
		}

		public String getInternalName() {
			return internalName;
		}

		public void setInternalName(String internalName) {
			this.internalName = internalName;
		}

	}

}
