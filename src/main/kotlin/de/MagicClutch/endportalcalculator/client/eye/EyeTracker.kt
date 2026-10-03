package de.MagicClutch.endportalcalculator.client.eye

import de.MagicClutch.endportalcalculator.calc.MeasurementSource
import de.MagicClutch.endportalcalculator.calc.StrongholdCalculator
import de.MagicClutch.endportalcalculator.calc.ThrowMeasurement
import de.MagicClutch.endportalcalculator.client.AddMeasurementResult
import de.MagicClutch.endportalcalculator.client.MeasurementStore
import de.MagicClutch.endportalcalculator.client.NotificationBar
import de.MagicClutch.endportalcalculator.client.config.CaptureMode
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.projectile.EyeOfEnder

object EyeTracker {

    private const val OWNER_SPAWN_PROXIMITY = 2.0
    private const val LOOK_ANGLE_THRESHOLD_DEGREES = 6.0
    private const val LOOK_MAX_DISTANCE = 128.0

    private const val START_MOVING_THRESHOLD = 0.3
    private const val STILL_THRESHOLD_PER_TICK = 0.01
    private const val REQUIRED_STILL_TICKS = 3
    private const val MAX_WAIT_TICKS = 100

    private class Pending(
        val eyeId: Int,
        val eye: EyeOfEnder,
        val originX: Double,
        val originY: Double,
        val originZ: Double,
        val pitch: Double
    ) {
        var lastX = originX
        var lastZ = originZ
        var hasLeftThrowPoint = false
        var stillTicks = 0
        var ageTicks = 0
    }

    private val pending = linkedMapOf<Int, Pending>()

    data class LiveThrow(val originX: Double, val originY: Double, val originZ: Double, val eye: EyeOfEnder)

    fun liveThrows(): List<LiveThrow> =
        pending.values
            .filter { it.hasLeftThrowPoint && !it.eye.isRemoved }
            .map { LiveThrow(it.originX, it.originY, it.originZ, it.eye) }

    fun register() {
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity is EyeOfEnder) {
                onEyeSpawned(entity)
            }
        }
        ClientTickEvents.END_CLIENT_TICK.register { tickPending() }
    }

    private fun onEyeSpawned(eye: EyeOfEnder) {
        if (!EndPortalCalculatorConfig.data.modEnabled) return
        if (EndPortalCalculatorConfig.data.captureMode != CaptureMode.AUTO) return
        if (pending.containsKey(eye.id)) return
        val player = Minecraft.getInstance().player ?: return
        val eyePos = eye.position()
        if (eyePos.distanceTo(player.position()) > OWNER_SPAWN_PROXIMITY) return

        pending[eye.id] = Pending(eye.id, eye, eyePos.x, eyePos.y, eyePos.z, player.xRot.toDouble())
    }

    private fun tickPending() {
        if (pending.isEmpty()) return
        val iterator = pending.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.ageTicks++

            if (entry.eye.isRemoved) {
                iterator.remove()
                if (entry.hasLeftThrowPoint) capture(entry, entry.lastX, entry.lastZ, "Eye detected")
                continue
            }

            val pos = entry.eye.position()

            if (!entry.hasLeftThrowPoint) {
                val dxFromOrigin = pos.x - entry.originX
                val dzFromOrigin = pos.z - entry.originZ
                if (dxFromOrigin * dxFromOrigin + dzFromOrigin * dzFromOrigin > START_MOVING_THRESHOLD * START_MOVING_THRESHOLD) {
                    entry.hasLeftThrowPoint = true
                }
                entry.lastX = pos.x
                entry.lastZ = pos.z
                continue
            }

            val stepX = pos.x - entry.lastX
            val stepZ = pos.z - entry.lastZ
            entry.lastX = pos.x
            entry.lastZ = pos.z
            entry.stillTicks = if (stepX * stepX + stepZ * stepZ < STILL_THRESHOLD_PER_TICK * STILL_THRESHOLD_PER_TICK) {
                entry.stillTicks + 1
            } else {
                0
            }

            if (entry.stillTicks >= REQUIRED_STILL_TICKS) {
                iterator.remove()
                capture(entry, pos.x, pos.z, "Eye detected")
            } else if (entry.ageTicks >= MAX_WAIT_TICKS) {
                iterator.remove()
                capture(entry, pos.x, pos.z, "Eye detected")
            }
        }
    }

    private fun capture(entry: Pending, restX: Double, restZ: Double, verb: String) {
        val measurement = buildMeasurement(entry.originX, entry.originY, entry.originZ, restX, restZ, entry.pitch, MeasurementSource.AUTO)
        if (measurement != null) {
            addAndNotify(measurement, verb)
        }
    }

    fun tryCaptureF3C(): Boolean {
        if (!EndPortalCalculatorConfig.data.modEnabled) return false
        if (EndPortalCalculatorConfig.data.captureMode != CaptureMode.F3C) return false
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        val level = client.level ?: return false

        val eye = findLookedAtEye(player, level)
        val origin = player.eyePosition
        val measurement = eye?.let {
            buildMeasurement(origin.x, origin.y, origin.z, it.position().x, it.position().z, player.xRot.toDouble(), MeasurementSource.F3C)
        }
        if (measurement == null) {
            NotificationBar.actionBar("§cNo valid Eye of Ender found")
            return false
        }

        addAndNotify(measurement, "Eye captured")
        return true
    }

    private fun findLookedAtEye(player: LocalPlayer, level: ClientLevel): EyeOfEnder? {
        val candidates = level.getEntitiesOfClass(EyeOfEnder::class.java, player.boundingBox.inflate(LOOK_MAX_DISTANCE))
        if (candidates.isEmpty()) return null

        val eyePos = player.getEyePosition(1.0f)
        val look = player.getViewVector(1.0f)

        var best: EyeOfEnder? = null
        var bestAngle = LOOK_ANGLE_THRESHOLD_DEGREES
        for (candidate in candidates) {
            val toEntity = candidate.position().subtract(eyePos)
            val distance = toEntity.length()
            if (distance < 0.001) continue
            val cosAngle = (toEntity.normalize().dot(look)).coerceIn(-1.0, 1.0)
            val angle = Math.toDegrees(Math.acos(cosAngle))
            if (angle < bestAngle) {
                bestAngle = angle
                best = candidate
            }
        }
        return best
    }

    private fun buildMeasurement(
        originX: Double, originY: Double, originZ: Double,
        targetX: Double, targetZ: Double,
        pitch: Double,
        source: MeasurementSource
    ): ThrowMeasurement? {
        val dx = targetX - originX
        val dz = targetZ - originZ
        if (dx * dx + dz * dz < 1e-6) return null

        return ThrowMeasurement(
            id = ThrowMeasurement.nextId(),
            playerX = originX,
            playerY = originY,
            playerZ = originZ,
            yawDegrees = StrongholdCalculator.yawFromDirection(dx, dz),
            pitchDegrees = pitch,
            source = source,
            timestamp = System.currentTimeMillis(),
            targetX = targetX,
            targetZ = targetZ
        )
    }

    private fun addAndNotify(measurement: ThrowMeasurement, verb: String) {
        val result = MeasurementStore.addMeasurement(measurement)
        val estimate = MeasurementStore.estimate
        val strongholdText = if (estimate != null) {
            "Stronghold: X ${estimate.x.toInt()} Z ${estimate.z.toInt()}"
        } else {
            "need one more throw"
        }
        NotificationBar.actionBar("§b$verb §7- §aMeasurement #${result.count} added §7- §f$strongholdText")
        if (result.tooClose) {
            NotificationBar.warning("§6⚠ Too close (${result.nearestDistanceBlocks.toInt()}/${result.requiredBlocks.toInt()} blocks) ⚠")
        } else if (estimate != null && estimate.lowSeparationWarning) {
            NotificationBar.warning("§6⚠ Throws too close (${estimate.throwSpreadBlocks.toInt()} blocks) ⚠")
        }

        val chatLine = StringBuilder("§b[End Portal Calculator]§r $verb - measurement #${result.count} added")
        chatLine.append(" §7(thrown from X: ${measurement.playerX.toInt()} Z: ${measurement.playerZ.toInt()})")
        if (result.tooClose) {
            chatLine.append("\n§6⚠ Too close (${result.nearestDistanceBlocks.toInt()}/${result.requiredBlocks.toInt()} blocks) ⚠")
        }
        if (estimate != null) {
            chatLine.append("\n§aX: ${estimate.x.toInt()}  Z: ${estimate.z.toInt()}")
            chatLine.append(" §7±${estimate.accuracyRadius.toInt()} blocks, ${estimate.throwsUsed}/${estimate.throwsTotal} throws")
            if (!result.tooClose && estimate.lowSeparationWarning) {
                chatLine.append("\n§6⚠ Throws too close (${estimate.throwSpreadBlocks.toInt()} blocks) ⚠")
            }
        } else {
            chatLine.append(" §7- need at least one more throw to estimate")
        }
        NotificationBar.chat(chatLine.toString())
    }
}
