import csc.distributed.webscraper.plugins.registerPlugin
import java.io.File

fun main(args: Array<String>) {
    println(args[0])
    File(args[0]).walk().drop(1)
            .filter { it.extension == "jar" }
            .forEach {
                println("registering plugin $it")
                registerPlugin(it.nameWithoutExtension, it)
            }
    println("all plugins registered")
}
