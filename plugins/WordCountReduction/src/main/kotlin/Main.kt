import ca.blakeasmith.kkafka.jvm.BootstrapServer
import ca.blakeasmith.kkafka.jvm.Consumer
import ca.blakeasmith.kkafka.jvm.KafkaConfig
import ca.blakeasmith.kkafka.jvm.readableBy
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.definitions.types.JobMetadata
import csc.distributed.webscraper.definitions.types.Service
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

val config = KafkaConfig(Config.get().bootstraps.map { BootstrapServer.fromString(it) })

@ExperimentalCoroutinesApi
val plugins = Scraper.Plugins(config)

@ExperimentalCoroutinesApi
val services = Scraper.Services(config)


@Serializable
data class WCResult(val word: String, val occ: Int)

@ExperimentalCoroutinesApi
suspend fun main(args: Array<String>) {

    Thread.currentThread().contextClassLoader
        .getResourceAsStream("WordCount.jar")!!
        .also(::println).readBytes()
        .also { plugins.write("WordCount", it) }
        .also { println("sent plugin") }
        .also { plugins.close() }

    val WCService = Service(
            "wcservice",
            listOf(
                    "http://wikipedia.com"
            ),
            listOf("#"),
            listOf("WordCount")
    )

    with(services){
        write(WCService.name, WCService)
        println("sent service")
        close()
    }

    val logfile = File("logs/result.json").apply {
        createNewFile()
    }
    val metadatafile = File("logs/metadata.json").apply {
        createNewFile()
    }
    val json = Json(JsonConfiguration.Stable)

    GlobalScope.launch {
        withContext(Dispatchers.IO){
            Scraper.Metadata(config)
                    .readableBy { Consumer.committing("logging", config) }
                    .read()
                    .onEach {
                        metadatafile.appendText(json.stringify(JobMetadata.serializer(), it.second))
                        metadatafile.appendText("\n\n")
                        println(it.second)
                    }
                    .collect()
        }
    }

    Scraper.Output(WCService.name, WCResult.serializer().list, config)
        .read("wordcount-reducer")
        .flatMapConcat { it.third.asFlow() }
        .fold(mutableMapOf<String, Int>()) { map, res ->
            map[res.word] = map.getOrDefault(res.word, 0) + res.occ
            logfile.writeText(json.stringify(WCResult.serializer().list, map.toList().map { WCResult(it.first, it.second) }))
            map
        }
}

