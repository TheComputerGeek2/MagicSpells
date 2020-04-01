package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class GillsJutsu extends BuffJutsu {

	private Map<UUID, ItemStack> fishes;

	private String headMaterialName;
	private Material headMaterial;

	private boolean headEffect;
	private boolean refillAirBar;
	
	public GillsJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		headMaterialName = getConfigString("head-block", "GLASS").toUpperCase();
		headMaterial = Material.getMaterial(headMaterialName);

		if (headMaterial == null || !headMaterial.isBlock()) {
			headMaterial = null;
			if (!headMaterialName.isEmpty()) MagicJutsus.error("GillsJutsu " + internalName + " has a wrong head-block defined! '" + headMaterialName + "'");
		}
		
		headEffect = getConfigBoolean("head-effect", true);
		refillAirBar = getConfigBoolean("refill-air-bar", true);
		
		fishes = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		if (headEffect && headMaterial != null) {
			EntityEquipment equipment = entity.getEquipment();
			ItemStack helmet = equipment.getHelmet();
			fishes.put(entity.getUniqueId(), helmet);
			equipment.setHelmet(new ItemStack(headMaterial));
			return true;
		}

		fishes.put(entity.getUniqueId(), null);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return fishes.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (headEffect && headMaterial != null) {
			EntityEquipment equipment = entity.getEquipment();
			equipment.setHelmet(fishes.get(entity.getUniqueId()));
		}

		fishes.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		if (headEffect && headMaterial != null) {
			for (UUID id : fishes.keySet()) {
				Entity entity = Bukkit.getEntity(id);
				if (!(entity instanceof LivingEntity)) continue;
				LivingEntity livingEntity = (LivingEntity) entity;
				if (!livingEntity.isValid()) continue;

				EntityEquipment equipment = livingEntity.getEquipment();

				equipment.setHelmet(fishes.get(id));
			}
		}

		fishes.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (event.getCause() != DamageCause.DROWNING) return;
		
		LivingEntity livingEntity = (LivingEntity) entity;
		if (!isActive(livingEntity)) return;
		if (isExpired(livingEntity)) {
			turnOff(livingEntity);
			return;
		}

		event.setCancelled(true);
		addUseAndChargeCost(livingEntity);
		if (refillAirBar) livingEntity.setRemainingAir(livingEntity.getMaximumAir());

	}

}
