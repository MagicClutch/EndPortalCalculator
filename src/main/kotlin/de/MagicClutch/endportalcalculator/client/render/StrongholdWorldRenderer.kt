package de.MagicClutch.endportalcalculator.client.render

import de.MagicClutch.endportalcalculator.calc.StrongholdCalculator
import de.MagicClutch.endportalcalculator.calc.StrongholdEstimate
import de.MagicClutch.endportalcalculator.calc.ThrowMeasurement
import de.MagicClutch.endportalcalculator.client.MeasurementStore
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import de.MagicClutch.endportalcalculator.client.eye.EyeTracker
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives
import net.minecraft.world.phys.Vec3
import kotlin.math.hypot

object StrongholdWorldRenderer {

    private const val TRAJECTORY_LENGTH = 300.0
    private const val MIN_RAY_LENGTH = 10.0
    private const val MAX_RAY_LENGTH = 4000.0
    private const val MARKER_HALF_HEIGHT = 40.0
    private const val LINE_WIDTH = 2.0f
    private const val CROSS_SIZE = 0.4

    private val TRAJECTORY_COLOR = 0xFF40FF80.toInt()
    private val TRAJECTORY_BAD_COLOR = 0xFFFF4040.toInt()
    private val MARKER_COLOR = 0xFF40FF80.toInt()
    private val THROW_POINT_COLOR = 0xFFFF0000.toInt()
    private val SELECTED_COLOR = 0xFFFFFFFF.toInt()
    private const val SELECTED_LINE_WIDTH = 4.0f

    fun register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(::render)
    }

    private fun render(context: LevelRenderContext) {
        if (!EndPortalCalculatorConfig.data.modEnabled) return
        if (!EndPortalCalculatorConfig.data.worldRenderEnabled) return
        val client = Minecraft.getInstance()
        if (client.player == null || client.level == null) return

        val estimate = MeasurementStore.estimate
        val measurements = MeasurementStore.all()
        val liveThrows = EyeTracker.liveThrows()
        if (measurements.isEmpty() && estimate == null && liveThrows.isEmpty()) return

        val cameraState = context.levelState().cameraRenderState
        val cameraPos = cameraState.pos

        val gizmos = DrawableGizmoPrimitives()
        val selectedId = MeasurementStore.selectedId
        val minSeparation = EndPortalCalculatorConfig.data.minThrowSeparationBlocks

        for (measurement in measurements) {
            val isSelected = measurement.id == selectedId
            val isOutlier = estimate?.outlierMeasurementIds?.contains(measurement.id) == true
            val isTooClose = minSeparation > 0.0 && nearestOtherDistance(measurement, measurements) < minSeparation
            val color = if (isSelected) SELECTED_COLOR else if (isOutlier || isTooClose) TRAJECTORY_BAD_COLOR else TRAJECTORY_COLOR
            val width = if (isSelected) SELECTED_LINE_WIDTH else LINE_WIDTH
            val startY = measurement.playerY
            val origin = Vec3(measurement.playerX, startY, measurement.playerZ)
            val crossColor = if (isSelected) SELECTED_COLOR else THROW_POINT_COLOR
            val (dx, dz) = StrongholdCalculator.directionFromYaw(measurement.yawDegrees)

            val rayLength = rayLengthTo(measurement.playerX, measurement.playerZ, dx, dz, estimate)
            val rayEnd = Vec3(measurement.playerX + dx * rayLength, startY, measurement.playerZ + dz * rayLength)

            if (measurement.targetX != null && measurement.targetZ != null) {
                val target = Vec3(measurement.targetX, startY, measurement.targetZ)
                gizmos.addLine(origin, target, color, width)
                addCross(gizmos, target, CROSS_SIZE, crossColor)
                gizmos.addLine(target, rayEnd, color, width)
            } else {
                gizmos.addLine(origin, rayEnd, color, width)
            }
            addCross(gizmos, origin, CROSS_SIZE, crossColor)
        }

        if (liveThrows.isNotEmpty()) {
            val partialTick = Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(false)
            for (live in liveThrows) {
                val origin = Vec3(live.originX, live.originY, live.originZ)
                val current = live.eye.getPosition(partialTick)
                gizmos.addLine(origin, current, TRAJECTORY_COLOR, LINE_WIDTH)
                addCross(gizmos, current, CROSS_SIZE, THROW_POINT_COLOR)
            }
        }

        if (estimate != null && EndPortalCalculatorConfig.data.createWaypointEnabled) {
            gizmos.addLine(
                Vec3(estimate.x, cameraPos.y - MARKER_HALF_HEIGHT, estimate.z),
                Vec3(estimate.x, cameraPos.y + MARKER_HALF_HEIGHT, estimate.z),
                MARKER_COLOR,
                LINE_WIDTH
            )
        }

        gizmos.submit(context.submitNodeCollector(), cameraState, false)
    }

    private fun rayLengthTo(originX: Double, originZ: Double, dx: Double, dz: Double, estimate: StrongholdEstimate?): Double {
        if (estimate == null) return TRAJECTORY_LENGTH
        val projected = (estimate.x - originX) * dx + (estimate.z - originZ) * dz
        return projected.coerceIn(MIN_RAY_LENGTH, MAX_RAY_LENGTH)
    }

    private fun nearestOtherDistance(measurement: ThrowMeasurement, measurements: List<ThrowMeasurement>): Double {
        var nearest = Double.MAX_VALUE
        for (other in measurements) {
            if (other.id == measurement.id) continue
            val distance = hypot(other.playerX - measurement.playerX, other.playerZ - measurement.playerZ)
            if (distance < nearest) nearest = distance
        }
        return nearest
    }

    private fun addCross(gizmos: DrawableGizmoPrimitives, center: Vec3, size: Double, color: Int) {
        gizmos.addLine(center.add(-size, 0.0, 0.0), center.add(size, 0.0, 0.0), color, LINE_WIDTH)
        gizmos.addLine(center.add(0.0, -size, 0.0), center.add(0.0, size, 0.0), color, LINE_WIDTH)
        gizmos.addLine(center.add(0.0, 0.0, -size), center.add(0.0, 0.0, size), color, LINE_WIDTH)
    }

}
