package systemSkyblockStyleAddon

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import java.lang.reflect.Method

class ReflectionMayorApiFacade private constructor(
    private val apiClass: Class<*>,
    private val apiInstance: Any,
    private val methodCurrentTerm: Method,
    private val methodActivePerks: Method
) : MayorApiFacade {

    override fun currentTermOrNull(): Int? = methodCurrentTerm.invoke(apiInstance) as? Int

    override fun activePerkIdsOrEmpty(): Set<String> {
        val raw = methodActivePerks.invoke(apiInstance) as? Set<*> ?: return emptySet()
        return raw.mapNotNull { it as? String }.toSet()
    }

    companion object {
        fun load(plugin: Plugin): ReflectionMayorApiFacade? {
            val mayorPlugin = Bukkit.getPluginManager().getPlugin("MayorSystem") ?: return null
            val mayorLoader = mayorPlugin.javaClass.classLoader
            val apiClass = runCatching {
                Class.forName("mayorSystem.api.MayorSystemApi", true, mayorLoader)
            }.getOrNull() ?: return null

            val serviceManager = plugin.server.servicesManager
            val registration = runCatching {
                @Suppress("UNCHECKED_CAST")
                serviceManager.getRegistration(apiClass as Class<Any>)
            }.getOrNull() ?: return null

            val apiInstance = registration.provider

            val methodCurrentTerm = apiClass.getMethod("currentTermOrNull")
            val methodActivePerks = apiClass.getMethod("activePerkIdsOrEmpty")

            return ReflectionMayorApiFacade(
                apiClass = apiClass,
                apiInstance = apiInstance,
                methodCurrentTerm = methodCurrentTerm,
                methodActivePerks = methodActivePerks
            )
        }
    }
}

class ReflectionMayorEventsBridge(
    private val plugin: Plugin,
    private val state: AddonState,
    private val api: MayorApiFacade
) {
    private var appliedMethod: Method? = null
    private var clearedMethod: Method? = null

    fun register(): Boolean {
        val mayorPlugin = Bukkit.getPluginManager().getPlugin("MayorSystem") ?: return false
        val mayorLoader = mayorPlugin.javaClass.classLoader
        val appliedClass = runCatching {
            Class.forName("mayorSystem.api.events.MayorPerksAppliedEvent", true, mayorLoader)
        }.getOrNull() ?: return false
        val clearedClass = runCatching {
            Class.forName("mayorSystem.api.events.MayorPerksClearedEvent", true, mayorLoader)
        }.getOrNull() ?: return false

        appliedMethod = appliedClass.getMethod("getActivePerkIds")
        clearedMethod = clearedClass.getMethod("getClearedPerkIds")

        val pm: PluginManager = plugin.server.pluginManager
        pm.registerEvent(
            appliedClass.asSubclass(Event::class.java),
            DummyListener,
            EventPriority.NORMAL,
            EventExecutor { _, event ->
                val task = Runnable {
                    val perks = appliedMethod?.invoke(event) as? Set<*>
                    val perkIds = perks?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                    state.setActivePerks(perkIds)
                    if (plugin.config.getBoolean("debug_logging", false)) {
                        plugin.logger.info("[SSSA] Perks applied event: ${perkIds.joinToString(", ")}")
                    }
                }
                if (Bukkit.isPrimaryThread()) task.run() else Bukkit.getScheduler().runTask(plugin, task)
            },
            plugin,
            true
        )

        pm.registerEvent(
            clearedClass.asSubclass(Event::class.java),
            DummyListener,
            EventPriority.NORMAL,
            EventExecutor { _, event ->
                val task = Runnable {
                    state.setActivePerks(emptySet())
                    if (plugin.config.getBoolean("debug_logging", false)) {
                        val cleared = clearedMethod?.invoke(event) as? Set<*>
                        val ids = cleared?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                        plugin.logger.info("[SSSA] Perks cleared event: ${ids.joinToString(", ")}")
                    }
                }
                if (Bukkit.isPrimaryThread()) task.run() else Bukkit.getScheduler().runTask(plugin, task)
            },
            plugin,
            true
        )

        return true
    }

    private object DummyListener : org.bukkit.event.Listener
}
