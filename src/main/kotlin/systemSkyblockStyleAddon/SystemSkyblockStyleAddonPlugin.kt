package systemSkyblockStyleAddon

import org.bukkit.plugin.java.JavaPlugin

class SystemSkyblockStyleAddonPlugin : JavaPlugin() {

    lateinit var api: MayorApiFacade
        private set

    private lateinit var state: AddonState
    private lateinit var commandHandler: AddonCommand
    private var apiReady: Boolean = false
    private var retryTaskId: Int = -1

    override fun onEnable() {
        saveDefaultConfig()

        state = AddonState(this)
        server.pluginManager.registerEvents(AddonListener(this, state), this)

        commandHandler = AddonCommand(this, state)
        val cmd = getCommand("sssa")
        if (cmd != null) {
            cmd.setExecutor(commandHandler)
            cmd.tabCompleter = commandHandler
        } else {
            logger.severe("[SSSA] Command 'sssa' missing from plugin.yml.")
        }

        if (!tryInitApi()) {
            logger.severe("[SSSA] MayorSystem API not found. Will retry shortly...")
            retryTaskId = server.scheduler.runTaskLater(this, Runnable {
                if (!tryInitApi()) {
                    logger.severe("[SSSA] MayorSystem API still not found. Make sure MayorSystem is updated and enabled.")
                    server.pluginManager.disablePlugin(this)
                }
            }, 20L).taskId
        }

        // logSelfCheck is invoked on successful API init
    }

    override fun onDisable() {
        if (retryTaskId != -1) {
            runCatching { server.scheduler.cancelTask(retryTaskId) }
            retryTaskId = -1
        }
        if (this::state.isInitialized) {
            state.clear()
        }
    }

    fun isApiReady(): Boolean = apiReady

    private fun tryInitApi(): Boolean {
        val apiService = ReflectionMayorApiFacade.load(this) ?: return false
        val bridge = ReflectionMayorEventsBridge(this, state, apiService)
        if (!bridge.register()) {
            logger.severe("[SSSA] MayorSystem events not found. Make sure MayorSystem is updated.")
            return false
        }
        api = apiService
        apiReady = true
        state.syncWithApi(api)
        return true
    }

    private fun logSelfCheck() {
        logger.info("[SSSA] MayorSystem API found: ${api.javaClass.name}")
        val term = api.currentTermOrNull()
        val perks = if (state.activePerkIds.isEmpty()) "none" else state.activePerkIds.joinToString(", ")
        logger.info("[SSSA] Current term: ${term ?: "none"} | Active perks: $perks")
        logger.info("[SSSA] Parsed mechanics: ${state.mechanicsCount()}")
    }
}
