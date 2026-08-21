package de.MagicClutch.endportalcalculator.client

import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import de.MagicClutch.endportalcalculator.client.eye.EyeTracker
import de.MagicClutch.endportalcalculator.client.hud.StrongholdHud
import de.MagicClutch.endportalcalculator.client.hud.StrongholdWaypointLabel
import de.MagicClutch.endportalcalculator.client.input.Keybinds
import de.MagicClutch.endportalcalculator.client.render.StrongholdWorldRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

class EndportalcalculatorClient : ClientModInitializer {

    override fun onInitializeClient() {
        EndPortalCalculatorConfig.load()

        EyeTracker.register()
        Keybinds.register()
        StrongholdHud.register()
        StrongholdWorldRenderer.register()
        StrongholdWaypoint.register()
        StrongholdWaypointLabel.register()
        StrongholdTpCommand.register()

        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            MeasurementStore.loadForWorld(WorldKeys.current(client))
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            MeasurementStore.unload()
            StrongholdWaypoint.reset()
        }
    }
}
