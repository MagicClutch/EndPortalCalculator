package de.MagicClutch.endportalcalculator.client

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

object NotificationBar {

    fun actionBar(text: String) {
        val client = Minecraft.getInstance()
        client.gui.hud.setOverlayMessage(Component.literal(text), false)
    }

    fun warning(text: String) {
        actionBar(text)
        Minecraft.getInstance().player?.playSound(SoundEvents.VILLAGER_NO, 0.6f, 1.0f)
    }

    fun chat(text: String) {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(text))
    }
}
