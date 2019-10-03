package net.alkaonline.alka3item.util

import me.finalchild.kotlinbukkit.util.dropItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun Player.giveOrDrop(item: ItemStack) {
    val left = this.inventory.addItem(item)
    for (entry in left) {
        this.eyeLocation.dropItem(entry.value)
    }
}
