package net.alkaonline.alka3item

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import de.tr7zw.itemnbtapi.NBTItem
import de.tr7zw.itemnbtapi.NBTType
import me.finalchild.kotlinbukkit.util.*
import net.alkaonline.alka3item.util.getMethod
import net.alkaonline.alka3item.util.getNMSClass
import net.alkaonline.alka3item.util.getOBCClass
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Double.max
import java.lang.Double.min
import java.text.DecimalFormat
import java.util.*


class Alka3Item : JavaPlugin() {
    override fun onEnable() {
        loadConfig()

        server.pluginManager.registerEvents(EventListener(this), this)
    }

    private fun loadConfig() {
        saveDefaultConfig()
        val config = config
        cfgMap = config.getValues(false)
        ranks = HashBiMap.create(
            config.getConfigurationSection("ranks")!!.getValues(false)
                .mapValues { (_, v) -> v as Int })

        items = config.getConfigurationSection("items")!!
    }
}

lateinit var items: ConfigurationSection

lateinit var cfgMap: Map<String, *>

lateinit var ranks: BiMap<String, Int>

/**
 * 아이템의 등급을 설정함
 */
fun ItemStack.setRank(meanRank: Double, player: Player): ItemStack {
    val lore = if (hasLore()) lore?.toMutableList() else mutableListOf()
    lore!!.remove(cfgMap["maybe-format"])
    this.lore = lore

    val attr =
        items.getConfigurationSection(type.name.toLowerCase())?.getValues(true)?.filterKeys { it != "generic" }?.filterValues { it is MemorySection }
                ?: return clone()

    val nbti = NBTItem(this)
    val attrMod = nbti.getList("AttributeModifiers", NBTType.NBTTagCompound)
    var prefixScore = 0.0

    attr.forEach { (k, v) ->
        v as MemorySection
        val mean = v.getDouble("mean")
        val bonusByRank = v.getDouble("bonus-by-rank")
        val standardDeviation = v.getDouble("standard-deviation")
        val lowerBoundFromMean = v.getDouble("lower-bound-from-mean")
        val upperBoundFromMean = v.getDouble("upper-bound-from-mean")

        val bonusedMean = mean + bonusByRank * (meanRank - 1)
        val gaussian = Random().nextGaussian()

        val lowerBound = max(bonusedMean - lowerBoundFromMean, 0.0)
        val upperBound = bonusedMean + upperBoundFromMean
        val amount = min(max(gaussian * standardDeviation + bonusedMean, lowerBound), upperBound)

        prefixScore += (amount - lowerBound) / (upperBound - lowerBound)

        var slot = "mainhand"
        when (k) {
            "miningSpeed" -> {
                lore.add(format("mining-speed-format", DecimalFormat("#.##").format(amount)))
                return@forEach // == continue at forEach
            }
            "generic.armor", "generic.armorToughness" -> {
                slot = "chest"
            }
        }

        val listCompound = (0 until attrMod.size()).firstOrNull {
            attrMod.getCompound(it).getString("AttributeName") == k
        }?.let { attrMod.getCompound(it) } ?: attrMod.addCompound()

        when (nbti.item.type) {
            Material.DIAMOND_AXE, Material.GOLDEN_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE -> {
                val speedAttr = attrMod.addCompound()
                speedAttr.setString("Slot", "mainhand")
                speedAttr.setString("AttributeName", "generic.attackSpeed")
                speedAttr.setString("Name", "generic.attackSpeed")
                speedAttr.setDouble("Amount", -3.75)
                speedAttr.setInteger("Operation", 0)
                speedAttr.setInteger("UUIDLeast", 59764)
                speedAttr.setInteger("UUIDMost", 31483)
            }
        }

        listCompound.setString("Slot", slot)
        listCompound.setString("AttributeName", k)
        listCompound.setString("Name", k)
        listCompound.setDouble("Amount", amount)
        listCompound.setInteger("Operation", 0)
        listCompound.setInteger("UUIDLeast", 59764)
        listCompound.setInteger("UUIDMost", 31483)
    }

    val newItem = nbti.item
    newItem.lore = lore

    if (attr.isEmpty()) {
        return newItem
    }

    val rank = prefixScore / attr.size

    newItem.name =
        when {
            newItem.type.name.startsWith("DIAMOND_") -> ChatColor.AQUA
            newItem.type.name.startsWith("IRON_") -> ChatColor.GRAY
            else -> ChatColor.RESET
        } +
                when (rank) {
                    in 0.0..0.1 -> "의미를 알 수 없는 "
                    in 0.1..0.3 -> "손이 미끄러진 "
                    in 0.3..0.5 -> "좋다 하기 힘든 "
                    in 0.5..0.6 -> "쓸만한 "
                    in 0.6..0.8 -> "화력빨 "
                    in 0.8..0.9 -> "올해 운을 다 끌어다 쓴 "
                    in 0.9..1.0 -> "그냥 최강 "
                    else -> "버그가 난 "
                } +
                when {
                    newItem.type.name.startsWith("DIAMOND_") -> "다이아몬드 "
                    newItem.type.name.startsWith("IRON_") -> "철 "
                    else -> ChatColor.RESET
                } +
                when {
                    newItem.type.name.endsWith("_PICKAXE") -> "곡괭이"
                    newItem.type.name.endsWith("_AXE") -> "도끼"
                    newItem.type.name.endsWith("_CHESTPLATE") -> "갑옷"
                    newItem.type.name.endsWith("_SWORD") -> "검"
                    else -> ""
                } + ChatColor.RED + ChatColor.YELLOW + ChatColor.GREEN + ChatColor.BLACK + ChatColor.DARK_BLUE + ChatColor.RESET

    Bukkit.getLogger()
        .info("{id:${newItem.type},Damage:${newItem.type.maxDurability},Count:${newItem.amount},tag:${nbti.asNBTString()}}")
    if (player.isOp) {
        player.spigot().sendMessage(
            *ComponentBuilder("[").color(ChatColor.GRAY).append(" ! ").color(ChatColor.GREEN).append("] ").color(
                ChatColor.GRAY
            ).append(
                player.displayName
            ).color(ChatColor.GRAY).append("님이 [").color(ChatColor.WHITE).append(newItem.displayName).event(
                HoverEvent(HoverEvent.Action.SHOW_ITEM, arrayOf(TextComponent(newItem.toJson())))
            ).append("]을(를) 만들었습니다!").color(ChatColor.WHITE).create()
        )
    } else {
        if (rank > 0.8) {
            Bukkit.spigot().broadcast(
                *ComponentBuilder("[").color(ChatColor.GRAY).append(" ! ").color(ChatColor.GREEN).append("] ").color(
                    ChatColor.GRAY
                ).append(
                    player.displayName
                ).color(ChatColor.GRAY).append("님이 [").color(ChatColor.WHITE).append(newItem.displayName).event(
                    HoverEvent(HoverEvent.Action.SHOW_ITEM, arrayOf(TextComponent(newItem.toJson())))
                ).append("]을(를) 만들었습니다!").color(ChatColor.WHITE).create()
            )
        } else {
            player.spigot().sendMessage(
                *ComponentBuilder("[").color(ChatColor.GRAY).append(" ! ").color(ChatColor.GREEN).append("] ").color(
                    ChatColor.GRAY
                ).append(
                    player.displayName
                ).color(ChatColor.GRAY).append("님이 [").color(ChatColor.WHITE).append(newItem.displayName).event(
                    HoverEvent(HoverEvent.Action.SHOW_ITEM, arrayOf(TextComponent(newItem.toJson())))
                ).append("]을(를) 만들었습니다!").color(ChatColor.WHITE).create()
            )
        }
    }
    return newItem
}

/**
 * 아이템들의 등급 평균을 구함 없으면 0
 */
fun Array<ItemStack?>.averageRank(): Double {
    val filteredList = this.filterNotNull()
    for (item in filteredList) {
        if ((item.type == Material.DIAMOND || item.type == Material.IRON_INGOT) && !item.itemMeta.hasLore()) return -1.0
    }

    return filteredList
        .filter { it.itemMeta?.hasLore() ?: false } // lore가 있는 아이템만 걸러내기
        .mapNotNull {
            // 아이템들의 등급 리스트를 얻기
            val rankValue = ranks.map { (key, value) ->
                val rankLore = format("rank-format", key)
                if (it.itemMeta.lore!!.indexOf(rankLore) > -1) value
                else -1
            }.max() ?: -1

            if (rankValue > -1) rankValue else null
        }
        .let { if (it.isNotEmpty()) it.average() else 0.0 }
}

fun format(formatKey: String, vararg args: Any?) = String.format(cfgMap[formatKey] as String, *args)

fun ItemStack.toJson(): String {
    val nbtTagCompoundClazz = getNMSClass("NBTTagCompound")

    return getMethod(getNMSClass("ItemStack"), "save", nbtTagCompoundClazz)
        .invoke(
            getMethod(getOBCClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack::class.java)
                .invoke(null, this),
            nbtTagCompoundClazz.getConstructor().newInstance()
        )
        .toString()
}
