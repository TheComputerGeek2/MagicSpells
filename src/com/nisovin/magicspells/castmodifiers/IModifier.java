package com.nisovin.magicspells.castmodifiers;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public interface IModifier {
	public boolean apply(SpellCastEvent event);
	public boolean apply(ManaChangeEvent event);
	public boolean apply(SpellTargetEvent event);
	public boolean apply(SpellTargetLocationEvent event);
	public boolean apply(MagicSpellsGenericPlayerEvent event);
	public boolean check(Player player);

}
