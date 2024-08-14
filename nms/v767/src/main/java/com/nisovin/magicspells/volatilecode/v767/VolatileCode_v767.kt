package com.nisovin.magicspells.volatilecode.v767

import java.util.*
import java.lang.reflect.Method

import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.Location
import org.bukkit.util.Vector
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.inventory.SmithingTransformRecipe

import org.bukkit.craftbukkit.entity.*
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.inventory.CraftItemStack

import net.kyori.adventure.text.Component

import io.papermc.paper.util.MCUtil
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.advancement.AdvancementDisplay

import com.nisovin.magicspells.volatilecode.VolatileCodeHandle
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper

import net.minecraft.core.BlockPos
import net.minecraft.advancements.*
import net.minecraft.util.FastColor
import net.minecraft.world.phys.Vec3
import net.minecraft.world.entity.EntityType
import net.minecraft.network.protocol.game.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.advancements.critereon.ImpossibleTrigger
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload

class VolatileCode_v767(helper: VolatileCodeHelper) : VolatileCodeHandle(helper) {

    private val toastKey = ResourceLocation.fromNamespaceAndPath("magicspells", "toast_effect")

    private var DATA_EFFECT_PARTICLES: EntityDataAccessor<List<ParticleOptions>>? = null
    private var DATA_EFFECT_AMBIENCE_ID: EntityDataAccessor<Boolean>? = null
    private var UPDATE_EFFECT_PARTICLES: Method? = null

    init {
        try {
            val dataEffectParticlesField = net.minecraft.world.entity.LivingEntity::class.java.getDeclaredField("DATA_EFFECT_PARTICLES")
            dataEffectParticlesField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            DATA_EFFECT_PARTICLES = dataEffectParticlesField.get(null) as EntityDataAccessor<List<ParticleOptions>>

            val dataEffectAmbienceIdField = net.minecraft.world.entity.LivingEntity::class.java.getDeclaredField("DATA_EFFECT_AMBIENCE_ID")
            dataEffectAmbienceIdField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            DATA_EFFECT_AMBIENCE_ID = dataEffectAmbienceIdField.get(null) as EntityDataAccessor<Boolean>

            UPDATE_EFFECT_PARTICLES = net.minecraft.world.entity.LivingEntity::class.java.getDeclaredMethod("updateSynchronizedMobEffectParticles")
            UPDATE_EFFECT_PARTICLES!!.isAccessible = true
        } catch (e: Exception) {
            helper.error("Encountered an error while creating the volatile code handler for " + Bukkit.getMinecraftVersion())
            e.printStackTrace()
        }
    }

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Long) {
        val livingEntity = (entity as CraftLivingEntity).handle
        val entityData = livingEntity.entityData

        entityData.set(
            DATA_EFFECT_PARTICLES!!, Collections.singletonList(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.color(255, color))
            )
        )

        entityData.set(DATA_EFFECT_AMBIENCE_ID!!, false)

        if (duration > 0)
            helper.scheduleDelayedTask({
                UPDATE_EFFECT_PARTICLES!!.invoke(livingEntity)
            }, duration)
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem = CraftItemStack.asNMSCopy(item)
        val packet = ClientboundContainerSetSlotPacket(0, 0, slot.toShort() + 36, nmsItem)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        val e = PrimedTnt((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }

    override fun playDragonDeathEffect(location: Location) {
        val dragon = EnderDragon(EntityType.ENDER_DRAGON, (location.world as CraftWorld).handle)
        dragon.setPos(location.x, location.y, location.z)

        val addMobPacket = ClientboundAddEntityPacket(dragon, 0, BlockPos(location.blockX, location.blockY, location.blockZ))
        val entityEventPacket = ClientboundEntityEventPacket(dragon, 3)
        val removeEntityPacket = ClientboundRemoveEntitiesPacket(dragon.id)

        val players = ArrayList<Player>()
        for (player in location.getNearbyPlayers(64.0)) {
            players.add(player)
            (player as CraftPlayer).handle.connection.send(addMobPacket)
            player.handle.connection.send(addMobPacket)
            player.handle.connection.send(entityEventPacket)
        }

        helper.scheduleDelayedTask({
            for (player in players) {
                if (!player.isValid) continue
                (player as CraftPlayer).handle.connection.send(removeEntityPacket)
            }
        }, 250)

    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = ClientboundSetEntityMotionPacket(player.entityId, Vec3(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun startAutoSpinAttack(player: Player?, ticks: Int) {
        val entityPlayer = (player as CraftPlayer).handle
        entityPlayer.startAutoSpinAttack(ticks, 0f, net.minecraft.world.item.ItemStack.EMPTY)
    }

    override fun playHurtAnimation(entity: LivingEntity, yaw: Float) {
        val e = (entity as CraftLivingEntity).handle

        for (p : Player in entity.location.getNearbyPlayers((entity.server.simulationDistance * 16).toDouble())) {
            (p as CraftPlayer).handle.connection.send(ClientboundHurtAnimationPacket(e.id, 90 + yaw))
        }

        if (e.isSilent) return
        val sound = e.getHurtSound0(e.damageSources().generic())
        e.level().playSound(null, e.x, e.y, e.z, sound, e.soundSource, e.soundVolume, e.voicePitch)
    }

    override fun createSmithingRecipe(
        namespacedKey: NamespacedKey,
        result: ItemStack,
        template: RecipeChoice,
        base: RecipeChoice,
        addition: RecipeChoice,
        copyNbt: Boolean
    ): Recipe {
        return SmithingTransformRecipe(namespacedKey, result, template, base, addition, copyNbt)
    }

    override fun sendToastEffect(receiver: Player, icon: ItemStack, frameType: AdvancementDisplay.Frame, text: Component) {
        val iconNms = CraftItemStack.asNMSCopy(icon)
        val textNms = PaperAdventure.asVanilla(text)
        val description = PaperAdventure.asVanilla(Component.empty())
        val frame = try {
            AdvancementType.valueOf(frameType.name)
        } catch (_: IllegalArgumentException) {
            AdvancementType.TASK
        }

        val advancement = Advancement.Builder.advancement()
            .display(iconNms, textNms, description, null, frame, true, false, true)
            .addCriterion("impossible", Criterion(ImpossibleTrigger(), ImpossibleTrigger.TriggerInstance()))
            .build(toastKey)
        val progress = AdvancementProgress()
        progress.update(AdvancementRequirements(listOf(listOf("impossible"))))
        progress.grantProgress("impossible")

        val player = (receiver as CraftPlayer).handle
        player.connection.send(ClientboundUpdateAdvancementsPacket(
            false,
            Collections.singleton(advancement),
            Collections.emptySet(),
            Collections.singletonMap(toastKey, progress)
        ))
        player.connection.send(ClientboundUpdateAdvancementsPacket(
            false,
            Collections.emptySet(),
            Collections.singleton(toastKey),
            Collections.emptyMap()
        ))
    }

    override fun sendStatusUpdate(player: Player, health: Double, food: Int, saturation: Float) {
        var displayedHealth = health
        if (player.isHealthScaled) {
            displayedHealth = player.health / player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value * player.healthScale
        }

        (player as CraftPlayer).handle.connection.send(ClientboundSetHealthPacket(displayedHealth.toFloat(), food, saturation))
    }

    override fun addGameTestMarker(player: Player, location: Location, color: Int, name: String, lifetime: Int) {
        val payload = GameTestAddMarkerDebugPayload(MCUtil.toBlockPosition(location), color, name, lifetime)
        (player as CraftPlayer).handle.connection.send(ClientboundCustomPayloadPacket(payload))
    }

    override fun clearGameTestMarkers(player: Player) {
        val payload = GameTestClearMarkersDebugPayload()
        (player as CraftPlayer).handle.connection.send(ClientboundCustomPayloadPacket(payload))
    }

}
