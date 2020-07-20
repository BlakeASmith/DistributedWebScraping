package plugin

import RoutingService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

interface RoverPlugin {
    fun scrape(doc: Document): String
}

class LoadedRoverPlugin(val loadedClass: Class<*>, vararg constructorArgs: Any?): RoverPlugin {
    val instance = loadedClass.getConstructor().newInstance(*constructorArgs)
    val scrapeMethod = loadedClass.getDeclaredMethod("scrape", Document::class.java)
    override fun scrape(doc: Document): String = scrapeMethod(instance, doc) as String

}

fun loadClassesFromJar(path: String, nameFilter: String = "Plugin") = plugin.loadClassesFromJar(java.util.jar.JarFile(path))

fun loadClassesFromJar(jar: JarFile, nameFilter: String = "Plugin") = jar
        .entries().toList().associateWith { URL("jar:file:${jar.name}!/") }
        .let { it.keys to URLClassLoader.newInstance(it.values.toTypedArray()) }
        .let { (jars, cl) ->
            jars.filterNot { it.isDirectory || !it.name.endsWith(".class") }
                    .filter { nameFilter in it.name }
                    .map { cl.loadClass(it.name.removeSuffix(".class")) }
        }

fun loadRoverPlugin(path: String, nameFilter: String = "Plugin"): RoverPlugin = loadRoverPlugin(JarFile(path), nameFilter)

fun loadRoverPlugin(jar: JarFile, nameFilter: String = "Plugin") = loadClassesFromJar(jar, nameFilter)
        .find { cls -> cls.annotatedInterfaces.find { it.type.typeName == "RoverPlugin" } != null }!!
        .let { LoadedRoverPlugin(it) }


fun main(){
    RoutingService(Configuration.routingServiceAddress)
            .getAllPlugin(".")
            .forEach { it.scrape(Jsoup.connect("https://www.google.com").get()) }

}
