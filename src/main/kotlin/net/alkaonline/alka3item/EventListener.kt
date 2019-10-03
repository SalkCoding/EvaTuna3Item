package net.alkaonline.alka3item

import com.codingforcookies.armorequip.ArmorEquipEvent
import de.tr7zw.itemnbtapi.NBTItem
import de.tr7zw.itemnbtapi.NBTType
import me.finalchild.kotlinbukkit.util.dropItem
import me.finalchild.kotlinbukkit.util.name
import net.alkaonline.alka3item.util.giveOrDrop
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.Damageable

class EventListener(val plugin: Alka3Item) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPrepareCraft(e: PrepareItemCraftEvent) {
        var result = e.inventory.result?.clone() ?: return
        if (e.isRepair) {
            e.inventory.result = null
            return
        }
        val type = result.type
        when (type) {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD -> {
                val nbti = NBTItem(result)
                val attrMod = nbti.getList("AttributeModifiers", NBTType.NBTTagCompound)
                /*val speedAttr = attrMod.addCompound()
                speedAttr.setString("Slot", "mainhand")
                speedAttr.setString("AttributeName", "generic.attackSpeed")
                speedAttr.setString("Name", "generic.attackSpeed")
                speedAttr.setDouble("Amount", 1000.0)
                speedAttr.setInteger("Operation", 0)
                speedAttr.setInteger("UUIDLeast", 59764)
                speedAttr.setInteger("UUIDMost", 31483)*/
                val damageAttr = attrMod.addCompound()
                damageAttr.setString("Slot", "mainhand")
                damageAttr.setString("AttributeName", "generic.attackDamage")
                damageAttr.setString("Name", "generic.attackDamage")
                damageAttr.setDouble(
                    "Amount", when (type) {
                        Material.WOODEN_SWORD -> 2.0
                        Material.STONE_SWORD -> 2.5
                        Material.GOLDEN_SWORD -> 2.0
                        Material.IRON_SWORD -> 3.0
                        Material.DIAMOND_SWORD -> 3.5
                        else -> 0.0
                    }
                )
                damageAttr.setInteger("Operation", 0)
                damageAttr.setInteger("UUIDLeast", 894654)
                damageAttr.setInteger("UUIDMost", 2872)
                result = nbti.item
                e.inventory.result = result
            }
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.GOLDEN_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET,
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS,
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.GOLDEN_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS -> {
                e.inventory.result = null
                return
            }
        }

        if (result.type.name.toLowerCase() !in items) return

        val mean = e.inventory.matrix.averageRank()
        if (mean > 0) {
            val lore = result.itemMeta.lore ?: mutableListOf()

            lore.add(cfgMap["maybe-format"] as String)

            result.lore = lore
            e.inventory.result = result
        } else {
            e.inventory.result = null
            return
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onCraft(e: CraftItemEvent) {
        val result = e.inventory.result?.clone() ?: return
        if (result.type.name.toLowerCase() !in items) return

        val meanRank = e.inventory.matrix.averageRank()
        if (meanRank <= 0) return

        e.isCancelled = true
        e.inventory.matrix.forEach {
            if (it != null) {
                if (it.amount == 1) {
                    e.inventory.result = null
                }
                it.amount--
            }
        }

        (e.inventory.viewers[0] as Player).giveOrDrop(result.setRank(meanRank, e.inventory.viewers[0] as Player))
    }

    /*@EventHandler
    fun onRepair(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val item = inventory.getItem(0)
        val subItem = inventory.getItem(1)
        val result = inventory.getItem(2)

        if (item == null || item.type == Material.AIR) return
        if (subItem == null || subItem.type == Material.AIR) return
        if (result == null || result.type == Material.AIR) return

        if (!item.name!!.endsWith("" + ChatColor.RED + ChatColor.YELLOW + ChatColor.GREEN + ChatColor.BLACK + ChatColor.DARK_BLUE + ChatColor.RESET)) return
        if (!subItem.name!!.endsWith("" + ChatColor.RED + ChatColor.YELLOW + ChatColor.GREEN + ChatColor.BLACK + ChatColor.DARK_BLUE + ChatColor.RESET)) return

        //min( 아이템 A 사용량 + 아이템 B 사용량 + floor(최대 사용량 / 20), 최대 사용량)
        //주괴로는 25%+
        when (item.type) {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD,
            Material.DIAMOND_AXE, Material.GOLDEN_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE -> {
                val meta = result.itemMeta
                meta.setDisplayName(item.itemMeta.displayName)
                when (subItem.type) {
                    Material.IRON_INGOT, Material.DIAMOND -> {
                        val lore = subItem.lore
                        if(lore == null || lore.size <= 0) return
                        if(meta is Damageable){
                            meta.damage -= (result.type.maxDurability / 4)
                            result.itemMeta = meta
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }*/

    /**
     * Listens for inventory events for changing equipment
     *
     * @param event event details
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.type == InventoryType.CRAFTING || event.inventory.type == InventoryType.PLAYER) {
            if (event.slotType == InventoryType.SlotType.ARMOR || event.isShiftClick) {
                val player = event.whoClicked as Player
                checkEquips(player)
            }
        }
    }

    /**
     * Listens for inventory events for changing equipment
     *
     * @param event event details
     */
    @EventHandler(priority = EventPriority.MONITOR,ignoreCancelled = true)
    fun onEquip(event: ArmorEquipEvent) {
        checkEquips(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR,ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        if(event.entityType != EntityType.PLAYER) return

        checkEquips(event.entity as Player)
    }



   /* @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val mainName = event.player.inventory.itemInMainHand.type.name
            val offName = event.player.inventory.itemInOffHand.type.name
            if (mainName.contains("_LEGGINGS") || mainName.contains("_BOOTS") || mainName.contains("_HELMET"))
                checkEquips(event.player)
            if (offName.contains("_LEGGINGS") || offName.contains("_BOOTS") || offName.contains("_HELMET"))
                checkEquips(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        checkEquips(event.entity)
    }*/

    /**
     * Runs a task one tick later that evaluates the player's equipment for any changes
     *
     * @param player player to evaluate
     */
    private fun checkEquips(player: Player) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.inventory.helmet?.type?.name?.endsWith("_HELMET") == true) {
                player.location.dropItem(player.inventory.helmet!!, true)
                player.inventory.helmet = null
            }
            if (player.inventory.leggings?.type?.name?.endsWith("_LEGGINGS") == true) {
                player.location.dropItem(player.inventory.leggings!!, true)
                player.inventory.leggings = null
            }
            if (player.inventory.boots?.type?.name?.endsWith("_BOOTS") == true) {
                player.location.dropItem(player.inventory.boots!!, true)
                player.inventory.boots = null
            }
        })
    }

}
