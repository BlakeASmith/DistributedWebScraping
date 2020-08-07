import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.plugins.*
import csc.distributed.webscraper.services.Service
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.common.serialization.StringSerializer
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
    val webscraper = ScrapingApplication("provider", kafka)

    ArgumentParser<File, Unit>{ File(it) }
            .async("--genexample"){ writeExampleJson(this) }
            .async("--jars"){
                jarsIn(this).map { it.nameWithoutExtension to it.readBytes() }
                    .asFlow()
                    .sendAndReceiveRecords(kafka, webscraper.pluginsTopic)
                        .onEach { println("sent ${it} to Kafka") }
                        .collect()
            }.async("--services"){
                json.parse(Service.serializer().list, readText())
                    .map { it.name to it }.asFlow()
                    .sendAndReceiveRecords(kafka, webscraper.servicesTopic)
                    .onEach { println("sent $it to kafka") }
                    .collect()
            }.parse(args)
}
