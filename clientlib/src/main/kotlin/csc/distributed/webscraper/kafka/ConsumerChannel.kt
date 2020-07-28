package csc.distributed.webscraper.kafka

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import java.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
class ConsumerChannel<K, V>(
        topic: String,
        consumerConfig: ConsumerConfig,
        consumerPollingRate: Duration = Duration.ofMillis(100),
        context: CoroutineScope = GlobalScope,
        private val channel: Channel<Pair<K, V>> = Channel(),
        private val consumer: Consumer<K, V> = KafkaConsumer(consumerConfig.originals())
): ReceiveChannel<Pair<K, V>> by channel{

    constructor(
            topic: String,
            clientId: Pair<String, String?>,
            bootstraps: List<String>,
            defaultKeySerializer: Class<out Deserializer<K>>,
            defaultValueSerializer: Class<out Deserializer<V>>,
            autocommit: Boolean = true,
            consumerPollingRate: Duration = Duration.ofMillis(100),
            context: CoroutineScope = GlobalScope
    ): this(topic, ConsumerConfig(mapOf(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autocommit,
            ConsumerConfig.CLIENT_ID_CONFIG to clientId.first,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstraps,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to defaultValueSerializer,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to defaultKeySerializer,
            ConsumerConfig.GROUP_ID_CONFIG to clientId.second
    )), consumerPollingRate, context)

    init {
        consumer.subscribe(listOf(topic))
    }

    private val job = flow {
        while(true) {
            emitAll(consumer.poll(consumerPollingRate).asFlow())
        }
    }.onEach { channel.send(it.key() to it.value()) }.launchIn(context)

    fun close(cause: Throwable?): Boolean {
        job.cancel(CancellationException("channel closed", cause))
        return channel.close(cause)
    }

    suspend fun closeJoin(cause: Throwable? = null): Boolean{
        job.cancelAndJoin()
        return channel.close()
    }
}