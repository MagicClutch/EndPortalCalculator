package de.MagicClutch.endportalcalculator.client

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft
import net.minecraft.server.permissions.Permissions
import java.util.Locale

object StrongholdTpCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("strongholdtp").executes {
                    run()
                    1
                }
            )
        }
    }

    private fun run() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val estimate = MeasurementStore.estimate
        if (estimate == null) {
            NotificationBar.actionBar("§cNo stronghold estimate yet - add more throws")
            return
        }

        val isOp = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
        val isCreative = player.abilities.instabuild
        if (!isOp && !isCreative) {
            NotificationBar.actionBar("§cRequires OP or Creative mode - not available in survival")
            NotificationBar.chat("§b[End Portal Calculator]§r §c/strongholdtp requires OP permissions or Creative mode.")
            return
        }

        val x = String.format(Locale.ROOT, "%.1f", estimate.x)
        val z = String.format(Locale.ROOT, "%.1f", estimate.z)
        player.connection.sendCommand("tp $x ${player.y} $z")
        NotificationBar.actionBar("§bTeleporting to estimated stronghold §7(±${estimate.accuracyRadius.toInt()} blocks)")
    }
}
