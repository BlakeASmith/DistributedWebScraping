import com.google.gson.Gson
import com.google.gson.GsonBuilder
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.PLUGIN_TOPIC
import csc.distributed.webscraper.plugins.services
import csc.distributed.webscraper.plugins.writeDefaultConfig
import csc.distributed.webscraper.services.Service
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.time.Duration

fun jarsIn(dir: File) = dir.walk()
        .drop(1) // drop the directory itself
        .filter { it.extension == "jar" }

fun openFileArgument(args: Array<String>, arg: String) = args.withIndex()
                .find { (_, value) -> value == arg }
                ?.let { args[it.index+1] }
                ?.let { File(it) }

fun parseServicesFromJson(json: String, gson: Gson = Gson()) =
        gson.fromJson<Array<Service>>(json, Array<Service>::class.java)

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

@ExperimentalCoroutinesApi
@FlowPreview
fun main(args: Array<String>) {
    val config = Config.get()
    PLUGIN_TOPIC.defaultConfig = Kafka(
            "plugin-provider",
            config.bootstraps,
            false,
            GlobalScope,
            Duration.ofSeconds(5),
            1
    )

    services.defaultConfig = PLUGIN_TOPIC.defaultConfig!!.copy(defaultGroupId = "services-provider")

    ArgumentParser<File, Unit>{ File(it) }
            .async("--genexample"){ writeExampleJson(this) }
            .async("--jars"){
                jarsIn(this).map { it.nameWithoutExtension to it.readBytes() }
                        .asFlow()
                        .produceTo(PLUGIN_TOPIC.producer())
                        .onEach { println("sent ${it.first} to Kafka, size is ${it.second.size} bytes") }
                        .collect()
            }.async("--services"){
                parseServicesFromJson(readText())
                        .map { it.name to it }.asFlow()
                        .produceTo(services.producer())
                        .onEach { println("sent $it to kafka") }
                        .collect()
            }.parse(args)
}
