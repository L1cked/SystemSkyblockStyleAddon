package systemSkyblockStyleAddon

import mayorSystem.api.MayorPerkDefinition
import mayorSystem.api.MayorPerkSection
import mayorSystem.api.MayorPerkSource
import mayorSystem.api.events.MayorPerksAppliedEvent
import mayorSystem.api.events.MayorPerksClearedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MayorPerkEventsListener(
    private val plugin: SystemSkyblockStyleAddonPlugin,
    private val state: AddonState
) : Listener {
    @EventHandler
    fun onPerksApplied(event: MayorPerksAppliedEvent) {
        state.setActivePerks(event.activePerkIds)
        if (plugin.config.getBoolean("debug_logging", false)) {
            plugin.logger.info("[SSSA] Perks applied event: ${event.activePerkIds.joinToString(", ")}")
        }
    }

    @EventHandler
    fun onPerksCleared(event: MayorPerksClearedEvent) {
        state.setActivePerks(emptySet())
        if (plugin.config.getBoolean("debug_logging", false)) {
            plugin.logger.info("[SSSA] Perks cleared event: ${event.clearedPerkIds.joinToString(", ")}")
        }
    }
}

class SkyblockStyleMayorPerkSource(
    private val plugin: SystemSkyblockStyleAddonPlugin
) : MayorPerkSource {
    override val id: String = "systemskyblockstyleaddon"
    override val displayName: String = "SystemSkyblockStyleAddon"
    override val available: Boolean = true

    override fun sections(): List<MayorPerkSection> {
        val root = plugin.config.getConfigurationSection("perks") ?: return emptyList()
        val perks = root.getKeys(false).mapNotNull { perkId ->
            if (!MAYOR_ID_PATTERN.matches(perkId)) {
                plugin.logger.warning("Skipping invalid MayorSystem skyblock perk id '$perkId'. Ids must match $MAYOR_ID_REGEX.")
                return@mapNotNull null
            }
            val path = "perks.$perkId.meta"
            MayorPerkDefinition(
                id = perkId,
                enabled = plugin.config.getBoolean("$path.enabled", true),
                displayName = plugin.config.getString("$path.display_name") ?: "<white>$perkId</white>",
                icon = plugin.config.getString("$path.icon") ?: "DIAMOND_PICKAXE",
                lore = plugin.config.getStringList("$path.lore"),
                adminLore = plugin.config.getStringList("$path.admin_lore"),
                onStart = emptyList(),
                onEnd = emptyList()
            )
        }
        if (perks.isEmpty()) return emptyList()
        return listOf(
            MayorPerkSection(
                id = "skyblock_style",
                enabled = true,
                pickLimit = 2,
                displayName = "<gradient:#2c3e50:#4ca1af>Skyblock Style</gradient>",
                icon = "DIAMOND_PICKAXE",
                perks = perks
            )
        )
    }

    private companion object {
        private const val MAYOR_ID_REGEX = "^[a-z0-9][a-z0-9_.-]{0,63}$"
        private val MAYOR_ID_PATTERN = Regex(MAYOR_ID_REGEX)
    }
}
