package systemSkyblockStyleAddon

import mayorSystem.api.MayorSystemApi

interface MayorApiFacade {
    fun currentTermIndex(): Int?
    fun activePerkIds(): Set<String>
}

class MayorSystemApiFacade(private val api: MayorSystemApi) : MayorApiFacade {
    override fun currentTermIndex(): Int? = api.currentTerm()?.index

    override fun activePerkIds(): Set<String> = api.activePerkIds()
}
