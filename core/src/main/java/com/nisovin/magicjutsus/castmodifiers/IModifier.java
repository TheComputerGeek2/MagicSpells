package com.nisovin.magicjutsus.castmodifiers;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

public interface IModifier {

	boolean apply(JutsuCastEvent event);
	boolean apply(ManaChangeEvent event);
	boolean apply(JutsuTargetEvent event);
	boolean apply(JutsuTargetLocationEvent event);
	boolean apply(MagicJutsusGenericPlayerEvent event);

	boolean check(LivingEntity livingEntity);
	boolean check(LivingEntity livingEntity, LivingEntity entity);
	boolean check(LivingEntity livingEntity, Location location);

}
