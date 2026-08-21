package de.MagicClutch.endportalcalculator.client

import com.google.gson.GsonBuilder
import de.MagicClutch.endportalcalculator.calc.StrongholdCalculator
import de.MagicClutch.endportalcalculator.calc.StrongholdEstimate
import de.MagicClutch.endportalcalculator.calc.ThrowMeasurement
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer
import kotlin.math.hypot

data class AddMeasurementResult(
    val count: Int,
    val tooClose: Boolean,
    val nearestDistanceBlocks: Double,
    val requiredBlocks: Double
)

object MeasurementStore {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val storeDir = FabricLoader.getInstance().configDir.resolve("endportalcalculator")

    private var worldKey: String = "unknown"
    private val measurements = mutableListOf<ThrowMeasurement>()

    var estimate: StrongholdEstimate? = null
        private set

    var selectedId: Long? = null
        private set

    fun toggleSelected(id: Long) {
        selectedId = if (selectedId == id) null else id
    }

    fun loadForWorld(key: String) {
        worldKey = key
        measurements.clear()
        val file = fileFor(key)
        if (file.exists()) {
            runCatching {
                file.reader().use { reader ->
                    gson.fromJson(reader, Array<ThrowMeasurement>::class.java)
                }
            }.getOrNull()?.let { measurements.addAll(it) }
        }
        recalculate()
    }

    fun unload() {
        saveCurrent()
        worldKey = "unknown"
        measurements.clear()
        estimate = null
    }

    fun all(): List<ThrowMeasurement> = measurements.toList()

    fun addMeasurement(measurement: ThrowMeasurement): AddMeasurementResult {
        val required = EndPortalCalculatorConfig.data.minThrowSeparationBlocks
        val nearest = measurements.minOfOrNull {
            hypot(it.playerX - measurement.playerX, it.playerZ - measurement.playerZ)
        } ?: Double.MAX_VALUE
        val tooClose = required > 0.0 && nearest < required

        measurements.add(measurement)
        recalculate()
        saveCurrent()
        return AddMeasurementResult(measurements.size, tooClose, nearest, required)
    }

    fun removeMeasurement(id: Long) {
        measurements.removeIf { it.id == id }
        if (selectedId == id) selectedId = null
        recalculate()
        saveCurrent()
    }

    fun clearAll() {
        measurements.clear()
        selectedId = null
        recalculate()
        saveCurrent()
    }

    private fun recalculate() {
        estimate = StrongholdCalculator.calculate(measurements)
    }

    private fun saveCurrent() {
        val file = fileFor(worldKey)
        Files.createDirectories(file.parent)
        file.writer().use { gson.toJson(measurements, it) }
    }

    private fun fileFor(key: String): Path = storeDir.resolve("$key.json")
}
