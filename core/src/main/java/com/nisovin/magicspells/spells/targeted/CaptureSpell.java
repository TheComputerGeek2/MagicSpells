package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class CaptureSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final ValidTargetChecker CAPTURABLE = e -> Bukkit.getItemFactory().getSpawnEgg(e.getType()) != null;

	private final String itemName;
	private final List<String> itemLore;

	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> addToInventory;
	private final ConfigData<Boolean> powerAffectsQuantity;

	public CaptureSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		itemName = getConfigString("item-name", null);
		itemLore = getConfigStringList("item-lore", null);

		gravity = getConfigDataBoolean("gravity", true);
		addToInventory = getConfigDataBoolean("add-to-inventory", false);
		powerAffectsQuantity = getConfigDataBoolean("power-affects-quantity", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data, CAPTURABLE);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		LivingEntity target = data.target();

		Material material = Bukkit.getItemFactory().getSpawnEgg(target.getType());
		if (material == null) return noTarget(data);

		ItemStack item = new ItemStack(material);

		if (powerAffectsQuantity.get(data)) {
			int q = Math.round(data.power());
			if (q > 1) item.setAmount(q);
		}

		if (itemName != null || itemLore != null) {
			String[] replacements = {"%name%", getTargetName(target)};

			item.editMeta(meta -> {
				if (itemName != null) meta.displayName(Util.getItemMiniMessage(itemName, data, replacements));

				if (itemLore != null) {
					List<Component> lore = new ArrayList<>();
					for (String line : itemLore) lore.add(Util.getItemMiniMessage(line, data, replacements));
					meta.lore(lore);
				}
			});
		}

		target.remove();
		boolean added = false;

		if (addToInventory.get(data) && data.caster() instanceof Player player)
			added = Util.addToInventory(player.getInventory(), item, true, false);

		if (!added) {
			Item dropped = target.getWorld().dropItem(target.getLocation().add(0, 1, 0), item);
			dropped.setItemStack(item);
			dropped.setGravity(gravity.get(data));
		}

		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return CAPTURABLE;
	}

}
