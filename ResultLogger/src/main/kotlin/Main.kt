import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.kafka.Topic
import csc.distributed.webscraper.kafka.asFlow
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.services
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.apache.kafka.common.serialization.Serdes
import java.io.File

val defaultKafka = Kafka("logger", Config.get().bootstraps, autocommit = false)

@ExperimentalCoroutinesApi
suspend fun main(){
    services.defaultConfig = defaultKafka
    services.asFlow().map { it.first to File("${it.first}.log").apply { if (!exists()) createNewFile() }.printWriter() }
        .map { (topic, writer) ->
            Topic(topic, Serdes.String(), Serdes.String()).apply {
                this.defaultConfig = defaultKafka
            }.asFlow()
                .onEach { writer.println(it) }
                .onEach { println("logged  ${it.first} results ${it.second.take(50)} on service $topic") }
                .launchIn(CoroutineScope(Dispatchers.IO))
        }.collect()
}