package com.nisovin.magicspells.volatilecode.v1_18_R1

import org.bukkit.entity.*
import org.bukkit.Location
import org.bukkit.util.Vector
import org.bukkit.inventory.ItemStack
import org.bukkit.craftbukkit.v1_18_R1.entity.*
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack

import com.nisovin.magicspells.volatilecode.VolatileCodeHandle

import net.minecraft.world.phys.Vec3D
import net.minecraft.network.protocol.game.*

private typealias nmsItemStack = net.minecraft.world.item.ItemStack

class VolatileCode1_18_R1: VolatileCodeHandle {

    /*private var entityLivingPotionEffectColor: DataWatcherObject<Int>? = null

    init {
        try {
            val entityLivingPotionEffectColorField = EntityLiving::class.java.getDeclaredField("bK")
            entityLivingPotionEffectColorField.isAccessible = true
            this.entityLivingPotionEffectColor = entityLivingPotionEffectColorField.get(null) as DataWatcherObject<Int>
        } catch (e: Exception) {
            MagicSpells.error("THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR 1.18, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICSPELLS.")
            e.printStackTrace()
        }
    }*/

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Int) {
        val livingEntity = (entity as CraftLivingEntity).handle;
        /*val dataWatcher = livingEntity.dataWatcher;
        dataWatcher.set(entityLivingPotionEffectColor, color)
        if (duration > 0) {
            MagicSpells.scheduleDelayedTask({
                var c = 0
                if (livingEntity.effects.isNotEmpty()) {
                    c = PotionUtil.a(livingEntity.effects)
                }
                dataWatcher.set(entityLivingPotionEffectColor, c)
            }, duration)
        }*/
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem: nmsItemStack?
        if (item != null) nmsItem = CraftItemStack.asNMSCopy(item)
        else nmsItem = null
        val packet = PacketPlayOutSetSlot(0, 0, slot.toShort() + 36, nmsItem!!)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        /*val e = EntityTNTPrimed((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        EventUtil.call(event)
        return event.isCancelled*/
        return false
    }

    override fun setFallingBlockHurtEntities(block: FallingBlock, damage: Float, max: Int) {
        val efb = (block as CraftFallingBlock).handle
        block.setHurtEntities(true)
        efb.setHurtsEntities(damage, max)
    }

    override fun playDragonDeathEffect(location: Location) {
        /*val dragon = EntityEnderDragon(EntityTypes.v, (location.world as CraftWorld).handle)
        dragon.setPositionRotation(location.x, location.y, location.z, location.yaw, 0f)

        val packet24 = PacketPlayOutSpawnEntityLiving(dragon)
        val packet38 = PacketPlayOutEntityStatus(dragon, 3.toByte())
        val packet29 = PacketPlayOutEntityDestroy(dragon.bukkitEntity.entityId)

        val box = BoundingBox(location, 64.0)
        val players = ArrayList<Player>()
        for (player in location.world!!.players) {
            if (!box.contains(player)) continue
            players.add(player)
            (player as CraftPlayer).handle.b.sendPacket(packet24)
            player.handle.b.sendPacket(packet38)
        }

        MagicSpells.scheduleDelayedTask({
            for (player in players) {
                if (player.isValid) {
                    (player as CraftPlayer).handle.b.sendPacket(packet29)
                }
            }
        }, 250)*/
    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = PacketPlayOutEntityVelocity(player.entityId, Vec3D(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun setInventoryTitle(player: Player, title: String) {
        val entityPlayer = (player as CraftPlayer).handle
        /*val container = entityPlayer.bV
        val packet = PacketPlayOutOpenWindow(container.j, container.type, ChatMessage(title))
        entityPlayer.b.sendPacket(packet)
        player.updateInventory()*/
    }

}

