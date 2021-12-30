package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;
import com.nisovin.magicspells.util.OverridePriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.EnumSet;

import static org.bukkit.event.entity.EntityTargetEvent.TargetReason;

public class EntityTargetListener extends PassiveListener {

    private final EnumSet<TargetReason> targetReasons = EnumSet.noneOf(TargetReason.class);

    @Override
    public void initialize(String var) {
        if (var == null || var.isEmpty()) return;

        String[] split = var.split("\\|");
        for (String s : split) {
            s = s.trim();

            boolean isTargetReason = false;
            for (TargetReason r : TargetReason.values()) {
                if (!s.equalsIgnoreCase(r.name())) continue;

                targetReasons.add(r);
                isTargetReason = true;
                break;
            }
            if (isTargetReason) continue;

            if (targetReasons == null) {
                MagicSpells.error("Invalid target reason'" + s + "' in entity target trigger on passive spell '" + passiveSpell.getInternalName() + "'");
                continue;
            }
        }
    }

    @OverridePriority
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        LivingEntity target = (LivingEntity) event.getTarget();
        LivingEntity caster = (LivingEntity) event.getEntity();
        if (!isCancelStateOk(event.isCancelled())) return;
        if (!hasSpell(caster) || !canTrigger(caster)) return;
        if (!targetReasons.isEmpty() && !targetReasons.contains(event.getReason())) return;

        boolean casted = passiveSpell.activate(caster, target);
        if (cancelDefaultAction(casted, target)) event.setCancelled(true);
    }

}
