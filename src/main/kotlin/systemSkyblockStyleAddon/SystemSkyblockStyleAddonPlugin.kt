package systemSkyblockStyleAddon

import mayorSystem.api.MayorAddonRegistration
import mayorSystem.api.MayorSystemApi
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class SystemSkyblockStyleAddonPlugin : JavaPlugin() {

    lateinit var api: MayorApiFacade
        private set

    private lateinit var state: AddonState
    private lateinit var commandHandler: AddonCommand
    private var mayorSystemApi: MayorSystemApi? = null
    private var perkSourceRegistration: MayorAddonRegistration? = null
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

        // Delay API init once; avoids noisy failure on first tick when MayorSystem is still booting.
        retryTaskId = server.scheduler.runTaskLater(this, Runnable {
            if (!tryInitApi()) {
                logger.severe("[SSSA] MayorSystem API not found. Make sure MayorSystem is updated and enabled.")
                server.pluginManager.disablePlugin(this)
            }
        }, 20L).taskId

        // logSelfCheck is invoked on successful API init
    }

    override fun onDisable() {
        perkSourceRegistration?.close()
        perkSourceRegistration = null
        mayorSystemApi = null
        apiReady = false
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
        val apiService = server.servicesManager.load(MayorSystemApi::class.java) ?: return false
        mayorSystemApi = apiService
        api = MayorSystemApiFacade(apiService)
        apiReady = true
        server.pluginManager.registerEvents(MayorPerkEventsListener(this, state), this)
        registerPerkSource()
        state.syncWithApi(api)
        logSelfCheck()
        return true
    }

    fun refreshMayorPerkSourceAfterConfigReload() {
        val service = mayorSystemApi ?: return
        perkSourceRegistration?.close()
        perkSourceRegistration = null
        registerPerkSource(service)
    }

    private fun registerPerkSource() {
        val service = mayorSystemApi ?: return
        registerPerkSource(service)
    }

    private fun registerPerkSource(service: MayorSystemApi) {
        perkSourceRegistration = runCatching {
            service.registerPerkSource(this, SkyblockStyleMayorPerkSource(this))
        }.onSuccess {
            logger.info("[SSSA] Registered Skyblock Style perk source with MayorSystem.")
        }.onFailure { ex ->
            logger.log(Level.WARNING, "[SSSA] Failed to register Skyblock Style perk source with MayorSystem.", ex)
        }.getOrNull()
    }

    private fun logSelfCheck() {
        logger.info("[SSSA] MayorSystem API found: ${api.javaClass.name}")
        val term = api.currentTermIndex()
        val perks = if (state.activePerkIds.isEmpty()) "none" else state.activePerkIds.joinToString(", ")
        logger.info("[SSSA] Current term: ${term ?: "none"} | Active perks: $perks")
        logger.info("[SSSA] Parsed mechanics: ${state.mechanicsCount()}")
    }
}
