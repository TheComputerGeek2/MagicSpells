package com.nisovin.magicspells.spells.targeted;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import net.kyori.adventure.text.format.NamedTextColor;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.glow.GlowManager;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.glow.impl.PacketEventsGlowManager;

public class GlowSpell extends TargetedSpell implements TargetedEntitySpell {

	private static GlowManager glowManager;

	private final ConfigData<Boolean> global;
	private final ConfigData<Boolean> remove;
	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> priority;
	private final ConfigData<NamespacedKey> key;
	private final ConfigData<NamedTextColor> color;

	public GlowSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		key = getConfigDataNamespacedKey("key", null);
		color = getConfigDataNamedTextColor("color", null);
		global = getConfigDataBoolean("global", true);
		remove = getConfigDataBoolean("remove", false);
		duration = getConfigDataInt("duration", 0);
		priority = getConfigDataInt("priority", 0);

		if (glowManager == null) {
			if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) glowManager = new PacketEventsGlowManager();
			else glowManager = MagicSpells.getVolatileCodeHandler().getGlowManager();

			glowManager.load();
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (global.get(data)) {
			NamespacedKey key = this.key.get(data);

			if (remove.get(data)) {
				if (key == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

				glowManager.removeGlow(data.target(), key);
			} else {
				glowManager.applyGlow(
					data.target(),
					key != null ? key : new NamespacedKey(MagicSpells.getInstance(), UUID.randomUUID().toString()),
					color.get(data),
					priority.get(data),
					duration.get(data)
				);
			}

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		NamespacedKey key = this.key.get(data);

		if (remove.get(data)) {
			if (key == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

			glowManager.removeGlow(caster, data.target(), key);
		} else {
			glowManager.applyGlow(
				caster,
				data.target(),
				key != null ? key : new NamespacedKey(MagicSpells.getInstance(), UUID.randomUUID().toString()),
				color.get(data),
				priority.get(data),
				duration.get(data)
			);
		}

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	protected void turnOff() {
		if (glowManager == null) return;

		glowManager.unload();
		glowManager = null;
	}

}
