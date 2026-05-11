package systemSkyblockStyleAddon

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AddonCommand(
    private val plugin: SystemSkyblockStyleAddonPlugin,
    private val state: AddonState
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("sssa.admin")) {
            sender.sendMessage("You do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!plugin.isApiReady()) {
                    sender.sendMessage("MayorSystem API not ready. Is MayorSystem updated and enabled?")
                    return true
                }
                plugin.reloadConfig()
                plugin.refreshMayorPerkSourceAfterConfigReload()
                state.syncWithApi(plugin.api)
                sender.sendMessage("SystemSkyblockStyleAddon reloaded. Active perks: ${state.activePerkIds.size}")
                return true
            }

            "debug" -> {
                if (!plugin.isApiReady()) {
                    sender.sendMessage("MayorSystem API not ready. Is MayorSystem updated and enabled?")
                    return true
                }
                val term = plugin.api.currentTermIndex()
                val perks = state.activePerkIds
                val summaries = state.mechanics.summaries
                sender.sendMessage("Current term: ${term ?: "none"}")
                sender.sendMessage("Active perks: ${if (perks.isEmpty()) "none" else perks.joinToString(", ")}")
                sender.sendMessage("Mechanics armed: ${if (summaries.isEmpty()) "none" else summaries.joinToString(", ") { "${it.perkId}:${it.type}" }}")

                if (args.size >= 2) {
                    val target = plugin.server.getPlayerExact(args[1]) ?: plugin.server.getPlayer(args[1])
                    if (target == null) {
                        sender.sendMessage("Player not found: ${args[1]}")
                        return true
                    }
                    sendPlayerCooldown(sender, target)
                }
                return true
            }

            else -> {
                sendUsage(sender)
                return true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("sssa.admin")) return emptyList()
        return when (args.size) {
            1 -> listOf("reload", "debug").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("debug", ignoreCase = true)) {
                plugin.server.onlinePlayers.map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("Usage: /sssa reload")
        sender.sendMessage("Usage: /sssa debug [player]")
    }

    private fun sendPlayerCooldown(sender: CommandSender, player: Player) {
        val now = System.currentTimeMillis()
        val until = state.fallCooldowns[player.uniqueId]
        if (until == null || now >= until) {
            sender.sendMessage("Fall Protect cooldown for ${player.name}: ready")
            return
        }
        val remainingMs = (until - now).coerceAtLeast(0)
        val remainingSec = remainingMs / 1000.0
        sender.sendMessage("Fall Protect cooldown for ${player.name}: ${"%.1f".format(remainingSec)}s")
    }
}
