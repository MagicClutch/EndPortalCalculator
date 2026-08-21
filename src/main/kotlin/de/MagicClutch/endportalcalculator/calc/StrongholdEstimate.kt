package de.MagicClutch.endportalcalculator.calc

import kotlin.math.hypot

data class StrongholdEstimate(
    val x: Double,
    val z: Double,
    val accuracyRadius: Double,
    val confidenceRadius: Double,
    val throwsUsed: Int,
    val throwsTotal: Int,
    val outlierMeasurementIds: Set<Long>,
    val throwSpreadBlocks: Double
) {
    fun distanceFrom(playerX: Double, playerZ: Double): Double = hypot(x - playerX, z - playerZ)

    val lowSeparationWarning: Boolean get() = throwSpreadBlocks < LOW_SEPARATION_THRESHOLD_BLOCKS

    companion object {
        const val LOW_SEPARATION_THRESHOLD_BLOCKS = 30.0
    }
}
