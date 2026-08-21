package de.MagicClutch.endportalcalculator.client.hud

import de.MagicClutch.endportalcalculator.Endportalcalculator
import de.MagicClutch.endportalcalculator.calc.StrongholdCalculator
import de.MagicClutch.endportalcalculator.client.MeasurementStore
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import kotlin.math.ceil
import kotlin.math.floor

object StrongholdWaypointLabel {

    private const val VISIBLE_DEGREE_RANGE = 60.0
    private const val BAR_WIDTH = 182.0
    private const val DOT_SIZE = 9
    private const val BAR_HEIGHT = 24
    private const val BAR_MARGIN_BOTTOM = 5
    private const val LABEL_GAP = 10

    private val LABEL_COLOR = 0xFF40FF80.toInt()

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(Endportalcalculator.MOD_ID, "stronghold_waypoint_label"),
            HudElement { graphics, _ -> render(graphics) }
        )
    }

    private fun render(graphics: GuiGraphicsExtractor) {
        if (!EndPortalCalculatorConfig.data.createWaypointEnabled) return
        val estimate = MeasurementStore.estimate ?: return
        val client = Minecraft.getInstance()
        if (client.level == null) return
        val camera = client.gameRenderer.mainCamera()

        val dx = estimate.x - camera.position().x
        val dz = estimate.z - camera.position().z
        if (dx * dx + dz * dz < 1.0) return

        val absoluteYaw = StrongholdCalculator.yawFromDirection(dx, dz)
        val relativeYaw = wrapDegrees(absoluteYaw - camera.yRot())
        if (relativeYaw <= -VISIBLE_DEGREE_RANGE || relativeYaw > VISIBLE_DEGREE_RANGE) return

        val guiWidth = graphics.guiWidth()
        val guiHeight = graphics.guiHeight()
        val centerX = ceil((guiWidth - DOT_SIZE) / 2.0).toInt()
        val dotX = centerX + floor(relativeYaw * BAR_WIDTH / 2.0 / VISIBLE_DEGREE_RANGE).toInt()
        val dotY = guiHeight - BAR_HEIGHT - BAR_MARGIN_BOTTOM - 2

        val font = client.font
        val text = "Stronghold"
        val textX = dotX + DOT_SIZE / 2 - font.width(text) / 2
        val textY = dotY - LABEL_GAP
        graphics.text(font, text, textX, textY, LABEL_COLOR, true)
    }

    private fun wrapDegrees(degrees: Double): Double {
        var d = degrees % 360.0
        if (d >= 180.0) d -= 360.0
        if (d < -180.0) d += 360.0
        return d
    }
}
