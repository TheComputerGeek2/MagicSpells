package com.nisovin.magicspells.spells.targeted.ext;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.inventivetalent.glow.GlowAPI;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

/*
	NOTE: ProtocolLib and GlowAPI are required for this spell class.
	GlowAPI: https://github.com/metalshark/GlowAPI/
*/
public class GlowSpell extends TargetedSpell implements TargetedEntitySpell {

	private final Multimap<UUID, UUID> glowing;
	private final Set<UUID> glowingUnpaired;

	private final boolean toggle;
	private final int duration;
	private final boolean clientSide;
	private final boolean visibleToTarget;
	private GlowAPI.Color color;

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		glowing = LinkedListMultimap.create();
		glowingUnpaired = new HashSet<>();

		toggle = getConfigBoolean("toggle", false);
		duration = getConfigInt("duration", 0);
		clientSide = getConfigBoolean("client-side", false);
		visibleToTarget = getConfigBoolean("visible-to-target", false);

		String colorName = getConfigString("color", "");
		try {
			color = GlowAPI.Color.valueOf(colorName.toUpperCase());
		}
		catch (IllegalArgumentException ignored) {
			color = GlowAPI.Color.WHITE;
			MagicSpells.log("GlowSpell '" + internalName + "' has an invalid color defined': " + colorName);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player caster) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();

			sendMessages(caster, target, args);
			glow(caster, target, targetInfo.getPower(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		glow(caster instanceof Player ? (Player) caster : null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		glow(null, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public void turnOff() {
		for (Map.Entry<UUID, UUID> entry : new HashSet<>(glowing.entries())) {
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null) continue;
			Entity entity = Bukkit.getEntity(entry.getValue());
			if (entity == null) continue;
			removeGlow(player, entity);
		}
		for (UUID uuid : new HashSet<>(glowingUnpaired)) {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity == null) continue;
			removeGlow(null, entity);
		}
	}

	private boolean isGlowing(LivingEntity entity) {
		UUID uuid = entity.getUniqueId();
		return glowing.containsValue(uuid) || glowingUnpaired.contains(uuid);
	}

	private void glow(Player caster, LivingEntity target, float power, String[] args) {
		int duration = Math.round(this.duration * power);
		UUID uuid = target.getUniqueId();

		// Handle reapply and toggle.
		if (isGlowing(target)) {
			removeGlow(caster, target);
			if (toggle) return;
		}

		GlowAPI.setGlowing(target, color, getWatchers(caster, target));
		if (caster == null) glowingUnpaired.add(uuid);
		else glowing.put(caster.getUniqueId(), uuid);
		if (duration > 0) MagicSpells.scheduleDelayedTask(() -> removeGlow(caster, target), duration);

		// Play effects.
		if (caster == null) playSpellEffects(EffectPosition.TARGET, target);
		else playSpellEffects(caster, target);
		playSpellEffectsBuff(target, entity -> entity instanceof LivingEntity && isGlowing((LivingEntity) entity));
	}

	private void removeGlow(Player caster, Entity target) {
		GlowAPI.setGlowing(target, false, getWatchers(caster, target));
		UUID uuid = target.getUniqueId();
		if (caster == null) glowingUnpaired.remove(uuid);
		else glowing.remove(caster.getUniqueId(), uuid);
	}

	private Collection<Player> getWatchers(Player caster, Entity target) {
		List<Player> watchers = new ArrayList<>();
		if (clientSide) watchers.addAll(Bukkit.getOnlinePlayers());
		else if (caster != null) watchers.add(caster);
		if (visibleToTarget && target instanceof Player) watchers.add((Player) target);
		return watchers;
	}

}
