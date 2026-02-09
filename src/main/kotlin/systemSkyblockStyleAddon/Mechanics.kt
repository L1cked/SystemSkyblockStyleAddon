package systemSkyblockStyleAddon

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import java.util.EnumSet

enum class MechanicType {
    BLOCK_DROP_BONUS,
    AUTO_SMELT,
    CROP_BONUS,
    AUTO_REPLANT,
    MOB_DROP_BONUS,
    EXP_MULTIPLIER,
    FISHING_BONUS,
    ANVIL_DISCOUNT,
    FALL_PROTECT
}

enum class ExpSource {
    BLOCKS,
    MOBS
}

data class MechanicSummary(
    val perkId: String,
    val type: MechanicType
)

data class ExtraDrop(
    val material: Material,
    val min: Int,
    val max: Int
)

data class BlockDropBonusMechanic(
    val perkId: String,
    val chance: Double,
    val blocks: Set<Material>,
    val extraDrops: List<ExtraDrop>,
    val worlds: Set<String>?,
    val fortuneScales: Boolean
)

data class AutoSmeltMechanic(
    val perkId: String,
    val chance: Double,
    val blocks: Set<Material>,
    val mapping: Map<Material, Material>,
    val worlds: Set<String>?
)

data class CropBonusMechanic(
    val perkId: String,
    val chance: Double,
    val crops: Set<Material>,
    val extraAmountMin: Int,
    val extraAmountMax: Int,
    val worlds: Set<String>?
)

data class AutoReplantMechanic(
    val perkId: String,
    val crops: Set<Material>,
    val requireSeed: Boolean,
    val worlds: Set<String>?
)

data class MobDropBonusMechanic(
    val perkId: String,
    val chance: Double,
    val entities: Set<EntityType>,
    val extraDrops: List<ExtraDrop>,
    val ignoreSpawnerMobs: Boolean,
    val worlds: Set<String>?
)

data class ExpMultiplierMechanic(
    val perkId: String,
    val multiplier: Double,
    val sources: EnumSet<ExpSource>,
    val worlds: Set<String>?
)

data class FishingBonusMechanic(
    val perkId: String,
    val chance: Double,
    val extraLoot: List<ExtraDrop>,
    val worlds: Set<String>?
)

data class AnvilDiscountMechanic(
    val perkId: String,
    val multiplier: Double,
    val minCost: Int
)

data class FallProtectMechanic(
    val perkId: String,
    val damageMultiplier: Double,
    val cooldownSeconds: Int,
    val minFallDamageToTrigger: Double,
    val worlds: Set<String>?
)

data class MechanicsState(
    val activePerkIds: Set<String>,
    val summaries: List<MechanicSummary>,
    val blockDropBonuses: List<BlockDropBonusMechanic>,
    val autoSmelt: List<AutoSmeltMechanic>,
    val cropBonus: List<CropBonusMechanic>,
    val autoReplant: List<AutoReplantMechanic>,
    val mobDropBonus: List<MobDropBonusMechanic>,
    val expMultiplier: List<ExpMultiplierMechanic>,
    val fishingBonus: List<FishingBonusMechanic>,
    val anvilDiscount: List<AnvilDiscountMechanic>,
    val fallProtect: List<FallProtectMechanic>
) {
    companion object {
        fun empty(activePerkIds: Set<String> = emptySet()): MechanicsState = MechanicsState(
            activePerkIds = activePerkIds,
            summaries = emptyList(),
            blockDropBonuses = emptyList(),
            autoSmelt = emptyList(),
            cropBonus = emptyList(),
            autoReplant = emptyList(),
            mobDropBonus = emptyList(),
            expMultiplier = emptyList(),
            fishingBonus = emptyList(),
            anvilDiscount = emptyList(),
            fallProtect = emptyList()
        )
    }
}

class MechanicParser(private val plugin: SystemSkyblockStyleAddonPlugin) {

    fun build(perkIds: Set<String>): MechanicsState {
        if (perkIds.isEmpty()) return MechanicsState.empty(perkIds)

        val summaries = mutableListOf<MechanicSummary>()
        val blockDropBonuses = mutableListOf<BlockDropBonusMechanic>()
        val autoSmelt = mutableListOf<AutoSmeltMechanic>()
        val cropBonus = mutableListOf<CropBonusMechanic>()
        val autoReplant = mutableListOf<AutoReplantMechanic>()
        val mobDropBonus = mutableListOf<MobDropBonusMechanic>()
        val expMultiplier = mutableListOf<ExpMultiplierMechanic>()
        val fishingBonus = mutableListOf<FishingBonusMechanic>()
        val anvilDiscount = mutableListOf<AnvilDiscountMechanic>()
        val fallProtect = mutableListOf<FallProtectMechanic>()

        for (perkId in perkIds) {
            val sssa = plugin.config.getConfigurationSection("perks.$perkId.sssa") ?: continue
            if (!sssa.getBoolean("enabled", true)) continue

            val typeRaw = sssa.getString("type")?.trim().orEmpty()
            if (typeRaw.isBlank()) continue
            val type = runCatching { MechanicType.valueOf(typeRaw.uppercase()) }.getOrNull()
            if (type == null) {
                plugin.logger.warning("[SSSA] Unknown mechanic type '$typeRaw' in perk '$perkId'.")
                continue
            }

            val worlds = parseWorlds(sssa)

            when (type) {
                MechanicType.BLOCK_DROP_BONUS -> {
                    val blocks = parseMaterials(sssa, "blocks", perkId)
                    val extraDrops = parseExtraDrops(sssa, "extra_drops", perkId)
                    if (blocks.isEmpty() || extraDrops.isEmpty()) continue

                    val chance = clampChance(sssa.getDouble("chance", 0.0))
                    val fortuneScales = sssa.getBoolean("fortune_scales", false)
                    blockDropBonuses += BlockDropBonusMechanic(
                        perkId = perkId,
                        chance = chance,
                        blocks = blocks,
                        extraDrops = extraDrops,
                        worlds = worlds,
                        fortuneScales = fortuneScales
                    )
                }

                MechanicType.AUTO_SMELT -> {
                    val blocks = parseMaterials(sssa, "blocks", perkId)
                    val mapping = parseMaterialMapping(sssa, "mapping", perkId)
                    if (blocks.isEmpty() || mapping.isEmpty()) continue

                    val chance = clampChance(sssa.getDouble("chance", 1.0))
                    autoSmelt += AutoSmeltMechanic(
                        perkId = perkId,
                        chance = chance,
                        blocks = blocks,
                        mapping = mapping,
                        worlds = worlds
                    )
                }

                MechanicType.CROP_BONUS -> {
                    val crops = parseMaterials(sssa, "crops", perkId)
                    if (crops.isEmpty()) continue

                    val chance = clampChance(sssa.getDouble("chance", 0.0))
                    val min = sssa.getInt("extra_amount_min", 1).coerceAtLeast(0)
                    val max = sssa.getInt("extra_amount_max", min).coerceAtLeast(min)
                    cropBonus += CropBonusMechanic(
                        perkId = perkId,
                        chance = chance,
                        crops = crops,
                        extraAmountMin = min,
                        extraAmountMax = max,
                        worlds = worlds
                    )
                }

                MechanicType.AUTO_REPLANT -> {
                    val crops = parseMaterials(sssa, "crops", perkId)
                    if (crops.isEmpty()) continue

                    val requireSeed = sssa.getBoolean("require_seed", true)
                    autoReplant += AutoReplantMechanic(
                        perkId = perkId,
                        crops = crops,
                        requireSeed = requireSeed,
                        worlds = worlds
                    )
                }

                MechanicType.MOB_DROP_BONUS -> {
                    val entities = parseEntities(sssa, "entities", perkId)
                    val extraDrops = parseExtraDrops(sssa, "extra_drops", perkId)
                    if (entities.isEmpty() || extraDrops.isEmpty()) continue

                    val chance = clampChance(sssa.getDouble("chance", 0.0))
                    val ignoreSpawner = sssa.getBoolean("ignore_spawner_mobs", true)
                    mobDropBonus += MobDropBonusMechanic(
                        perkId = perkId,
                        chance = chance,
                        entities = entities,
                        extraDrops = extraDrops,
                        ignoreSpawnerMobs = ignoreSpawner,
                        worlds = worlds
                    )
                }

                MechanicType.EXP_MULTIPLIER -> {
                    val multiplier = sssa.getDouble("multiplier", 1.0).coerceAtLeast(0.0)
                    val sources = parseExpSources(sssa, "sources")
                    expMultiplier += ExpMultiplierMechanic(
                        perkId = perkId,
                        multiplier = multiplier,
                        sources = sources,
                        worlds = worlds
                    )
                }

                MechanicType.FISHING_BONUS -> {
                    val extraLoot = parseExtraDrops(sssa, "extra_loot", perkId)
                    if (extraLoot.isEmpty()) continue

                    val chance = clampChance(sssa.getDouble("chance", 0.0))
                    fishingBonus += FishingBonusMechanic(
                        perkId = perkId,
                        chance = chance,
                        extraLoot = extraLoot,
                        worlds = worlds
                    )
                }

                MechanicType.ANVIL_DISCOUNT -> {
                    val multiplier = sssa.getDouble("multiplier", 1.0).coerceAtLeast(0.0)
                    val minCost = sssa.getInt("min_cost", 1).coerceAtLeast(0)
                    anvilDiscount += AnvilDiscountMechanic(
                        perkId = perkId,
                        multiplier = multiplier,
                        minCost = minCost
                    )
                }

                MechanicType.FALL_PROTECT -> {
                    val multiplier = sssa.getDouble("damage_multiplier", 1.0).coerceAtLeast(0.0)
                    val cooldown = sssa.getInt("cooldown_seconds", 0).coerceAtLeast(0)
                    val minDamage = sssa.getDouble("min_fall_damage_to_trigger", 0.0).coerceAtLeast(0.0)
                    fallProtect += FallProtectMechanic(
                        perkId = perkId,
                        damageMultiplier = multiplier,
                        cooldownSeconds = cooldown,
                        minFallDamageToTrigger = minDamage,
                        worlds = worlds
                    )
                }
            }

            summaries += MechanicSummary(perkId, type)
        }

        return MechanicsState(
            activePerkIds = perkIds,
            summaries = summaries,
            blockDropBonuses = blockDropBonuses,
            autoSmelt = autoSmelt,
            cropBonus = cropBonus,
            autoReplant = autoReplant,
            mobDropBonus = mobDropBonus,
            expMultiplier = expMultiplier,
            fishingBonus = fishingBonus,
            anvilDiscount = anvilDiscount,
            fallProtect = fallProtect
        )
    }

    private fun clampChance(raw: Double): Double = raw.coerceIn(0.0, 1.0)

    private fun parseWorlds(section: ConfigurationSection): Set<String>? {
        val list = section.getStringList("worlds")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        return if (list.isEmpty()) null else list.toSet()
    }

    private fun parseMaterials(section: ConfigurationSection, key: String, perkId: String): Set<Material> {
        val raw = section.getStringList(key)
        if (raw.isEmpty()) {
            plugin.logger.warning("[SSSA] Missing or empty '$key' for perk '$perkId'.")
            return emptySet()
        }
        val out = linkedSetOf<Material>()
        for (entry in raw) {
            val name = entry.trim().uppercase()
            if (name.isBlank()) continue
            val mat = runCatching { Material.valueOf(name) }.getOrNull()
            if (mat == null) {
                plugin.logger.warning("[SSSA] Unknown material '$name' in '$key' for perk '$perkId'.")
                continue
            }
            out += mat
        }
        return out
    }

    private fun parseEntities(section: ConfigurationSection, key: String, perkId: String): Set<EntityType> {
        val raw = section.getStringList(key)
        if (raw.isEmpty()) {
            plugin.logger.warning("[SSSA] Missing or empty '$key' for perk '$perkId'.")
            return emptySet()
        }
        val out = linkedSetOf<EntityType>()
        for (entry in raw) {
            val name = entry.trim().uppercase()
            if (name.isBlank()) continue
            val type = runCatching { EntityType.valueOf(name) }.getOrNull()
            if (type == null) {
                plugin.logger.warning("[SSSA] Unknown entity type '$name' in '$key' for perk '$perkId'.")
                continue
            }
            out += type
        }
        return out
    }

    private fun parseMaterialMapping(section: ConfigurationSection, key: String, perkId: String): Map<Material, Material> {
        val mapSec = section.getConfigurationSection(key)
        if (mapSec == null || mapSec.getKeys(false).isEmpty()) {
            plugin.logger.warning("[SSSA] Missing or empty '$key' mapping for perk '$perkId'.")
            return emptyMap()
        }
        val out = linkedMapOf<Material, Material>()
        for (entry in mapSec.getKeys(false)) {
            val fromName = entry.trim().uppercase()
            val toName = mapSec.getString(entry)?.trim()?.uppercase().orEmpty()
            if (fromName.isBlank() || toName.isBlank()) continue
            val fromMat = runCatching { Material.valueOf(fromName) }.getOrNull()
            val toMat = runCatching { Material.valueOf(toName) }.getOrNull()
            if (fromMat == null || toMat == null) {
                plugin.logger.warning("[SSSA] Invalid mapping '$fromName' -> '$toName' in perk '$perkId'.")
                continue
            }
            out[fromMat] = toMat
        }
        return out
    }

    private fun parseExtraDrops(section: ConfigurationSection, key: String, perkId: String): List<ExtraDrop> {
        val raw = section.getMapList(key)
        if (raw.isEmpty()) {
            plugin.logger.warning("[SSSA] Missing or empty '$key' list for perk '$perkId'.")
            return emptyList()
        }
        val out = mutableListOf<ExtraDrop>()
        for (entry in raw) {
            val matName = (entry["material"] as? String)?.trim()?.uppercase().orEmpty()
            if (matName.isBlank()) continue
            val mat = runCatching { Material.valueOf(matName) }.getOrNull()
            if (mat == null) {
                plugin.logger.warning("[SSSA] Unknown material '$matName' in '$key' for perk '$perkId'.")
                continue
            }
            val minRaw = (entry["min"] as? Number)?.toInt() ?: 1
            val maxRaw = (entry["max"] as? Number)?.toInt() ?: minRaw
            val min = minRaw.coerceAtLeast(0)
            val max = maxRaw.coerceAtLeast(min)
            out += ExtraDrop(mat, min, max)
        }
        return out
    }

    private fun parseExpSources(section: ConfigurationSection, key: String): EnumSet<ExpSource> {
        val raw = section.getStringList(key)
        if (raw.isEmpty()) return EnumSet.of(ExpSource.BLOCKS, ExpSource.MOBS)
        val out = EnumSet.noneOf(ExpSource::class.java)
        for (entry in raw) {
            val name = entry.trim().uppercase()
            val src = runCatching { ExpSource.valueOf(name) }.getOrNull() ?: continue
            out.add(src)
        }
        if (out.isEmpty()) return EnumSet.of(ExpSource.BLOCKS, ExpSource.MOBS)
        return out
    }
}
