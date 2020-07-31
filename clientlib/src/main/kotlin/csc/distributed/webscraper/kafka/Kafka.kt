package csc.distributed.webscraper.kafka
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.time.Duration


data class Kafka(
        val bootstraps: List<String> = listOf("127.0.0.1:9092"),
        val context: CoroutineScope = GlobalScope
)

open class Producer<K, V>(
        val kafka: Kafka,
        val keySerializer: Class<out Serializer<K>>,
        val valueSerializer: Class<out Serializer<V>>,
        val clientId: String? = null
): KafkaProducer<K, V>(mutableMapOf<String, Any>().apply {
    clientId?.let { put(ProducerConfig.CLIENT_ID_CONFIG, it) }
    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstraps)
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer)
    clientId?.let { put(ProducerConfig.CLIENT_ID_CONFIG, it) }
}){

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun produceTo(topic: String) = Production<K, V>(topic, this, kafka.context)
}

data class TopicEntry<K, V>(val key: K, val value: V, val record: ConsumerRecord<K, V>)

open class Consumer<K, V>(
        val topics: List<String>,
        val keyDeserializer: Class<out Deserializer<K>>,
        val valueDeserializer: Class<out Deserializer<V>>,
        val kafka: Kafka,
        val groupId: String? = null,
        val clientId: String? = null,
        val autocommit: Boolean = false,
        val autoOffsetReset: AutoOffsetConfig = AutoOffsetConfig.earliest,
        val fetchBytesRange: IntRange = 1..Int.MAX_VALUE,
        val maxPollRecords: Int = 20,
        val maxPollInterval: Duration = Duration.ofSeconds(5),
        val pollTimeout: Duration = Duration.ofSeconds(1)
): KafkaConsumer<K,V>(
        mutableMapOf<String, Any>().apply {
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,   autocommit)
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstraps)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer)
            clientId?.let { put(ConsumerConfig.CLIENT_ID_CONFIG, it) }
            groupId?.let { put(ConsumerConfig.GROUP_ID_CONFIG, it) }
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset.name)
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords)
            put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval.toMillis().toInt())
            put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchBytesRange.last)
            put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchBytesRange.first)
        }
){

    @ExperimentalCoroutinesApi
    private fun unassignedFlow() =
            generateSequence { poll(pollTimeout) }.flatMap { it.asSequence() }
                    .asFlow()
                    .onCompletion { close() }
                    .map { TopicEntry(it.key(), it.value(), it) }

    @ExperimentalCoroutinesApi
    fun asFlow(): Flow<TopicEntry<K, V>> = this.apply { subscribe(topics) }.unassignedFlow()

    @ExperimentalCoroutinesApi
    fun partitionFlow(partitions: List<TopicPartition>) = this.apply { assign(partitions) }.unassignedFlow()

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun asBroadcast(): BroadcastChannel<TopicEntry<K, V>> = asFlow().broadcastIn(kafka.context)

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun asChannel(): ReceiveChannel<TopicEntry<K, V>> = asFlow().produceIn(kafka.context)

    @ExperimentalCoroutinesApi
    @FlowPreview
    fun asResolver(): FlowResolver<K, V, TopicEntry<K, V>> = FlowResolver(asFlow(), kafka.context) { entry: TopicEntry<K, V> ->
        entry.key to entry.value
    }
}






