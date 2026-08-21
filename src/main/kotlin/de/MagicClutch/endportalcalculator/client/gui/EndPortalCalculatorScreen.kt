package de.MagicClutch.endportalcalculator.client.gui

import de.MagicClutch.endportalcalculator.client.MeasurementStore
import de.MagicClutch.endportalcalculator.client.config.CaptureMode
import de.MagicClutch.endportalcalculator.client.config.EndPortalCalculatorConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class EndPortalCalculatorScreen : Screen(Component.literal("End Portal Calculator")) {

    private companion object {
        const val ROW_HEIGHT = 14
        const val FIELD_WIDTH = 150
        const val ROW_DELETE_OFFSET = 218
    }

    private lateinit var minDistanceField: EditBox
    private var minDistanceLabelY = 0

    private lateinit var autoModeButton: Button
    private lateinit var f3cModeButton: Button
    private lateinit var waypointToggleButton: Button

    private var scrollOffset = 0
    private var lastSeenIds: List<Long> = emptyList()

    private val rowButtons = mutableListOf<Button>()
    private var listLeft = 0
    private var listTop = 0
    private var listVisibleRows = 0

    override fun init() {
        val leftX = 12
        var y = 34

        addRenderableWidget(Button.builder(Component.literal("Clear All")) { onClearAll() }.pos(leftX, y).size(FIELD_WIDTH, 20).build())
        y += 30

        autoModeButton = addRenderableWidget(
            Button.builder(Component.literal("Auto Mode")) { setMode(CaptureMode.AUTO) }.pos(leftX, y).size(FIELD_WIDTH, 20).build()
        )
        y += 22
        f3cModeButton = addRenderableWidget(
            Button.builder(Component.literal("F3+C Mode")) { setMode(CaptureMode.F3C) }.pos(leftX, y).size(FIELD_WIDTH, 20).build()
        )
        y += 30

        waypointToggleButton = addRenderableWidget(
            Button.builder(Component.literal("")) { toggleWaypoint() }.pos(leftX, y).size(FIELD_WIDTH, 20).build()
        )
        y += 30

        minDistanceLabelY = y - 10
        minDistanceField = EditBox(font, leftX, y, FIELD_WIDTH, 18, Component.literal("Min throw distance"))
        minDistanceField.setHint(Component.literal("Min. blocks between throws"))
        minDistanceField.setValue(EndPortalCalculatorConfig.data.minThrowSeparationBlocks.toInt().toString())
        minDistanceField.setResponder { text ->
            text.trim().toDoubleOrNull()?.let { EndPortalCalculatorConfig.setMinThrowSeparationBlocks(it) }
        }
        addRenderableWidget(minDistanceField)

        listLeft = leftX + FIELD_WIDTH + 24
        listTop = 70
        listVisibleRows = ((height - listTop - 30) / ROW_HEIGHT).coerceAtLeast(1)

        refreshModeButtons()
        rebuildMeasurementRows()
    }

    override fun tick() {
        super.tick()
        val currentIds = MeasurementStore.all().map { it.id }
        if (currentIds != lastSeenIds) {
            rebuildMeasurementRows()
        }
    }

    private fun refreshModeButtons() {
        val mode = EndPortalCalculatorConfig.data.captureMode
        autoModeButton.message = Component.literal(if (mode == CaptureMode.AUTO) "> Auto Mode <" else "Auto Mode")
        f3cModeButton.message = Component.literal(if (mode == CaptureMode.F3C) "> F3+C Mode <" else "F3+C Mode")
        val waypointOn = EndPortalCalculatorConfig.data.createWaypointEnabled
        waypointToggleButton.message = Component.literal(if (waypointOn) "Create Waypoint: ON" else "Create Waypoint: OFF")
    }

    private fun setMode(mode: CaptureMode) {
        EndPortalCalculatorConfig.setCaptureMode(mode)
        refreshModeButtons()
    }

    private fun toggleWaypoint() {
        EndPortalCalculatorConfig.setCreateWaypointEnabled(!EndPortalCalculatorConfig.data.createWaypointEnabled)
        refreshModeButtons()
    }

    private fun onClearAll() {
        MeasurementStore.clearAll()
    }

    private fun rebuildMeasurementRows() {
        rowButtons.forEach { removeWidget(it) }
        rowButtons.clear()

        val measurements = MeasurementStore.all().sortedBy { it.timestamp }
        lastSeenIds = measurements.map { it.id }

        val maxOffset = (measurements.size - listVisibleRows).coerceAtLeast(0)
        scrollOffset = scrollOffset.coerceIn(0, maxOffset)

        val visible = measurements.drop(scrollOffset).take(listVisibleRows)
        visible.forEachIndexed { index, measurement ->
            val rowY = listTop + index * ROW_HEIGHT
            val delete = Button.builder(Component.literal("x")) {
                MeasurementStore.removeMeasurement(measurement.id)
            }.pos(listLeft + ROW_DELETE_OFFSET, rowY - 2).size(14, 12).build()
            rowButtons.add(addRenderableWidget(delete))
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, isDoubleClick)) return true

        if (event.button() == 0 && event.x() >= listLeft && event.x() < listLeft + ROW_DELETE_OFFSET && event.y() >= listTop) {
            val measurements = MeasurementStore.all().sortedBy { it.timestamp }
            val visible = measurements.drop(scrollOffset).take(listVisibleRows)
            val rowIndex = ((event.y() - listTop) / ROW_HEIGHT).toInt()
            if (rowIndex in visible.indices) {
                MeasurementStore.toggleSelected(visible[rowIndex].id)
                return true
            }
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (mouseX >= listLeft) {
            scrollOffset -= verticalAmount.toInt()
            rebuildMeasurementRows()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.text(font, "End Portal Calculator", 12, 12, 0xFFFFFFFF.toInt(), true)
        graphics.text(font, "Min. blocks between throws:", 12, minDistanceLabelY, 0xFFAAAAAA.toInt(), false)
        renderEstimateBox(graphics)
        renderMeasurementList(graphics)
    }

    private fun renderEstimateBox(graphics: GuiGraphicsExtractor) {
        val estimate = MeasurementStore.estimate
        val player = Minecraft.getInstance().player
        val boxX = 12
        var boxY = 208

        if (estimate == null) {
            graphics.text(font, "Need at least 2 throws to estimate", boxX, boxY, 0xFFAAAAAA.toInt(), false)
            return
        }

        val distance = player?.let { estimate.distanceFrom(it.x, it.z) }
        graphics.text(font, "X: ${estimate.x.toInt()}", boxX, boxY, 0xFFFFFFAA.toInt(), false); boxY += 11
        graphics.text(font, "Z: ${estimate.z.toInt()}", boxX, boxY, 0xFFFFFFAA.toInt(), false); boxY += 11
        if (distance != null) {
            graphics.text(font, "Distance: ${"%,d".format(distance.toInt())} blocks", boxX, boxY, 0xFFFFFFFF.toInt(), false)
        }
        boxY += 11
        graphics.text(font, "Accuracy: ±${estimate.accuracyRadius.toInt()} blocks", boxX, boxY, 0xFFAAFFAA.toInt(), false); boxY += 11
        graphics.text(font, "Confidence radius: ±${estimate.confidenceRadius.toInt()} blocks", boxX, boxY, 0xFFAAAAFF.toInt(), false); boxY += 11
        if (estimate.lowSeparationWarning) {
            graphics.text(font, "⚠ Throws ${estimate.throwSpreadBlocks.toInt()} blocks apart", boxX, boxY, 0xFFFFAA33.toInt(), false); boxY += 11
            graphics.text(font, "  move further away, add more", boxX, boxY, 0xFFFFAA33.toInt(), false)
        }
    }

    private fun renderMeasurementList(graphics: GuiGraphicsExtractor) {
        val estimate = MeasurementStore.estimate
        val measurements = MeasurementStore.all().sortedBy { it.timestamp }
        graphics.text(font, "Throws: ${measurements.size}", listLeft, listTop - 14, 0xFFFFFFFF.toInt(), true)

        if (measurements.isEmpty()) {
            graphics.text(font, "No throws recorded yet", listLeft, listTop, 0xFFAAAAAA.toInt(), false)
            return
        }

        val visible = measurements.drop(scrollOffset).take(listVisibleRows)
        visible.forEachIndexed { index, measurement ->
            val rowY = listTop + index * ROW_HEIGHT
            val isSelected = measurement.id == MeasurementStore.selectedId
            if (isSelected) {
                graphics.fill(listLeft - 2, rowY - 2, listLeft + ROW_DELETE_OFFSET - 4, rowY + 9, 0x50FFFFFF)
            }
            val isOutlier = estimate?.outlierMeasurementIds?.contains(measurement.id) == true
            val color = if (isSelected) 0xFFFFFFFF.toInt() else if (isOutlier) 0xFFFF8080.toInt() else 0xFFE0E0E0.toInt()
            val displayIndex = scrollOffset + index + 1
            val origin = "${measurement.playerX.toInt()},${measurement.playerZ.toInt()}"
            val text = if (measurement.targetX != null && measurement.targetZ != null) {
                "#$displayIndex  $origin → ${measurement.targetX.toInt()},${measurement.targetZ.toInt()}"
            } else {
                "#$displayIndex  $origin  ∠${"%.1f".format(measurement.yawDegrees)}°"
            }
            graphics.text(font, text, listLeft, rowY, color, false)
        }

        if (measurements.size > listVisibleRows) {
            graphics.text(font, "Scroll for more", listLeft, listTop + listVisibleRows * ROW_HEIGHT + 4, 0xFF808080.toInt(), false)
        }
    }

    override fun onClose() {
        EndPortalCalculatorConfig.save()
        super.onClose()
    }
}
