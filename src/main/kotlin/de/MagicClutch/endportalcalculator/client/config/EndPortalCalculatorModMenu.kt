package de.MagicClutch.endportalcalculator.client.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class EndPortalCalculatorModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> =
        ConfigScreenFactory { parent -> buildConfigScreen(parent) }
}

private fun buildConfigScreen(parent: Screen): Screen {
    val data = EndPortalCalculatorConfig.data

    val modEnabled = Option.createBuilder<Boolean>()
        .name(Component.literal("Mod Enabled"))
        .description(OptionDescription.of(Component.literal("Master switch - turns the whole mod on or off.")))
        .binding(true, { data.modEnabled }, { EndPortalCalculatorConfig.setModEnabled(it) })
        .controller(TickBoxControllerBuilder::create)
        .build()

    val hudEnabled = Option.createBuilder<Boolean>()
        .name(Component.literal("HUD Enabled"))
        .description(OptionDescription.of(Component.literal("Shows the stronghold estimate overlay on screen.")))
        .binding(true, { data.hudEnabled }, { EndPortalCalculatorConfig.setHudEnabled(it) })
        .controller(TickBoxControllerBuilder::create)
        .build()

    val worldRenderEnabled = Option.createBuilder<Boolean>()
        .name(Component.literal("World Render Enabled"))
        .description(OptionDescription.of(Component.literal("Draws throw trajectories and the stronghold marker in the world.")))
        .binding(true, { data.worldRenderEnabled }, { EndPortalCalculatorConfig.setWorldRenderEnabled(it) })
        .controller(TickBoxControllerBuilder::create)
        .build()

    val createWaypointEnabled = Option.createBuilder<Boolean>()
        .name(Component.literal("Create Waypoint"))
        .description(OptionDescription.of(Component.literal("Tracks the estimated stronghold position as an in-game waypoint.")))
        .binding(true, { data.createWaypointEnabled }, { EndPortalCalculatorConfig.setCreateWaypointEnabled(it) })
        .controller(TickBoxControllerBuilder::create)
        .build()

    val captureMode = Option.createBuilder<CaptureMode>()
        .name(Component.literal("Capture Mode"))
        .description(OptionDescription.of(Component.literal("Auto detects Eye of Ender throws; F3+C requires pressing F3+C while looking at the eye.")))
        .binding(CaptureMode.AUTO, { data.captureMode }, { EndPortalCalculatorConfig.setCaptureMode(it) })
        .controller { opt -> EnumControllerBuilder.create(opt).enumClass(CaptureMode::class.java) }
        .build()

    val minThrowSeparationBlocks = Option.createBuilder<Double>()
        .name(Component.literal("Min. Throw Separation (blocks)"))
        .description(OptionDescription.of(Component.literal("Minimum distance between throw locations before warning about low accuracy.")))
        .binding(20.0, { data.minThrowSeparationBlocks }, { EndPortalCalculatorConfig.setMinThrowSeparationBlocks(it) })
        .controller { opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 200.0).step(1.0) }
        .build()

    return YetAnotherConfigLib.createBuilder()
        .title(Component.literal("End Portal Calculator"))
        .category(
            ConfigCategory.createBuilder()
                .name(Component.literal("General"))
                .group(
                    OptionGroup.createBuilder()
                        .name(Component.literal("General"))
                        .option(modEnabled)
                        .build()
                )
                .group(
                    OptionGroup.createBuilder()
                        .name(Component.literal("Display"))
                        .option(hudEnabled)
                        .option(worldRenderEnabled)
                        .option(createWaypointEnabled)
                        .build()
                )
                .group(
                    OptionGroup.createBuilder()
                        .name(Component.literal("Capture"))
                        .option(captureMode)
                        .option(minThrowSeparationBlocks)
                        .build()
                )
                .build()
        )
        .save { EndPortalCalculatorConfig.save() }
        .build()
        .generateScreen(parent)
}
