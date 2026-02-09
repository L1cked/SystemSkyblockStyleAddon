package systemSkyblockStyleAddon

interface MayorApiFacade {
    fun currentTermOrNull(): Int?
    fun activePerkIdsOrEmpty(): Set<String>
}
