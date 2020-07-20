import org.jsoup.nodes.Document
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

interface RoverPlugin {
    fun scrape(doc: Document): String
}

/**
 * Implements RoverPlugin by reflection into the given Class,
 * It is difficult to cast a Class<*> instance to an interface so this is my workaround.
 *
 * TODO: using java service loaders is probably a better solution
 */
class LoadedRoverPlugin(val loadedClass: Class<*>, vararg constructorArgs: Any?): RoverPlugin {
    val instance = loadedClass.getConstructor().newInstance(*constructorArgs)
    val scrapeMethod = loadedClass.getDeclaredMethod("scrape", Document::class.java)
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


fun loadPlugin(path: String, nameFilter: String = "Plugin"): RoverPlugin = loadPlugin(JarFile(path), nameFilter)

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
        .find { cls -> cls.annotatedInterfaces.also(::println).find { it.type.typeName == "RoverPlugin" } != null }!!
        .let { LoadedRoverPlugin(it) }


