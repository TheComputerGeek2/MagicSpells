package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PackageVisibleField", "unused", "WeakerAccess", "FieldMayBeFinal"})
public class CaptureSpell extends TargetedSpell implements TargetedEntitySpell {
    
    boolean powerAffectsQuantity;
    boolean addToInventory;
    String itemName;
    List<String> itemLore;
    private boolean gravity;
    
    public CaptureSpell(final MagicConfig config, final String spellName) {
        super(config, spellName);
        
        powerAffectsQuantity = getConfigBoolean("power-affects-quantity", true);
        addToInventory = getConfigBoolean("add-to-inventory", false);
        gravity = getConfigBoolean("gravity", true);
        itemName = getConfigString("item-name", null);
        itemLore = getConfigStringList("item-lore", null);
        
        if(itemName != null) {
            itemName = ChatColor.translateAlternateColorCodes('&', itemName);
        }
        if(itemLore != null) {
            for(int i = 0; i < itemLore.size(); i++) {
                itemLore.set(i, ChatColor.translateAlternateColorCodes('&', itemLore.get(i)));
            }
        }
    }
    
    @Override
    public PostCastAction castSpell(final Player player, final SpellCastState state, final float power, final String[] args) {
        if(state == SpellCastState.NORMAL) {
            final TargetInfo<LivingEntity> target = getTargetedEntity(player, power, getValidTargetChecker());
            if(target == null) {
                return noTarget(player);
            }
            final boolean ok = capture(player, target.getTarget(), target.getPower());
            if(!ok) {
                return noTarget(player);
            }
            sendMessages(player, target.getTarget());
            return PostCastAction.NO_MESSAGES;
        }
        return PostCastAction.HANDLE_NORMALLY;
    }
    
    @Override
    public ValidTargetChecker getValidTargetChecker() {
        return entity -> !(entity instanceof Player) && entity.getType().isSpawnable();
    }
    
    @SuppressWarnings("TypeMayBeWeakened")
    boolean capture(final Player caster, final LivingEntity target, final float power) {
        final ItemStack item = Util.getEggItemForEntityType(target.getType());
        //noinspection ConstantConditions
        if(item != null) {
            if(powerAffectsQuantity) {
                final int q = Math.round(power);
                if(q > 1) {
                    item.setAmount(q);
                }
            }
            String entityName = MagicSpells.getEntityNames().get(target.getType());
            if(itemName != null || itemLore != null) {
                if(entityName == null) {
                    entityName = "unknown";
                }
                final ItemMeta meta = item.getItemMeta();
                if(itemName != null) {
                    meta.setDisplayName(itemName.replace("%name%", entityName));
                }
                if(itemLore != null) {
                    final List<String> lore = new ArrayList<>();
                    for(final String l : itemLore) {
                        lore.add(l.replace("%name%", entityName));
                    }
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }
            target.remove();
            boolean added = false;
            if(addToInventory && caster != null) {
                added = Util.addToInventory(caster.getInventory(), item, true, false);
            }
            if(!added) {
                final Item dropped = target.getWorld().dropItem(target.getLocation().add(0, 1, 0), item);
                dropped.setItemStack(item);
                MagicSpells.getVolatileCodeHandler().setGravity(dropped, gravity);
            }
            if(caster != null) {
                playSpellEffects(caster, target.getLocation());
            } else {
                playSpellEffects(EffectPosition.TARGET, target.getLocation());
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean castAtEntity(final Player caster, final LivingEntity target, final float power) {
        return target.getType().isSpawnable() && validTargetList.canTarget(caster, target) && capture(caster, target, power);
    }
    
    @Override
    public boolean castAtEntity(final LivingEntity target, final float power) {
        return target.getType().isSpawnable() && validTargetList.canTarget(target) && capture(null, target, power);
    }
}
