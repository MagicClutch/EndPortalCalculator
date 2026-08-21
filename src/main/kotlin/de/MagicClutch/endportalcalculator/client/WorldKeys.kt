package de.MagicClutch.endportalcalculator.client

import net.minecraft.client.Minecraft

object WorldKeys {

    fun current(client: Minecraft): String {
        val server = client.singleplayerServer
        if (server != null) {
            return "sp_" + sanitize(server.worldData.levelName)
        }
        val serverData = client.currentServer
        if (serverData != null) {
            return "mp_" + sanitize(serverData.ip)
        }
        return "unknown"
    }

    private fun sanitize(raw: String): String {
        val cleaned = raw.trim().lowercase().replace(Regex("[^a-z0-9_.-]"), "_")
        return cleaned.ifBlank { "world" }
    }
}
