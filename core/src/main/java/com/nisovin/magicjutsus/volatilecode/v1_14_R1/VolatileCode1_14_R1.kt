package com.nisovin.magicjutsus.volatilecode.v1_14_R1

import java.util.UUID

import java.io.File
import java.io.FileWriter
import java.lang.reflect.Field

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.util.Vector
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.craftbukkit.v1_14_R1.entity.*
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.craftbukkit.v1_14_R1.CraftServer
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack

import com.nisovin.magicjutsus.util.*
import com.nisovin.magicjutsus.MagicJutsus
import com.nisovin.magicjutsus.util.compat.EventUtil
import com.nisovin.magicjutsus.volatilecode.VolatileCodeDisabled
import com.nisovin.magicjutsus.volatilecode.VolatileCodeHandle

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property

import net.minecraft.server.v1_14_R1.*

private typealias nmsItemStack = net.minecraft.server.v1_14_R1.ItemStack

class VolatileCode1_14_R1: VolatileCodeHandle {

    private var fallback = VolatileCodeDisabled()

    private var entityFallingBlockFallHurtAmountField: Field? = null
    private var entityFallingBlockFallHurtMaxField: Field? = null
    private var craftMetaSkullClass: Class<*>? = null
    private var craftMetaSkullProfileField: Field? = null

    init {
        try {
            this.entityFallingBlockFallHurtAmountField = EntityFallingBlock::class.java.getDeclaredField("fallHurtAmount")
            this.entityFallingBlockFallHurtAmountField!!.isAccessible = true

            this.entityFallingBlockFallHurtMaxField = EntityFallingBlock::class.java.getDeclaredField("fallHurtMax")
            this.entityFallingBlockFallHurtMaxField!!.isAccessible = true

            this.craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMetaSkull")
            this.craftMetaSkullProfileField = this.craftMetaSkullClass!!.getDeclaredField("profile")
            this.craftMetaSkullProfileField!!.isAccessible = true
        } catch (e: Exception) {
            MagicJutsus.error("THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR 1.14, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICJUTSUS.")
            e.printStackTrace()
        }
    }

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Int) {
        /*final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
        final DataWatcher dw = el.getDataWatcher();
        dw.watch(7, Integer.valueOf(color));

        if (duration > 0) {
            MagicJutsus.scheduleDelayedTask(new Runnable() {
                public void run() {
                    int c = 0;
                    if (!el.effects.isEmpty()) {
                        c = net.minecraft.server.v1_12_R1.PotionBrewer.a(el.effects.values());
                    }
                    dw.watch(7, Integer.valueOf(c));
                }
            }, duration);
        }*/
    }

    override fun creaturePathToLoc(creature: Creature, loc: Location, speed: Float) {
        val entity = (creature as CraftCreature).handle
        val pathEntity = entity.navigation.a(loc.x, loc.y, loc.z, 1)
        entity.navigation.a(pathEntity, speed.toDouble())
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem: nmsItemStack?
        if (item != null) {
            nmsItem = CraftItemStack.asNMSCopy(item)
        } else {
            nmsItem = null
        }
        val packet = PacketPlayOutSetSlot(0, slot.toShort() + 36, nmsItem!!)
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        val e = EntityTNTPrimed((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        EventUtil.call(event)
        return event.isCancelled
    }

    override fun createExplosionByEntity(entity: Entity, location: Location, size: Float, fire: Boolean, breakBlocks: Boolean): Boolean {
        // Due to the way MagicJutsus is set up, the new method introduced for this in 1.14 can't be used properly
        // return location.world!!.createExplosion(location, size, fire, entity)
        return !(location.world as CraftWorld).handle.createExplosion((entity as CraftEntity).handle, location.x, location.y, location.z, size, fire, if (breakBlocks) Explosion.Effect.BREAK else Explosion.Effect.NONE).wasCanceled
    }

    override fun setExperienceBar(player: Player, level: Int, percent: Float) {
        val packet = PacketPlayOutExperience(percent, player.totalExperience, level)
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    override fun setTarget(entity: LivingEntity?, target: LivingEntity?) {
        if (entity is Creature) {
            entity.target = target
        } else {
            ((entity as CraftLivingEntity).handle as EntityInsentient).setGoalTarget((target as CraftLivingEntity).handle, EntityTargetEvent.TargetReason.CUSTOM, true)
        }
    }

    override fun setFallingBlockHurtEntities(block: FallingBlock, damage: Float, max: Int) {
        val efb = (block as CraftFallingBlock).handle
        try {
            block.setHurtEntities(true)
            this.entityFallingBlockFallHurtAmountField!!.setFloat(efb, damage)
            this.entityFallingBlockFallHurtMaxField!!.setInt(efb, max)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun playDragonDeathEffect(location: Location) {
        val dragon = EntityEnderDragon(EntityTypes.ENDER_DRAGON, (location.world as CraftWorld).handle)
        dragon.setPositionRotation(location.x, location.y, location.z, location.yaw, 0f)

        val packet24 = PacketPlayOutSpawnEntityLiving(dragon)
        val packet38 = PacketPlayOutEntityStatus(dragon, 3.toByte())
        val packet29 = PacketPlayOutEntityDestroy(dragon.bukkitEntity.entityId)

        val box = BoundingBox(location, 64.0)
        val players = ArrayList<Player>()
        for (player in location.world!!.players) {
            if (!box.contains(player)) continue
            players.add(player)
            (player as CraftPlayer).handle.playerConnection.sendPacket(packet24)
            player.handle.playerConnection.sendPacket(packet38)
        }

        MagicJutsus.scheduleDelayedTask({
            for (player in players) {
                if (player.isValid) {
                    (player as CraftPlayer).handle.playerConnection.sendPacket(packet29)
                }
            }
        }, 250)
    }

    override fun setKiller(entity: LivingEntity, killer: Player) {
        (entity as CraftLivingEntity).handle.killer = (killer as CraftPlayer).handle
    }

    override fun addAILookAtPlayer(entity: LivingEntity, range: Int) {
        try {
            val ev = (entity as CraftLivingEntity).handle as EntityInsentient

            val goalsField = EntityInsentient::class.java.getDeclaredField("goalSelector")
            goalsField.isAccessible = true
            val goals = goalsField.get(ev) as PathfinderGoalSelector

            goals.a(1, PathfinderGoalLookAtPlayer(ev, EntityHuman::class.java, range.toFloat(), 1.0f))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun saveSkinData(player: Player, name: String) {
        val profile = (player as CraftPlayer).handle.profile
        val props = profile.properties.get("textures")
        for (prop in props) {
            val skin = prop.value
            val sig = prop.signature

            val folder = File(MagicJutsus.getInstance().dataFolder, "disguiseskins")
            if (!folder.exists()) folder.mkdir()
            val skinFile = File(folder, "$name.skin.txt")
            val sigFile = File(folder, "$name.sig.txt")
            try {
                var writer = FileWriter(skinFile)
                writer.write(skin)
                writer.flush()
                writer.close()
                writer = FileWriter(sigFile)
                writer.write(sig)
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            break
        }
    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = PacketPlayOutEntityVelocity(player.entityId, Vec3D(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    override fun getAbsorptionHearts(entity: LivingEntity): Double {
        return (entity as CraftLivingEntity).handle.absorptionHearts.toDouble()
    }

    override fun setTexture(meta: SkullMeta, texture: String, signature: String) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField)) return

        try {
            val profile = this.craftMetaSkullProfileField!!.get(meta) as GameProfile
            setTexture(profile, texture, signature)
            this.craftMetaSkullProfileField!!.set(meta, profile)
        } catch (e: SecurityException) {
            MagicJutsus.handleException(e)
        } catch (e: IllegalArgumentException) {
            MagicJutsus.handleException(e)
        } catch (e: IllegalAccessException) {
            MagicJutsus.handleException(e)
        }

    }

    override fun setSkin(player: Player, skin: String, signature: String) {
        val craftPlayer = player as CraftPlayer
        setTexture(craftPlayer.profile, skin, signature)
    }

    private fun setTexture(profile: GameProfile, texture: String, signature: String?): GameProfile {
        if (signature == null || signature.isEmpty()) {
            profile.properties.put("textures", Property("textures", texture))
        } else {
            profile.properties.put("textures", Property("textures", texture, signature))
        }
        return profile
    }

    override fun setTexture(meta: SkullMeta, texture: String, signature: String, uuid: String?, name: String) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField)) return

        try {
            val profile = GameProfile(if (uuid != null) UUID.fromString(uuid) else null, name)
            setTexture(profile, texture, signature)
            this.craftMetaSkullProfileField!!.set(meta, profile)
        } catch (e: SecurityException) {
            MagicJutsus.handleException(e)
        } catch (e: IllegalArgumentException) {
            MagicJutsus.handleException(e)
        } catch (e: IllegalAccessException) {
            MagicJutsus.handleException(e)
        }

    }

    override fun getCustomModelData(meta: ItemMeta?): Int {
        if (meta == null) return 0
        if (meta.hasCustomModelData()) return meta.customModelData
        return 0
    }

    override fun setCustomModelData(meta: ItemMeta?, data: Int) {
        meta?.setCustomModelData(data)
    }

}
