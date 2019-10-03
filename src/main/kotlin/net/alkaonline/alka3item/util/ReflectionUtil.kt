package net.alkaonline.alka3item.util


import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

/*
 * The server version string to location NMS & OBC classes
 */
private lateinit var versionString: String

/*
 * Cache of NMS classes that we've searched for
 */
private val loadedNMSClasses = mutableMapOf<String, Class<*>>()

/*
 * Cache of OBS classes that we've searched for
 */
private val loadedOBCClasses = mutableMapOf<String, Class<*>>()

/*
 * Cache of methods that we've found in particular classes
 */
private val loadedMethods = mutableMapOf<Class<*>, MutableMap<String, Method>>()

/*
 * Cache of fields that we've found in particular classes
 */
private val loadedFields = mutableMapOf<Class<*>, MutableMap<String, Field>>()

/**
 * Gets the version string for NMS & OBC class paths
 *
 * @return The version string of OBC and NMS packages
 */
val version by lazy {
    val name = Bukkit.getServer().javaClass.`package`.name
    name.substring(name.lastIndexOf('.') + 1) + "."
}

/**
 * Get an NMS Class
 *
 * @param nmsClassName The name of the class
 * @return The class
 */
fun getNMSClass(nmsClassName: String): Class<*> {
    if (loadedNMSClasses.containsKey(nmsClassName)) {
        return loadedNMSClasses[nmsClassName]!!
    }

    val clazzName = "net.minecraft.server." + version + nmsClassName
    val clazz: Class<*>

    clazz = Class.forName(clazzName)

    loadedNMSClasses[nmsClassName] = clazz
    return clazz
}

/**
 * Get a class from the org.bukkit.craftbukkit package
 *
 * @param obcClassName the path to the class
 * @return the found class at the specified path
 */
@Synchronized
fun getOBCClass(obcClassName: String): Class<*> {
    if (loadedOBCClasses.containsKey(obcClassName)) {
        return loadedOBCClasses[obcClassName]!!
    }

    val clazzName = "org.bukkit.craftbukkit." + version + obcClassName
    val clazz: Class<*>

    clazz = Class.forName(clazzName)

    loadedOBCClasses[obcClassName] = clazz
    return clazz
}

/**
 * Get a Bukkit [Player] players NMS playerConnection object
 *
 * @param player The player
 * @return The players connection
 */
fun getConnection(player: Player): Any {
    val getHandleMethod = getMethod(player.javaClass, "getHandle")

    val nmsPlayer = getHandleMethod.invoke(player)
    val playerConField = getField(nmsPlayer.javaClass, "playerConnection")
    return playerConField.get(nmsPlayer)
}

/**
 * Get a classes constructor
 *
 * @param clazz  The constructor class
 * @param params The parameters in the constructor
 * @return The constructor object
 */
fun getConstructor(clazz: Class<*>, vararg params: Class<*>): Constructor<*> {
    return clazz.getConstructor(*params)


}

/**
 * Get a method from a class that has the specific paramaters
 *
 * @param clazz      The class we are searching
 * @param methodName The name of the method
 * @param params     Any parameters that the method has
 * @return The method with appropriate paramaters
 */
fun getMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>): Method {
    if (!loadedMethods.containsKey(clazz)) {
        loadedMethods[clazz] = mutableMapOf()
    }

    val methods = loadedMethods[clazz]!!

    if (methods.containsKey(methodName)) {
        return methods[methodName]!!
    }

    val method = clazz.getMethod(methodName, *params)!!
    methods[methodName] = method
    loadedMethods[clazz] = methods
    return method

}

/**
 * Get a field with a particular name from a class
 *
 * @param clazz     The class
 * @param fieldName The name of the field
 * @return The field object
 */
fun getField(clazz: Class<*>, fieldName: String): Field {
    if (!loadedFields.containsKey(clazz)) {
        loadedFields[clazz] = HashMap()
    }

    val fields = loadedFields[clazz]!!

    if (fields.containsKey(fieldName)) {
        return fields[fieldName]!!
    }

    val field = clazz.getField(fieldName)
    fields[fieldName] = field
    loadedFields[clazz] = fields
    return field

}
