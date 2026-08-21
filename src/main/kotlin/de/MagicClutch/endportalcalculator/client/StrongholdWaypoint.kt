package de.MagicClutch.endportalcalculator.client

import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.waypoints.TrackedWaypoint
import net.minecraft.world.waypoints.Waypoint
import net.minecraft.world.waypoints.WaypointStyleAssets
import java.util.Optional
import java.util.UUID

object StrongholdWaypoint {

    private val WAYPOINT_ID: UUID = UUID.nameUUIDFromBytes("endportalcalculator:stronghold".toByteArray(Charsets.UTF_8))
    private val ICON_COLOR = 0xFF40FF80.toInt()

    private var trackedPos: BlockPos? = null

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { sync() }
    }

    fun reset() {
        trackedPos = null
    }

    private fun sync() {
        val player = Minecraft.getInstance().player
        val manager = player?.connection?.waypointManager
        if (manager == null) {
            trackedPos = null
            return
        }

        val estimate = MeasurementStore.estimate
        if (estimate == null || !EndPortalCalculatorConfig.data.createWaypointEnabled) {
            if (trackedPos != null) {
                manager.untrackWaypoint(TrackedWaypoint.empty(WAYPOINT_ID))
                trackedPos = null
            }
            return
        }

        val pos = BlockPos(estimate.x.toInt(), player.blockY, estimate.z.toInt())
        if (pos == trackedPos) return

        val icon = Waypoint.Icon()
        icon.style = WaypointStyleAssets.DEFAULT
        icon.color = Optional.of(ICON_COLOR)

        manager.trackWaypoint(TrackedWaypoint.setPosition(WAYPOINT_ID, icon, pos))
        trackedPos = pos
    }
}
