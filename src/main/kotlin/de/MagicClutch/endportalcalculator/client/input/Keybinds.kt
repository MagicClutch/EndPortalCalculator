package de.MagicClutch.endportalcalculator.client.input

import de.MagicClutch.endportalcalculator.Endportalcalculator
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import de.MagicClutch.endportalcalculator.client.eye.EyeTracker
import de.MagicClutch.endportalcalculator.client.gui.EndPortalCalculatorScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object Keybinds {

    private lateinit var openGuiKey: KeyMapping
    private lateinit var toggleHudKey: KeyMapping
    private var comboWasDown = false

    fun register() {
        val category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Endportalcalculator.MOD_ID, "category"))

        openGuiKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.endportalcalculator.open_gui", GLFW.GLFW_KEY_G, category)
        )
        toggleHudKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping("key.endportalcalculator.toggle_hud", GLFW.GLFW_KEY_UNKNOWN, category)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openGuiKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(EndPortalCalculatorScreen())
                }
            }
            while (toggleHudKey.consumeClick()) {
                EndPortalCalculatorConfig.setHudEnabled(!EndPortalCalculatorConfig.data.hudEnabled)
            }

            if (client.player != null && client.level != null && client.gui.screen() == null) {
                val windowHandle = client.window.handle()
                val comboDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS &&
                    GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS
                if (comboDown && !comboWasDown) {
                    EyeTracker.tryCaptureF3C()
                }
                comboWasDown = comboDown
            } else {
                comboWasDown = false
            }
        }
    }
}
