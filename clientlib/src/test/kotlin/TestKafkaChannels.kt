import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.Serdes
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class TestKafkaChannels {
    val mockProducer: MockProducer<String, String> = MockProducer()
    val mockConsumer: MockConsumer<String, String> = MockConsumer(OffsetResetStrategy.LATEST)

    @Test
    fun testSendToChannel() = runBlocking {
    }

    @Test
    fun TestWebscraper() {
        val plugins = Topic("plugins", Serdes.String(), Serdes.ByteArray())
        plugins.defaultConfig = Kafka("test", listOf("127.0.0.1:9092"), autocommit = false)

        runBlocking {
            println("reading available")
            plugins.readAvailable().last().also(::println)
        }
    }
}

