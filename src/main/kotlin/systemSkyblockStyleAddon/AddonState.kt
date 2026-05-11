package systemSkyblockStyleAddon

import java.util.UUID

class AddonState(private val plugin: SystemSkyblockStyleAddonPlugin) {
    private val parser = MechanicParser(plugin)

    var activePerkIds: Set<String> = emptySet()
        private set

    var mechanics: MechanicsState = MechanicsState.empty()
        private set

    val fallCooldowns: MutableMap<UUID, Long> = mutableMapOf()

    fun syncWithApi(api: MayorApiFacade) {
        setActivePerks(api.activePerkIds())
    }

    fun setActivePerks(perkIds: Set<String>) {
        activePerkIds = perkIds.toSet()
        mechanics = parser.build(activePerkIds)
        if (mechanics.fallProtect.isEmpty()) {
            fallCooldowns.clear()
        }
    }

    fun rebuildMechanics() {
        mechanics = parser.build(activePerkIds)
        if (mechanics.fallProtect.isEmpty()) {
            fallCooldowns.clear()
        }
    }

    fun clear() {
        activePerkIds = emptySet()
        mechanics = MechanicsState.empty()
        fallCooldowns.clear()
    }

    fun mechanicsCount(): Int = mechanics.summaries.size
}
