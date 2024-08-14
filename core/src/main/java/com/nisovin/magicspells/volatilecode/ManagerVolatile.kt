package com.nisovin.magicspells.volatilecode

import org.bukkit.Bukkit

import com.nisovin.magicspells.MagicSpells

object ManagerVolatile {

    private val helper = object: VolatileCodeHelper {
        override fun error(message: String) {
            MagicSpells.error(message)
        }

        override fun scheduleDelayedTask(task: Runnable?, delay: Long): Int {
            return MagicSpells.scheduleDelayedTask(task, delay)
        }
    }

    fun constructVolatileCodeHandler(): VolatileCodeHandle {
        val mcVersion = Bukkit.getMinecraftVersion()
        return try {
            @Suppress("DEPRECATION")
            val version = "v" + Bukkit.getUnsafe().protocolVersion
            val volatileCode = Class.forName("com.nisovin.magicspells.volatilecode.$version.VolatileCode_$version")

            MagicSpells.log("Found volatile code handler for $mcVersion.")
            volatileCode.getConstructor(VolatileCodeHelper::class.java).newInstance(helper) as VolatileCodeHandle
        } catch (ex: Exception) {
            MagicSpells.error("Volatile code handler for $mcVersion not found, using fallback.")
            VolatileCodeDisabled()
        }
    }
}
