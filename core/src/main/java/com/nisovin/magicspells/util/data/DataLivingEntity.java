package com.nisovin.magicspells.util.data;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import org.bukkit.entity.LivingEntity;

public class DataLivingEntity {
	
	private static Map<String, Function<LivingEntity, String>> dataElements = new HashMap<>();
	
	static {
		dataElements.put("eyeheight", livingEntity -> livingEntity.getEyeHeight() + "");
		dataElements.put("maxair", livingEntity -> livingEntity.getMaximumAir() + "");
		dataElements.put("air", livingEntity -> livingEntity.getRemainingAir() + "");
		dataElements.put("maxnodamageticks", livingEntity -> livingEntity.getMaximumNoDamageTicks() + "");
		dataElements.put("targetedblock.x", livingEntity -> String.valueOf(livingEntity.getTargetBlockExact(32).getX()));
		dataElements.put("targetedblock.y", livingEntity -> String.valueOf(livingEntity.getTargetBlockExact(32).getY()));
		dataElements.put("targetedblock.z", livingEntity -> String.valueOf(livingEntity.getTargetBlockExact(32).getZ()));
		dataElements.put("eyelocation", livingEntity -> livingEntity.getEyeLocation().toString());
		dataElements.put("eyelocation.x", livingEntity -> livingEntity.getEyeLocation().getX() + "");
		dataElements.put("eyelocation.y", livingEntity -> livingEntity.getEyeLocation().getY() + "");
		dataElements.put("eyelocation.z", livingEntity -> livingEntity.getEyeLocation().getZ() + "");
		dataElements.put("eyelocation.pitch", livingEntity -> livingEntity.getEyeLocation().getPitch() + "");
		dataElements.put("eyelocation.yaw", livingEntity -> livingEntity.getEyeLocation().getYaw() + "");
		dataElements.put("nodamageticks", livingEntity -> livingEntity.getNoDamageTicks() + "");
		dataElements.put("health", livingEntity -> livingEntity.getHealth() + "");
	}

	public static Function<? super LivingEntity, String> getDataFunction(String elementId) {
		// See if it's here.
		Function<? super LivingEntity, String> ret = dataElements.get(elementId);
		if (ret != null) return ret;

		// See if DataEntity has it.
		ret = DataEntity.getDataFunction(elementId);
		return ret;
	}
	
}
