package de.MagicClutch.endportalcalculator.calc

enum class MeasurementSource {
    AUTO,
    F3C
}

data class ThrowMeasurement(
    val id: Long,
    val playerX: Double,
    val playerY: Double,
    val playerZ: Double,
    val yawDegrees: Double,
    val pitchDegrees: Double?,
    val source: MeasurementSource,
    val timestamp: Long,
    val targetX: Double? = null,
    val targetZ: Double? = null
) {
    companion object {
        private var lastId = 0L

        @Synchronized
        fun nextId(): Long {
            val now = System.nanoTime()
            lastId = if (now > lastId) now else lastId + 1
            return lastId
        }
    }
}
