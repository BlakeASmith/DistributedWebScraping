package csc.distributed.webscraper.kafka

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import java.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
/**
 *  expose csc.distributed.webscraper.kafka.Kafka topic as a BroadcastChannel,
 *  The channel is HOT, it will get as many values as it can from
 *  csc.distributed.webscraper.kafka.Kafka as fast as it can.
 */
class TopicBroadcast<K, V> (
        topic: String,
        consumerConfig: ConsumerConfig,
        consumerPollingRate: Duration = Duration.ofMillis(100),
        context: CoroutineScope = GlobalScope,
        private val channel: BroadcastChannel<Pair<K, V>> = BroadcastChannel(Channel.BUFFERED),
        private val consumer: Consumer<K, V> = KafkaConsumer(consumerConfig.originals())
){

    constructor(
            topic: String,
            clientId: Pair<String, String>,
            bootstraps: List<String>,
            defaultKeySerializer: Class<out Deserializer<K>>,
            defaultValueSerializer: Class<out Deserializer<V>>,
            autocommit: Boolean = true,
            consumerPollingRate: Duration = Duration.ofMillis(100),
            context: CoroutineScope = GlobalScope,
            maxRecordsPerPoll: Int = 10
    ): this(topic, ConsumerConfig(mapOf(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autocommit,
            ConsumerConfig.CLIENT_ID_CONFIG to clientId.first,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstraps,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to defaultValueSerializer,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to defaultKeySerializer,
            ConsumerConfig.GROUP_ID_CONFIG to clientId.second,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxRecordsPerPoll
    )), consumerPollingRate, context)

    constructor(
            topic: Topic<K,V>,
            clientId: Pair<String, String>,
            bootstraps: List<String>,
            autocommit: Boolean = true,
            consumerPollingRate: Duration = Duration.ofMillis(100),
            context: CoroutineScope = GlobalScope,
            maxRecordsPerPoll: Int = 10
    ): this(topic.name, clientId, bootstraps,
            topic.keySerde.deserializer()::class.java, topic.valueSerde.deserializer()::class.java,
            autocommit, consumerPollingRate, context, maxRecordsPerPoll)

    init { consumer.subscribe(listOf(topic)) }

    private val records: Channel<ConsumerRecord<K, V>> = Channel(Channel.UNLIMITED)

    @FlowPreview
    fun records() = records.consumeAsFlow()

    @FlowPreview
    fun openFlow() = channel.asFlow()
    fun openChannel() = channel.openSubscription()

    private val job = flow {
        while(true) {
            consumer.poll(consumerPollingRate).forEach{ emit(it) }
        }
    }
            .onEach { channel.send(it.key() to it.value()) }
            .onEach { records.send(it) }
            .launchIn(context)

    fun close(cause: Throwable?): Boolean {
        job.cancel(CancellationException("channel closed", cause))
        records.close()
        return channel.close(cause)
    }

    suspend fun closeJoin(cause: Throwable? = null): Boolean{
        job.cancelAndJoin()
        records.close()
        return channel.close()
    }
}

