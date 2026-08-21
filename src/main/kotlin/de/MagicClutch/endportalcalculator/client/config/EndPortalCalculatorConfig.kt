package de.MagicClutch.endportalcalculator.client.config

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer

object EndPortalCalculatorConfig {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path = FabricLoader.getInstance().configDir.resolve("endportalcalculator.json")

    data class Data(
        var captureMode: CaptureMode = CaptureMode.AUTO,
        var hudEnabled: Boolean = true,
        var worldRenderEnabled: Boolean = true,
        var createWaypointEnabled: Boolean = true,
        var minThrowSeparationBlocks: Double = 20.0
    )

    var data: Data = Data()
        private set

    fun load() {
        data = if (path.exists()) {
            runCatching { path.reader().use { gson.fromJson(it, Data::class.java) } }.getOrNull() ?: Data()
        } else {
            Data()
        }
        save()
    }

    fun save() {
        Files.createDirectories(path.parent)
        path.writer().use { gson.toJson(data, it) }
    }

    fun setCaptureMode(mode: CaptureMode) {
        data.captureMode = mode
        save()
    }

    fun setHudEnabled(enabled: Boolean) {
        data.hudEnabled = enabled
        save()
    }

    fun setWorldRenderEnabled(enabled: Boolean) {
        data.worldRenderEnabled = enabled
        save()
    }

    fun setCreateWaypointEnabled(enabled: Boolean) {
        data.createWaypointEnabled = enabled
        save()
    }

    fun setMinThrowSeparationBlocks(blocks: Double) {
        data.minThrowSeparationBlocks = blocks.coerceAtLeast(0.0)
        save()
    }
}
