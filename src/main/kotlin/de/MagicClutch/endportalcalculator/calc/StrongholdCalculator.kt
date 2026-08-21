package de.MagicClutch.endportalcalculator.calc

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object StrongholdCalculator {

    private const val MIN_DET = 1e-9
    private const val OUTLIER_MIN_MEASUREMENTS = 5
    private const val OUTLIER_MAD_SCALE = 1.4826
    private const val OUTLIER_SIGMA_MULTIPLIER = 3.0
    private const val OUTLIER_FLOOR_BLOCKS = 6.0
    private const val TWO_THROW_CONFIDENCE_BLOCKS = 128.0
    private const val CONFIDENCE_Z = 1.5

    private class Line(val measurement: ThrowMeasurement, val px: Double, val pz: Double, val nx: Double, val nz: Double) {
        val b: Double = nx * px + nz * pz

        fun residual(x: Double, z: Double): Double = nx * x + nz * z - b
    }

    fun directionFromYaw(yawDegrees: Double): Pair<Double, Double> {
        val yawRad = Math.toRadians(yawDegrees)
        return -sin(yawRad) to cos(yawRad)
    }

    fun yawFromDirection(dx: Double, dz: Double): Double = Math.toDegrees(Math.atan2(-dx, dz))

    fun calculate(measurements: List<ThrowMeasurement>): StrongholdEstimate? {
        val usable = measurements.filter {
            it.playerX.isFinite() && it.playerZ.isFinite() && it.yawDegrees.isFinite()
        }
        if (usable.size < 2) return null

        val allLines = usable.map(::toLine)
        val fitAll = solve(allLines) ?: return null

        val (lines, outlierIds) = if (usable.size >= OUTLIER_MIN_MEASUREMENTS) {
            rejectOutliers(allLines, fitAll)
        } else {
            allLines to emptySet()
        }

        val fit = if (lines.size >= 2) solve(lines) ?: fitAll else fitAll
        val usedLines = if (lines.size >= 2) lines else allLines

        val (x, z) = fit
        val residuals = usedLines.map { it.residual(x, z) }
        val n = usedLines.size

        val accuracyRadius: Double
        val confidenceRadius: Double
        if (n <= 2) {
            accuracyRadius = rms(residuals)
            confidenceRadius = TWO_THROW_CONFIDENCE_BLOCKS
        } else {
            accuracyRadius = rms(residuals)
            val sumSq = residuals.sumOf { it * it }
            val sigmaSq = sumSq / (n - 2)
            val (sxx, sxz, szz) = normalEquationSums(usedLines)
            val det = sxx * szz - sxz * sxz
            confidenceRadius = if (abs(det) < MIN_DET) {
                TWO_THROW_CONFIDENCE_BLOCKS
            } else {
                val covXx = sigmaSq * szz / det
                val covZz = sigmaSq * sxx / det
                CONFIDENCE_Z * sqrt(abs(covXx) + abs(covZz))
            }
        }

        return StrongholdEstimate(
            x = x,
            z = z,
            accuracyRadius = accuracyRadius,
            confidenceRadius = confidenceRadius,
            throwsUsed = n,
            throwsTotal = usable.size,
            outlierMeasurementIds = outlierIds,
            throwSpreadBlocks = maxPairwiseSpread(usedLines)
        )
    }

    private fun maxPairwiseSpread(lines: List<Line>): Double {
        var max = 0.0
        for (i in lines.indices) {
            for (j in i + 1 until lines.size) {
                val dx = lines[i].px - lines[j].px
                val dz = lines[i].pz - lines[j].pz
                val distance = sqrt(dx * dx + dz * dz)
                if (distance > max) max = distance
            }
        }
        return max
    }

    private fun toLine(measurement: ThrowMeasurement): Line {
        val (dx, dz) = directionFromYaw(measurement.yawDegrees)
        return Line(measurement, measurement.playerX, measurement.playerZ, -dz, dx)
    }

    private fun normalEquationSums(lines: List<Line>): Triple<Double, Double, Double> {
        var sxx = 0.0
        var sxz = 0.0
        var szz = 0.0
        for (line in lines) {
            sxx += line.nx * line.nx
            sxz += line.nx * line.nz
            szz += line.nz * line.nz
        }
        return Triple(sxx, sxz, szz)
    }

    private fun solve(lines: List<Line>): Pair<Double, Double>? {
        if (lines.size < 2) return null
        var sxx = 0.0
        var sxz = 0.0
        var szz = 0.0
        var sxb = 0.0
        var szb = 0.0
        for (line in lines) {
            sxx += line.nx * line.nx
            sxz += line.nx * line.nz
            szz += line.nz * line.nz
            sxb += line.nx * line.b
            szb += line.nz * line.b
        }
        val det = sxx * szz - sxz * sxz
        if (abs(det) < MIN_DET) return null

        val x = (sxb * szz - szb * sxz) / det
        val z = (sxx * szb - sxz * sxb) / det
        return x to z
    }

    private fun rejectOutliers(lines: List<Line>, fit: Pair<Double, Double>): Pair<List<Line>, Set<Long>> {
        val (x, z) = fit
        val residuals = lines.map { abs(it.residual(x, z)) }
        val sortedResiduals = residuals.sorted()
        val median = median(sortedResiduals)
        val deviations = residuals.map { abs(it - median) }.sorted()
        val mad = median(deviations)
        val threshold = maxOf(OUTLIER_SIGMA_MULTIPLIER * OUTLIER_MAD_SCALE * mad, OUTLIER_FLOOR_BLOCKS)

        val inliers = mutableListOf<Line>()
        val outliers = mutableSetOf<Long>()
        lines.forEachIndexed { index, line ->
            if (residuals[index] <= threshold) {
                inliers.add(line)
            } else {
                outliers.add(line.measurement.id)
            }
        }
        return inliers to outliers
    }

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    private fun rms(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return sqrt(values.sumOf { it * it } / values.size)
    }
}
