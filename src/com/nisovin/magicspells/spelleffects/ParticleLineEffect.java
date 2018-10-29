package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.ParticleNameUtil;

public class ParticleLineEffect extends ParticlesEffect {
		
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			String[] data = string.split(" ");
			if (data.length >= 1) distanceBetween = Float.parseFloat(data[0]);
			if (data.length >= 2) name = data[1];
			if (data.length >= 3) {
				xSpread = Float.parseFloat(data[2]);
				zSpread = xSpread;
			}
			if (data.length >= 4) ySpread = Float.parseFloat(data[3]);
			if (data.length >= 5) speed = Float.parseFloat(data[4]);
			if (data.length >= 6) count = Integer.parseInt(data[5]);
			if (data.length >= 7) yOffset = Float.parseFloat(data[6]);
			if (data.length >= 8) color = ColorUtil.getColorFromHexString(data[7]);
		}
		EffectPackage pkg = ParticleNameUtil.findEffectPackage(name);
		canRender = pkg.canRender();
		if (canRender) {
			data = pkg.data;
			effect = pkg.effect;
		}
	}
	
	@Override
	public Runnable playEffect(Location location1, Location location2) {
		int c = (int)Math.ceil(location1.distance(location2) / distanceBetween) - 1;
		if (c <= 0) return null;
		Vector v = location2.toVector().subtract(location1.toVector()).normalize().multiply(distanceBetween);
		Location l = location1.clone();
		l = l.add(0, yOffset, 0);
		for (int i = 0; i < c; i++) {
			
			l.add(v);
			
			//MagicSpells.getVolatileCodeHandler().playParticleEffect(l, name, horizSpread, vertSpread, speed, count, 15, yOffset);
			//Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset
			DebugHandler.debugEffectInfo("Playing particle line effect: location = " + l.toString());
			effect.display(data, l, color, renderDistance, xSpread, ySpread, zSpread, speed, count);
			//ParticleData data, Location center, Color color, double range, float offsetX, float offsetY, float offsetZ, float speed, int amount
			//MagicSpells.getVolatileCodeHandler().playParticleEffect(l, name, xSpread, ySpread, zSpread, speed, count, 15, yOffset);
		}
		return null;
	}

}
