package com.nisovin.magicspells.util.managers;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.spelleffects.*;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.spelleffects.effecttypes.*;

public class SpellEffectManager {

	private static final Map<String, Class<? extends SpellEffect>> spellEffects = new HashMap<>();

	public SpellEffectManager() {
		initialize();
	}

	public Map<String, Class<? extends SpellEffect>> getSpellEffects() {
		return spellEffects;
	}

	public void addSpellEffect(String name, Class<? extends SpellEffect> spellEffect) {
		spellEffects.put(name.toLowerCase(), spellEffect);
	}

	public void addSpellEffect(Class<? extends SpellEffect> spellEffect, String name) {
		spellEffects.put(name.toLowerCase(), spellEffect);
	}

	public void removeSpellEffect(String name) {
		spellEffects.remove(name.toLowerCase());
	}

	public SpellEffect getSpellEffectByName(String name) {
		Class<? extends SpellEffect> clazz = spellEffects.get(name.toLowerCase());
		if (clazz == null) return null;

		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}

	private void initialize() {
		addSpellEffect("armorstand", ArmorStandEffect.class);
		addSpellEffect("actionbartext", ActionBarTextEffect.class);
		addSpellEffect("bossbar", BossBarEffect.class);
		addSpellEffect("broadcast", BroadcastEffect.class);
		addSpellEffect("cloud", CloudEffect.class);
		addSpellEffect("dragondeath", DragonDeathEffect.class);
		addSpellEffect("ender", EnderSignalEffect.class);
		addSpellEffect("entity", EntityEffect.class);
		addSpellEffect("explosion", ExplosionEffect.class);
		addSpellEffect("fireworks", FireworksEffect.class);
		addSpellEffect("itemcooldown", ItemCooldownEffect.class);
		addSpellEffect("itemspray", ItemSprayEffect.class);
		addSpellEffect("lightning", LightningEffect.class);
		addSpellEffect("nova", NovaEffect.class);
		addSpellEffect("particles", ParticlesEffect.class);
		addSpellEffect("particlespersonal", ParticlesPersonalEffect.class);
		addSpellEffect("particlecloud", ParticleCloudEffect.class);
		addSpellEffect("potion", PotionEffect.class);
		addSpellEffect("smokeswirl", SmokeSwirlEffect.class);
		addSpellEffect("smoketrail", SmokeTrailEffect.class);
		addSpellEffect("sound", SoundEffect.class);
		addSpellEffect("soundpersonal", SoundPersonalEffect.class);
		addSpellEffect("spawn", MobSpawnerEffect.class);
		addSpellEffect("splash", SplashPotionEffect.class);
		addSpellEffect("title", TitleEffect.class);
		addSpellEffect("effectlib", EffectLibEffect.class);
		addSpellEffect("effectlibline", EffectLibLineEffect.class);
		addSpellEffect("effectlibentity", EffectLibEntityEffect.class);
	}

}
