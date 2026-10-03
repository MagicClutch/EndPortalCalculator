package de.MagicClutch.endportalcalculator.client.hud

import de.MagicClutch.endportalcalculator.Endportalcalculator
import de.MagicClutch.endportalcalculator.client.MeasurementStore
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

object StrongholdHud {

    private const val LINE_HEIGHT = 10

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(Endportalcalculator.MOD_ID, "stronghold_hud"),
            HudElement { graphics, _ -> render(graphics) }
        )
    }

    private fun render(graphics: GuiGraphicsExtractor) {
        if (!EndPortalCalculatorConfig.data.modEnabled) return
        if (!EndPortalCalculatorConfig.data.hudEnabled) return
        val client = Minecraft.getInstance()
        if (client.level == null) return

        val font = client.font
        val lines = buildLines()
        val x = 6
        var y = 6
        val width = lines.maxOf { font.width(it) }
        graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size * LINE_HEIGHT + 1, 0x80000000.toInt())
        for (line in lines) {
            graphics.text(font, line, x, y, 0xFFFFFFFF.toInt(), true)
            y += LINE_HEIGHT
        }
    }

    private fun buildLines(): List<String> {
        val estimate = MeasurementStore.estimate
        val lines = mutableListOf("§b§lEND PORTAL CALCULATOR")
        if (estimate == null) {
            lines.add("§7Throws: ${MeasurementStore.all().size} (need 2+)")
            return lines
        }
        lines.add("§fStronghold")
        lines.add("§aX: ${estimate.x.toInt()}")
        lines.add("§aZ: ${estimate.z.toInt()}")
        lines.add("§7±${estimate.accuracyRadius.toInt()} blocks")
        lines.add("§7Throws: ${estimate.throwsUsed}/${estimate.throwsTotal}")
        if (estimate.lowSeparationWarning) {
            lines.add("§6⚠ Throws too close, move further")
        }
        return lines
    }
}
