package com.nisovin.magicspells.spelleffects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.ColorUtil;
import com.nisovin.magicspells.util.EffectPackage;
import com.nisovin.magicspells.util.ParticleNameUtil;

import de.slikey.effectlib.util.ParticleEffect;
import de.slikey.effectlib.util.ParticleEffect.ParticleData;

/**
 * ParticlesEffect<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>particle-name</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>horiz-spread</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>vert-spread</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>red</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>green</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>blue</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>speed</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>count</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>y-offset</code>
 *         </td>
 *         <td>
 *             Double
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>render-distance</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>color</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *     </tr>
 * </table>
 */
public class ParticlesEffect extends SpellEffect {
	
	ParticleEffect effect;
	String name = "explode";
	float xSpread = 0.2F;
	float ySpread = 0.2F;
	float zSpread = 0.2F;
	float speed = 0.2F;
	int count = 5;
	float yOffset = 0F;
	
	int renderDistance = 32;
	Color color = null;
	ParticleData data = null;

	boolean canRender;

	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			String[] data = string.split(" ");
			if (data.length >= 1) name = data[0];
			if (data.length >= 2) {
				xSpread = Float.parseFloat(data[1]);
				zSpread = xSpread;
			}
			if (data.length >= 3) ySpread = Float.parseFloat(data[2]);
			if (data.length >= 4) speed = Float.parseFloat(data[3]);
			if (data.length >= 5) count = Integer.parseInt(data[4]);
			if (data.length >= 6) yOffset = Float.parseFloat(data[5]);
			if (data.length >= 7) color = ColorUtil.getColorFromHexString(data[6]);
		}
		EffectPackage pkg = ParticleNameUtil.findEffectPackage(name);
		canRender = pkg.canRender();
		if (canRender) {
			data = pkg.data;
			effect = pkg.effect;
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		name = config.getString("particle-name", name);
		xSpread = (float)config.getDouble("horiz-spread", xSpread);
		ySpread = (float)config.getDouble("vert-spread", ySpread);
		zSpread = xSpread;
		xSpread = (float)config.getDouble("red", xSpread);
		ySpread = (float)config.getDouble("green", ySpread);
		zSpread = (float)config.getDouble("blue", zSpread);
		speed = (float)config.getDouble("speed", speed);
		count = config.getInt("count", count);
		yOffset = (float)config.getDouble("y-offset", yOffset);
		renderDistance = config.getInt("render-distance", renderDistance);
		color = ColorUtil.getColorFromHexString(config.getString("color", null));
		EffectPackage pkg = ParticleNameUtil.findEffectPackage(name);
		canRender = pkg.canRender();
		if (canRender) {
			data = pkg.data;
			effect = pkg.effect;
		}
	}
	
	@Override
	public Runnable playEffectLocation(Location location) {
		//ParticleData data, Location center, Color color, double range, float offsetX, float offsetY, float offsetZ, float speed, int amount
		if (canRender) effect.display(data, location.clone().add(0, yOffset, 0), color, renderDistance, xSpread, ySpread, zSpread, speed, count);
		//MagicSpells.getVolatileCodeHandler().playParticleEffect(location, name, xSpread, ySpread, zSpread, speed, count, renderDistance, yOffset);
		return null;
	}
	
}
