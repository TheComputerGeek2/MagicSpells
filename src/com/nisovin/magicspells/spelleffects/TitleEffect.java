package com.nisovin.magicspells.spelleffects;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

/**
 * TitleEffect<br>
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
 *         <th>
 *             Default
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>title</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             <code>null</code>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>subtitle</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>fade-in</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>stay</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *              ???
 *         </td>
 *         <td>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>fade-out</code>
 *         </td>
 *         <td>
 *             Integer
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>broadcast</code>
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *         </td>
 *     </tr>
 * </table>
 */
public class TitleEffect extends SpellEffect {
	String title = null;
	
	String subtitle = null;
	
	int fadeIn = 10;
	
	int stay = 40;
	
	int fadeOut = 10;
	
	boolean broadcast = false;
	
	@Override
	public void loadFromString(String string) {
		// No string format
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		title = config.getString("title", title);
		if (title != null) title = ChatColor.translateAlternateColorCodes('&', title);
		subtitle = config.getString("subtitle", subtitle);
		if (subtitle != null) subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
		fadeIn = config.getInt("fade-in", fadeIn);
		stay = config.getInt("stay", stay);
		fadeOut = config.getInt("fade-out", fadeOut);
		broadcast = config.getBoolean("broadcast", broadcast);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (broadcast) {
			Util.forEachPlayerOnline(this::send);
		} else if (entity instanceof Player) {
			send((Player) entity);
		}
		return null;
	}
	
	private void send(Player player) {
		MagicSpells.getVolatileCodeHandler().sendTitleToPlayer(player, MagicSpells.plugin.doArgumentAndVariableSubstitution(title, player, null), MagicSpells.plugin.doArgumentAndVariableSubstitution(subtitle, player, null), fadeIn, stay, fadeOut);
	}

}
