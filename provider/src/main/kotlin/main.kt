import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.types.Service
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

val json = Json(JsonConfiguration.Stable)

fun jarsIn(dir: File) = dir.walk()
        .drop(1) // drop the directory itself
        .filter { it.extension == "jar" }

fun openFileArgument(args: Array<String>, arg: String) = args.withIndex()
                .find { (_, value) -> value == arg }
                ?.let { args[it.index+1] }
                ?.let { File(it) }

fun writeExampleJson(file: File){
    val service = Service("example", listOf("https://www.example.com"), listOf("/dontlook") , listOf("wordcount"))
    json.stringify(Service.serializer().list, listOf(service))
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
    val kafka = KafkaConfig(Config.get().bootstraps.map { BootstrapServer.fromString(it) })

    ArgumentParser<File, Unit>{ File(it) }
            .async("--genexample"){ writeExampleJson(this) }
            .async("--jars"){
                val plugs = jarsIn(this).map { it.nameWithoutExtension to it.readBytes() }
                        .onEach { println("sending $it to kafka") }
                Scraper.Plugins(kafka)
                        .writeFrom(plugs.asFlow())
                        .join()
            }.async("--services"){
                val services = json.parse(Service.serializer().list, readText())
                    .map { it.name to it }.asFlow().onEach { println("sending $it to kafka") }
                Scraper.Services(kafka).writeFrom(services).join()
            }.parse(args)
}
