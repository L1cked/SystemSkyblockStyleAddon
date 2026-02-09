package systemSkyblockStyleAddon

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.block.data.Ageable
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor

class AddonListener(
    private val plugin: SystemSkyblockStyleAddonPlugin,
    private val state: AddonState
) : Listener {

    private val spawnerKey = NamespacedKey(plugin, "sssa_spawner_mob")
    private val enchantRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
    private val fortuneEnchant = enchantRegistry.get(NamespacedKey.minecraft("fortune"))
    private val silkTouchEnchant = enchantRegistry.get(NamespacedKey.minecraft("silk_touch"))

    @EventHandler(ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.entity.persistentDataContainer.set(spawnerKey, PersistentDataType.BYTE, 1.toByte())
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockDrop(event: BlockDropItemEvent) {
        val mechanics = state.mechanics
        if (mechanics.autoSmelt.isEmpty() && mechanics.blockDropBonuses.isEmpty()) return

        val blockType = event.blockState.type
        val world = event.blockState.world
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val hasSilkTouch = silkTouchEnchant?.let { tool.containsEnchantment(it) } ?: false
        val fortuneLevel = fortuneEnchant?.let { tool.getEnchantmentLevel(it) } ?: 0

        if (mechanics.autoSmelt.isNotEmpty() && !hasSilkTouch) {
            for (auto in mechanics.autoSmelt) {
                if (!auto.blocks.contains(blockType)) continue
                if (!worldAllowed(world.name, auto.worlds)) continue
                val mapped = auto.mapping[blockType] ?: continue
                if (!roll(auto.chance)) continue

                for (item in event.items) {
                    val stack = item.itemStack
                    if (!isSmeltableDrop(blockType, stack.type)) continue
                    if (stack.type != mapped) {
                        item.itemStack = ItemStack(mapped, stack.amount)
                    }
                }
                break
            }
        }

        if (mechanics.blockDropBonuses.isNotEmpty()) {
            for (bonus in mechanics.blockDropBonuses) {
                if (!bonus.blocks.contains(blockType)) continue
                if (!worldAllowed(world.name, bonus.worlds)) continue
                val chance = if (bonus.fortuneScales && fortuneLevel > 0) {
                    (bonus.chance * (1 + fortuneLevel)).coerceAtMost(1.0)
                } else {
                    bonus.chance
                }
                if (!roll(chance)) continue
                dropExtras(event.blockState.location, bonus.extraDrops)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val mechanics = state.mechanics
        if (mechanics.cropBonus.isEmpty() && mechanics.autoReplant.isEmpty()) return

        val block = event.block
        val blockType = block.type
        val worldName = block.world.name
        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        if (mechanics.cropBonus.isNotEmpty()) {
            for (bonus in mechanics.cropBonus) {
                if (!bonus.crops.contains(blockType)) continue
                if (!worldAllowed(worldName, bonus.worlds)) continue
                if (!roll(bonus.chance)) continue
                val extra = randomBetween(bonus.extraAmountMin, bonus.extraAmountMax)
                val produce = cropProduce(blockType) ?: continue
                if (extra > 0) {
                    dropExtras(block.location, listOf(ExtraDrop(produce, extra, extra)))
                }
            }
        }

        if (mechanics.autoReplant.isNotEmpty()) {
            val replant = mechanics.autoReplant.firstOrNull { it.crops.contains(blockType) && worldAllowed(worldName, it.worlds) }
            if (replant != null) {
                if (replant.requireSeed) {
                    val seed = cropSeed(blockType) ?: return
                    if (!takeSeed(event.player, seed)) return
                }
                val loc = block.location
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    val target = loc.block
                    if (!target.type.isAir) return@Runnable
                    target.type = blockType
                    val data = target.blockData as? Ageable ?: return@Runnable
                    data.age = 0
                    target.blockData = data
                })
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val mechanics = state.mechanics
        if (mechanics.mobDropBonus.isEmpty() && mechanics.expMultiplier.isEmpty()) return

        val entity = event.entity
        val entityType = entity.type
        val worldName = entity.world.name

        if (mechanics.mobDropBonus.isNotEmpty()) {
            for (bonus in mechanics.mobDropBonus) {
                if (!bonus.entities.contains(entityType)) continue
                if (!worldAllowed(worldName, bonus.worlds)) continue
                if (bonus.ignoreSpawnerMobs && isSpawnerMob(entity)) continue
                if (!roll(bonus.chance)) continue
                dropExtras(entity.location, bonus.extraDrops)
            }
        }

        if (mechanics.expMultiplier.isNotEmpty()) {
            val multiplier = maxExpMultiplier(mechanics.expMultiplier, ExpSource.MOBS, worldName)
            if (multiplier != null) {
                val original = event.droppedExp
                val adjusted = adjustExp(original, multiplier)
                if (adjusted != original) {
                    event.droppedExp = adjusted
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExp(event: BlockExpEvent) {
        val mechanics = state.mechanics
        if (mechanics.expMultiplier.isEmpty()) return

        val worldName = event.block.world.name
        val multiplier = maxExpMultiplier(mechanics.expMultiplier, ExpSource.BLOCKS, worldName) ?: return
        val original = event.expToDrop
        val adjusted = adjustExp(original, multiplier)
        if (adjusted != original) {
            event.expToDrop = adjusted
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        val mechanics = state.mechanics
        if (mechanics.fishingBonus.isEmpty()) return
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return

        val worldName = event.player.world.name
        for (bonus in mechanics.fishingBonus) {
            if (!worldAllowed(worldName, bonus.worlds)) continue
            if (!roll(bonus.chance)) continue
            val loc = event.hook.location
            dropExtras(loc, bonus.extraLoot)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val mechanics = state.mechanics
        if (mechanics.anvilDiscount.isEmpty()) return
        if (event.result == null) return

        val inv = event.inventory
        val oldCost = getRepairCost(inv)
        if (oldCost <= 0) return

        val maxCost = getMaximumRepairCost(inv)
        if (oldCost >= maxCost) return

        val best = mechanics.anvilDiscount.minByOrNull { it.multiplier } ?: return
        val newCost = floor(oldCost * best.multiplier).toInt().coerceAtLeast(best.minCost)
        if (newCost < oldCost) {
            setRepairCost(inv, newCost)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val mechanics = state.mechanics
        if (mechanics.fallProtect.isEmpty()) return
        if (event.cause != EntityDamageEvent.DamageCause.FALL) return

        val player = event.entity as? Player ?: return
        val worldName = player.world.name
        val mechanic = mechanics.fallProtect
            .filter { worldAllowed(worldName, it.worlds) }
            .minByOrNull { it.damageMultiplier }
            ?: return

        val damage = event.damage
        if (damage < mechanic.minFallDamageToTrigger) return

        val now = System.currentTimeMillis()
        val cooldownUntil = state.fallCooldowns[player.uniqueId]
        if (cooldownUntil != null && now < cooldownUntil) return

        val newDamage = (damage * mechanic.damageMultiplier).coerceAtLeast(0.0)
        event.damage = newDamage
        if (mechanic.cooldownSeconds > 0) {
            state.fallCooldowns[player.uniqueId] = now + mechanic.cooldownSeconds * 1000L
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        state.fallCooldowns.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        state.fallCooldowns.remove(event.player.uniqueId)
    }

    private fun worldAllowed(worldName: String, worlds: Set<String>?): Boolean {
        return worlds == null || worlds.contains(worldName.lowercase())
    }

    private fun roll(chance: Double): Boolean {
        if (chance <= 0.0) return false
        if (chance >= 1.0) return true
        return ThreadLocalRandom.current().nextDouble() < chance
    }

    private fun randomBetween(min: Int, max: Int): Int {
        if (max <= min) return min
        return ThreadLocalRandom.current().nextInt(max - min + 1) + min
    }

    private fun dropExtras(location: Location, drops: List<ExtraDrop>) {
        if (drops.isEmpty()) return
        val world = location.world ?: return
        val dropLoc = location.clone().add(0.5, 0.5, 0.5)
        for (drop in drops) {
            val amount = randomBetween(drop.min, drop.max)
            if (amount <= 0) continue
            world.dropItemNaturally(dropLoc, ItemStack(drop.material, amount))
        }
    }

    private fun isSmeltableDrop(blockType: Material, dropType: Material): Boolean {
        if (dropType == blockType) return true
        val raw = when (blockType) {
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> Material.RAW_IRON
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER
            else -> null
        }
        return raw == dropType
    }

    private fun cropProduce(type: Material): Material? {
        return when (type) {
            Material.WHEAT -> Material.WHEAT
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT
            else -> null
        }
    }

    private fun cropSeed(type: Material): Material? {
        return when (type) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            else -> null
        }
    }

    private fun takeSeed(player: Player, seed: Material): Boolean {
        val inv = player.inventory
        val needle = ItemStack(seed, 1)
        if (!inv.containsAtLeast(needle, 1)) return false
        inv.removeItem(needle)
        return true
    }

    private fun isSpawnerMob(entity: LivingEntity): Boolean {
        return entity.persistentDataContainer.has(spawnerKey, PersistentDataType.BYTE)
    }

    private fun maxExpMultiplier(
        list: List<ExpMultiplierMechanic>,
        source: ExpSource,
        worldName: String
    ): Double? {
        var best: Double? = null
        for (entry in list) {
            if (!entry.sources.contains(source)) continue
            if (!worldAllowed(worldName, entry.worlds)) continue
            if (best == null || entry.multiplier > best) {
                best = entry.multiplier
            }
        }
        return best
    }

    private fun adjustExp(original: Int, multiplier: Double): Int {
        if (original <= 0) return original
        val cappedMultiplier = multiplier.coerceAtLeast(0.0)
        val raw = floor(original * cappedMultiplier).toInt().coerceAtLeast(0)
        val max = original * 10
        return raw.coerceAtMost(max)
    }

    @Suppress("DEPRECATION")
    private fun getRepairCost(inv: org.bukkit.inventory.AnvilInventory): Int = inv.repairCost

    @Suppress("DEPRECATION")
    private fun setRepairCost(inv: org.bukkit.inventory.AnvilInventory, cost: Int) {
        inv.repairCost = cost
    }

    @Suppress("DEPRECATION")
    private fun getMaximumRepairCost(inv: org.bukkit.inventory.AnvilInventory): Int = inv.maximumRepairCost
}
