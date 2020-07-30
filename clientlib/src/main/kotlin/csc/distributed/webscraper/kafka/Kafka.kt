package csc.distributed.webscraper.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration

data class Kafka(
        val defaultGroupId: String,
        val bootstraps: List<String> = listOf("127.0.0.1:9092"),
        val autocommit: Boolean = true,
        val context: CoroutineScope = GlobalScope,
        val pollingRate: Duration = Duration.ofMillis(100),
        val defaultRecordsPerPoll: Int = 10
){
    fun consumerConfig(
            clientId: String? = null,
            groupId: String = defaultGroupId,
            defaultKeySerializer: Class<out Deserializer<*>> = StringDeserializer::class.java,
            defaultValueSerializer: Class<out Deserializer<*>> = StringDeserializer::class.java,
            offsetResetConfig: AutoOffsetConfig = AutoOffsetConfig.earliest,
            recordsPerPoll: Int = defaultRecordsPerPoll
    ) = mutableMapOf<String, Any>().apply {
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,   autocommit)
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstraps)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, defaultValueSerializer)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, defaultKeySerializer)
        clientId?.let { put(ConsumerConfig.CLIENT_ID_CONFIG, it) }
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetConfig.name)
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, recordsPerPoll)
    }.let { ConsumerConfig(it) }

    fun producerConfig(
            clientId: String? = null,
            groupId: String = defaultGroupId,
            defaultKeySerializer: Class<out Serializer<*>> = StringSerializer::class.java,
            defaultValueSerializer: Class<out Serializer<*>> = StringSerializer::class.java
    ) = mutableMapOf<String, Any>(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to defaultValueSerializer,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to defaultKeySerializer,
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstraps
            ).apply { clientId?.let { put(ProducerConfig.CLIENT_ID_CONFIG, it) } }
            .let { ProducerConfig(it) }
}