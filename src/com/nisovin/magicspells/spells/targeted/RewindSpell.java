package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RewindSpell extends TargetedSpell implements TargetedEntitySpell {

    private Map<LivingEntity, Rewinder> entities;

    private int tickInterval;
    private int startDuration;
    private int rewindInterval;
    private int specialEffectInterval;
    private int delayedEffectInterval;

    private boolean rewindMana;
    private boolean rewindHealth;
    private boolean allowForceRewind;

    private Subspell rewindSpell;
    private String rewindSpellName;

    public RewindSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        tickInterval = getConfigInt("tick-interval", 4);
        startDuration = getConfigInt("start-duration", 200);
        startDuration = (startDuration / tickInterval);
        rewindInterval = getConfigInt("rewind-interval", 2);

        rewindMana = getConfigBoolean("rewind-mana", false);
        rewindHealth = getConfigBoolean("rewind-health", true);
        rewindSpellName = getConfigString("spell-on-rewind", "");
        allowForceRewind = getConfigBoolean("allow-force-rewind", true);

        specialEffectInterval = getConfigInt("special-effect-interval", 5);
        delayedEffectInterval = getConfigInt("delayed-effect-interval", 5);

        entities = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();

        rewindSpell = new Subspell(rewindSpellName);
        if (!rewindSpell.process()) {
            if (!rewindSpellName.isEmpty()) MagicSpells.error("RewindSpell '" + this.internalName + "' has an invalid spell-on-rewind defined!");
            rewindSpell = null;
        }
    }

    @Override
    public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            if (targetSelf) {
                new Rewinder(player, player, power);
            } else {
                TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
                if (targetInfo == null) return noTarget(player);

                sendMessages(player, targetInfo.getTarget());
                new Rewinder(player, targetInfo.getTarget(), power);
            }
            playSpellEffects(EffectPosition.CASTER, player);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpellCast(SpellCastEvent e) {
        if (!allowForceRewind) return;
        Player pl = e.getCaster();
        if (!entities.containsKey(pl)) return;
        if (!e.getSpell().getInternalName().equals(this.internalName)) return;
        entities.get(pl).rewind();
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player pl = e.getPlayer();
        if (!entities.containsKey(pl)) return;
        entities.get(pl).stop();
    }

    @Override
    public boolean castAtEntity(Player player, LivingEntity livingEntity, float v) {
        new Rewinder(player, livingEntity, v);
        sendMessages(player, livingEntity);
        playSpellEffects(EffectPosition.CASTER, player);
        playSpellEffects(EffectPosition.TARGET, livingEntity);
        playSpellEffectsTrail(player.getLocation(), livingEntity.getLocation());
        return true;
    }

    @Override
    public boolean castAtEntity(LivingEntity livingEntity, float v) {
        new Rewinder(null, livingEntity, v);
        playSpellEffects(EffectPosition.TARGET, livingEntity);
        return true;
    }

    class Rewinder implements Runnable {

        int taskId;
        int counter = 0;

        int startMana;
        double startHealth;

        Player caster;
        float power;
        LivingEntity entity;
        List<Location> locations;

        public Rewinder(Player caster, LivingEntity entity, float power) {
            this.locations = new ArrayList<>();
            this.caster = caster;
            this.power = power;
            this.entity = entity;
            if (entity instanceof Player) this.startMana = MagicSpells.getManaHandler().getMana((Player) entity);
            this.startHealth = entity.getHealth();

            entities.put(entity, this);
            this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
        }

        @Override
        public void run() {
            // Save locations
            this.locations.add(this.entity.getLocation());
            // Loop through already saved locations and play effects with special position
            if (specialEffectInterval > 0 && this.counter % specialEffectInterval == 0) this.locations.forEach(loc -> playSpellEffects(EffectPosition.SPECIAL, loc));
            counter++;
            if (counter >= startDuration) rewind();
        }

        public void rewind() {
            MagicSpells.cancelTask(taskId);
            entities.remove(this.entity);
            if (rewindSpell != null) rewindSpell.cast(this.caster, this.power);
            new ForceRewinder(this.entity, this.locations, this.startHealth, this.startMana);
        }

        public void stop() {
            MagicSpells.cancelTask(taskId);
            entities.remove(this.entity);
        }
    }

    class ForceRewinder implements Runnable {

        int taskId;
        int counter;

        int startMana;
        double startHealth;
        LivingEntity entity;

        Location tempLocation;
        List<Location> locations;

        public ForceRewinder(LivingEntity entity, List<Location> locations, double startHealth, int startMana) {
            this.locations = locations;
            this.entity = entity;
            this.startMana = startMana;
            this.startHealth = startHealth;
            this.counter = locations.size();
            this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, rewindInterval);
        }

        @Override
        public void run() {
            // Check if the entity is valid and alive
            if (this.entity == null || !this.entity.isValid() || this.entity.isDead()) {
                cancel();
                return;
            }

            if (this.locations != null && this.locations.size() > 0) this.tempLocation = this.locations.get(this.counter - 1);
            if (this.tempLocation != null) {
                this.entity.teleport(this.tempLocation);
                this.locations.remove(tempLocation);
                if (delayedEffectInterval > 0 && this.counter % delayedEffectInterval == 0) this.locations.forEach(loc -> playSpellEffects(EffectPosition.DELAYED, loc));
            }

            counter--;
            if (counter <= 0) stop();
        }

        public void stop() {
            MagicSpells.cancelTask(taskId);
            if (rewindHealth) this.entity.setHealth(this.startHealth);
            if (this.entity instanceof Player && rewindMana) MagicSpells.getManaHandler().setMana((Player) this.entity, this.startMana, ManaChangeReason.OTHER);
        }

        public void cancel() {
            MagicSpells.cancelTask(taskId);
            this.locations.clear();
            this.locations = null;
        }
    }
}
