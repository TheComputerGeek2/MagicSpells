package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.MagicConfig;

public class VolatileCodeUnknown extends VolatileCodeEnabledNMSBase {
    public VolatileCodeUnknown(MagicConfig config, String version) {
        super(config, version);
    }

    @Override
    public void removeAI(LivingEntity entity) {

    }

    @Override
    public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {

    }

    @Override
    public void addAILookAtPlayer(LivingEntity entity, int range) {

    }

    @Override
    public void resetEntityAttributes(LivingEntity entity) {

    }

}
