import com.google.gson.Gson
import com.google.gson.GsonBuilder
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.*
import csc.distributed.webscraper.services.Service
import csc.distributed.webscraper.services.ServiceSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.StringSerializer
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
    val kafka = Kafka(Config.get().bootstraps)
    val serviceProduction = Producer(kafka, StringSerializer::class.java, ServiceSerializer::class.java)
            .produceTo("services")

    ArgumentParser<File, Unit>{ File(it) }
            .async("--genexample"){ writeExampleJson(this) }
            .async("--jars"){
                jarsIn(this).map { it.nameWithoutExtension to it.readBytes() }
                        .asFlow()
                        .produceTo(pluginProduction(kafka))
                        .onEach { println("sent ${it.first} to Kafka, size is ${it.second.size} bytes") }
                        .collect()
            }.async("--services"){
                parseServicesFromJson(readText())
                        .map { it.name to it }.asFlow()
                        .produceTo(serviceProduction)
                        .onEach { println("sent $it to kafka") }
                        .collect()
            }.parse(args)
}
