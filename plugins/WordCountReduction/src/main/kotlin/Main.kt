import ca.blakeasmith.kkafka.jvm.BootstrapServer
import ca.blakeasmith.kkafka.jvm.KafkaConfig
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.types.Service
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list

val config = KafkaConfig(Config.get().bootstraps.map { BootstrapServer.fromString(it) })

@ExperimentalCoroutinesApi
val plugins = Scraper.Plugins(config)

@ExperimentalCoroutinesApi
val services = Scraper.Services(config)


@Serializable
data class WCResult(val word: String, val occ: Int)

@ExperimentalCoroutinesApi
suspend fun main() {
    Thread.currentThread().contextClassLoader
        .getResourceAsStream("WordCount.jar")!!
        .also(::println).readBytes()
        .also { plugins.write("WordCount", it) }
        .also { println("sent plugin") }
        .also { plugins.close() }

    val WCService = Service(
        "wcservice",
        listOf("https://usedvictoria.com"),
        listOf("#", "twitter", "facebook"),
        listOf("WordCount")
    )

    with(services){
        write(WCService.name, WCService)
        println("sent service")
        close()
    }

    Scraper.Output(WCService.name, WCResult.serializer().list, config)
        .read("wordcount-reducer")
        .flatMapConcat { it.third.asFlow() }
        .fold(mutableMapOf<String, Int>()) { map, res ->
            map[res.word] = map.getOrDefault(res.word, 0) + res.occ
            map.also { println("$it".take(100)) }
        }

}

