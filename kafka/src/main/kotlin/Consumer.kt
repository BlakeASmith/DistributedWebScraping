package ca.blakeasmith.kkafka.jvm

import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerialization
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration


data class Topic<K, V>(val name: String, val consumer: KeyValueSerialization<K, V>)

@ExperimentalCoroutinesApi
fun consumer(group: String, kafkaConfig: KafkaConfig, init: Consumer.Builder.() -> Unit) =
    Consumer.Builder(group, kafkaConfig).apply(init).build()

fun <K, T> KafkaConsumer<K, T>.asSequence(pollLength: Duration = Duration.ofSeconds(5)) =
        generateSequence {
            poll(pollLength)
        }.flatMap { it.asSequence() }

fun <K, T> KafkaConsumer<K, T>.asFlow(pollLength: Duration= Duration.ofSeconds(5)) = asSequence(pollLength)
        .asFlow()
        .onCompletion { close() }

@ExperimentalCoroutinesApi
class Consumer(val group: String, val kafkaConfig: KafkaConfig, val config: ConsumerConfig) {

    companion object {
        fun committing(group: String, kafkaConfig: KafkaConfig) =
            consumer(group, kafkaConfig) {
                autocommit(true)
            }

        fun nonCommitting(group: String, kafkaConfig: KafkaConfig) =
            consumer(group, kafkaConfig) {
                autocommit(false)
            }

        fun UUID(kafkaConfig: KafkaConfig, autocommit: Boolean, props: Map<String, Any> = mutableMapOf()) =
                consumer(java.util.UUID.randomUUID().toString(), kafkaConfig){
                    autocommit(autocommit)
                    props.forEach { set(it.key, it.value) }
                }
    }

    class Builder(val group: String, val kafkaConfig: KafkaConfig){
        private val props = mutableMapOf<String, Any>().apply {
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,   true)
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstraps.map { it.toString() })
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name.toLowerCase())
                put(ConsumerConfig.GROUP_ID_CONFIG, group)
            }

        fun autocommit(enabled: Boolean) = this.apply {
            props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enabled
        }

        fun offsetResetStrategy(offsetResetStrategy: OffsetResetStrategy) = this.apply {
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = offsetResetStrategy.name.toLowerCase()
        }

        fun maxRecordsPerPoll(n: Int){
            props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = n
        }

        fun set(prop: String, value: Any) = this.apply{
            props[prop] = value
        }

        fun build() = Consumer(
            group,
            kafkaConfig,
            ConsumerConfig(props)
        )
    }

    private val partitions = mutableMapOf<String, List<TopicPartition>>()

    private fun <K, V> kafkaConsumer(topic: Topic<K, V>) =
            KafkaConsumer<K, V>(config.originals().apply {
                this[ConsumerConfig.KEY_DESERIALIZER_CLASS_DOC] =
                        topic.consumer.keyDeserializer::class.java
                this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                        topic.consumer.valueDeserializer::class.java
            })

    fun <K, V> open(topic: Topic<K, V>,
                    pollLength: Duration = Duration.ofSeconds(5),
                    init: KafkaConsumer<K, V>.() -> Unit = {}
    ) =
            kafkaConsumer(topic)
                    .apply { subscribe(listOf(topic.name)) }
                    .apply(init).asFlow(pollLength)

    fun <K, V> readPartitions(topic: Topic<K, V>, vararg partition: Int, pollLength: Duration = Duration.ofSeconds(4)) =
            kafkaConsumer(topic)
                    .apply { assign(partition.map { TopicPartition(topic.name, it) }) }
                    .asFlow()

    fun <K, V> read(topic: Topic<K, V>) = open(topic)
        .map { it.key() to it.value() }

    suspend fun <K, V> readAll(topic: Topic<K, V>): Map<K, V> {
        val endOffsets = mutableMapOf<Int, Long>()
        return open(topic){
            partitionsFor(topic.name).map {
                TopicPartition(
                    topic.name,
                    it.partition()
                )
            }
                .also { endOffsets.putAll(endOffsets(it).map { it.key.partition() to it.value }) }
                .let { seekToBeginning(it) }
        }.takeWhile { it.offset() < endOffsets[it.partition().toInt()]!!  }
            .map { it.key() to it.value() }
            .toList()
            .toMap()
    }

}