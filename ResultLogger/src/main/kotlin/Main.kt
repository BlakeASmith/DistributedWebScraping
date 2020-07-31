import csc.distributed.webscraper.kafka.Consumer
import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.ServiceConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import java.io.File

val kafka = Kafka(Config.get().bootstraps)

fun outputConsumer(topic: String) = Consumer(
        listOf(topic),
        StringDeserializer::class.java,
        StringDeserializer::class.java,
        kafka,
        "logger",
        autocommit = true
)

fun makeLogFile(name: String) = File("$name.log").apply { if (!exists()) createNewFile() }.printWriter()

@ExperimentalCoroutinesApi
suspend fun main() =
    ServiceConsumer(kafka).asFlow()
            // create a new file for each service log
            .map { (name, service, _) -> name to makeLogFile(name) }
            // push results to a topic with the same name as the service
            .map { (topic, writer) ->
                outputConsumer(topic).asFlow()
                        .onEach { writer.println(it) }
                        .onEach { println(it.record) }
                        .launchIn(CoroutineScope(Dispatchers.IO))
            }.collect()
