package com.nisovin.magicjutsus.jutsus.targeted;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

public class SlotSelectJutsu extends TargetedJutsu implements TargetedEntityJutsu {

    private boolean isVariable = false;

    private String variable;
    private int slot;
    private final boolean ignoreSlotBounds;

    public SlotSelectJutsu(MagicConfig config, String jutsuName) {
        super(config, jutsuName);
        if(isConfigString("slot")) {
            isVariable = true;
            variable = getConfigString("slot", null);
        }
        else slot = getConfigInt("slot", 0);
        ignoreSlotBounds = getConfigBoolean("ignore-slot-bounds", false);
    }

    @Override
    public void initialize() {
        super.initialize();
        if(isVariable && (variable == null || variable.isEmpty() || MagicJutsus.getVariableManager().getVariable(variable) == null)) {
            MagicJutsus.error("SlotSelectJutsu '" + internalName + "' has an invalid variable specified in 'slot'!");
        }
    }

    @Override
    public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
        if(state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
            TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
            if(targetInfo == null) return noTarget(livingEntity);
            Player target = targetInfo.getTarget();
            if(target == null) return noTarget(livingEntity);
            slotChange(target);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    @Override
    public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
        return slotChange(target);
    }

    @Override
    public boolean castAtEntity(LivingEntity target, float power) {
        return slotChange(target);
    }

    private boolean slotChange(LivingEntity target) {
        if(!(target instanceof Player)) return false;
        Player player = (Player) target;
        int newSlot = -1;
        if(isVariable) {
            if(variable == null || variable.isEmpty() || MagicJutsus.getVariableManager().getVariable(variable) == null) {
                MagicJutsus.error("SlotSelectJutsu '" + internalName + "' has an invalid variable specified in 'slot'!");
            }
            else newSlot = (int) Math.round(MagicJutsus.getVariableManager().getValue(variable, player));
        }
        else newSlot = slot;
        try {
            player.getInventory().setHeldItemSlot(newSlot);
        }
        catch(IllegalArgumentException e) {
            if(!ignoreSlotBounds) MagicJutsus.error("SlotSelectJutsu '" + internalName + "' attempted to set to a slot outside bounds (0-8)! If this is intended, set 'ignore-slot-bounds' to true.");
        }
        return true;
    }
}
