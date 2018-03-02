package com.nisovin.magicspells.spelleffects;

import com.nisovin.magicspells.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

/**
 * ActionBarTextEffect<br>
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
 *             <code>message</code>
 *         </td>
 *         <td>
 *             String
 *         </td>
 *         <td>
 *             The message to display.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             <code>broadcast</code>
 *         </td>
 *         <td>
 *             boolean
 *         </td>
 *         <td>
 *             If true, the effect will be sent to everyone online. If false, it will only be sent to ???
 *         </td>
 *     </tr>
 * </table>
 */
public class ActionBarTextEffect extends SpellEffect {
	String message = "";
	
	boolean broadcast = false;
	
	@Override
	public void loadFromString(String string) {
		message = ChatColor.translateAlternateColorCodes('&', string);
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		message = ChatColor.translateAlternateColorCodes('&', config.getString("message", message));
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
		MagicSpells.getVolatileCodeHandler().sendActionBarMessage(player, MagicSpells.plugin.doArgumentAndVariableSubstitution(message, player, null).replace("%t", player.getName()));
	}
	
}
