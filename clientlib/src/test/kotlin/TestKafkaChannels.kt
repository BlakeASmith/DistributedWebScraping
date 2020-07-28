import csc.distributed.webscraper.kafka.ConsumerChannel
import csc.distributed.webscraper.kafka.ProducerChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.TopicPartitionInfo
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Test

@FlowPreview
@ExperimentalCoroutinesApi
class TestKafkaChannels {
    val mockProducer: MockProducer<String, String> = MockProducer()
    val mockConsumer: MockConsumer<String, String> = MockConsumer(OffsetResetStrategy.LATEST)

    val channel = {
        ProducerChannel<String, String>(
                "test",
                ProducerConfig(mapOf(
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
                )),
                GlobalScope,
                Channel(),
                mockProducer
        )
    }

    val consumerChannel = {
        ConsumerChannel<String, String>(
                "test",
                ConsumerConfig(mapOf(
                        ConsumerConfig.CLIENT_ID_CONFIG to "test",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringSerializer::class.java
                )),
                consumer = mockConsumer
        )
    }


    @Test
    fun testSendToChannel() = runBlocking {
        val producer = channel()
        val consumer = consumerChannel()
        repeat(10) {
            producer.sendBlocking("foo" to "bar")
        }

        val partitions = listOf(TopicPartition("test", 0))
        mockConsumer.rebalance(partitions)
        mockConsumer.updateBeginningOffsets(partitions.associateWith { 0L })
        mockConsumer.addEndOffsets(partitions.associateWith { 20L })
        var offset: Long = 0
        producer.closeJoin()
        assert(mockProducer.history().size == 10)
        assert(mockProducer.closed())

        mockProducer.history()
                .forEach {
                    mockConsumer.addRecord(ConsumerRecord(it.topic(), 0, offset++, it.key(), it.value()))
                    println("added record")
                }

        consumer.receive()

        println("closed producer")
        println("closed producer")
    }
}

