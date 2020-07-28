import com.google.gson.Gson
import com.google.gson.GsonBuilder
import csc.distributed.webscraper.plugins.KafkaConfig
import csc.distributed.webscraper.plugins.WebScraper
import csc.distributed.webscraper.services.Service
import kotlinx.coroutines.runBlocking
import java.io.File

suspend fun pushFromDir(f: File, webScraper: WebScraper) = f.walk()
        .drop(1) // drop the directory itself
        .filter { it.extension == "jar" }
        .forEach {
            println("registering plugin $it")
            webScraper.registerPlugin(it)
        }

fun openFileArgument(args: Array<String>, arg: String) = args.withIndex()
                .find { (_, value) -> value == arg }
                ?.let { args[it.index+1] }
                ?.let { File(it) }

fun parseServicesFromJson(json: String, gson: Gson = Gson()) =
        gson.fromJson(json, mutableListOf<Service>()::class.java)

fun writeExampleJson(file: File){
    val service = Service("example", listOf("https://www.example.com"), listOf("/dontlook"), "<ip>" to 6969 , listOf("wordcount"))
    GsonBuilder().setPrettyPrinting().create().toJson(mutableListOf(service))
            .let {json ->
                file.apply {
                    if (!exists()) createNewFile()
                    writeText(json)
                }
            }
}

fun main(args: Array<String>) {
    openFileArgument(args, "--genexample")?.also { writeExampleJson(it) }
    val webScraper = WebScraper(KafkaConfig(listOf("127.0.0.1:9092")))
    openFileArgument(args, "--jars")?.also {
        runBlocking {
            pushFromDir(it, webScraper)
        }
    }
}
