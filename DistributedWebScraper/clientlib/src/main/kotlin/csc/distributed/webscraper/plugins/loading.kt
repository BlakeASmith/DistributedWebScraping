package csc.distributed.webscraper.plugins

import org.jsoup.nodes.Document
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Implements Plugin by reflection into the given Class,
 * It is difficult to cast a Class<*> instance to an interface so this is my workaround.
 *
 * TODO: using java service loaders is probably a better solution
 */
class LoadedPlugin(val loadedClass: Class<*>, vararg constructorArgs: Any?): Plugin {
    private val instance: Any = loadedClass.getConstructor().newInstance(*constructorArgs)
    val scrapeMethod: Method = loadedClass.getDeclaredMethod("scrape", Document::class.java)
    override fun scrape(doc: Document): String = scrapeMethod(instance, doc) as String
}

/**
 * load all the classes from the given JarFile which contain the
 * given nameFilter string.
 *
 * @param jar: the JarFile from which to load classes
 * @param nameFilter: a substring which must be in the name of a class for it to be loaded, 'Plugin' by default
 */
fun loadClassesFromJar(jar: JarFile, nameFilter: String = "Plugin") = jar
        .entries().toList().associateWith { URL("jar:file:${jar.name}!/") }
        .let { it.keys to URLClassLoader.newInstance(it.values.toTypedArray()) }
        .let { (jars, cl) ->
            jars.filterNot { it.isDirectory || !it.name.endsWith(".class") }
                    .filter { nameFilter in it.name }
                    .map { cl.loadClass(it.name.removeSuffix(".class")) }
        }

fun loadClassesFromJar(path: String, nameFilter: String = "Plugin") = loadClassesFromJar(JarFile(path))


fun loadPlugin(path: String, nameFilter: String = "Plugin"): Plugin = loadPlugin(JarFile(path), nameFilter)

/**
 * loads a Plugin out of the given JarFile
 * the Jar must contain a class which implements the 'RoverPlugin' interface
 *
 * @param jar: the JarFile to load from
 * @param nameFilter: a substring which must be contained in the name of a class for it to be loaded
 *
 * @return a LoadedRoverPlugin which accesses the methods of the plugin via reflection
 */
fun loadPlugin(jar: JarFile, nameFilter: String = "Plugin") = loadClassesFromJar(jar, nameFilter)
        .find { cls -> cls.annotatedInterfaces.find { it.type.typeName == "Plugin" } != null }!!
        .also { println("loaded ${it.name}") }
        .let { LoadedPlugin(it) }

fun loadAllFromDir(path: String) = File(path).apply {
    if (!exists()) {
        createNewFile()
        mkdir()
    }
    
}.walk().drop(1)
        .filter { it.extension == "jar" }
        .map { JarFile(it) }
        .associateBy { it.name }
        .mapValues { (_, jar) -> loadPlugin(jar) }
        .toMutableMap()

