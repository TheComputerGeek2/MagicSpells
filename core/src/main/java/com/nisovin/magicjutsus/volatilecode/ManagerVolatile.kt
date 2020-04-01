package com.nisovin.magicjutsus.volatilecode

import org.bukkit.Bukkit

import com.nisovin.magicjutsus.MagicJutsus

object ManagerVolatile {

    fun constructVolatileCodeHandler(): VolatileCodeHandle {
        try {
            val nmsPackage = Bukkit.getServer().javaClass.getPackage().name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[3]
            val volatileCode = Class.forName("com.nisovin.magicjutsus.volatilecode.$nmsPackage.VolatileCode${nmsPackage.replace("v", "")}")

            MagicJutsus.log("Found volatile code handler for $nmsPackage.")
            return volatileCode.newInstance() as VolatileCodeHandle
        } catch (ex: Exception) {
            // No volatile code handler found
        }

        return VolatileCodeDisabled()
    }

}
