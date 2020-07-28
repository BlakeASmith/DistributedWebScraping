package csc.distributed.webscraper.kafka

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer

@FlowPreview
@ExperimentalCoroutinesApi
class ProducerChannel<K, V>(
        topic: String,
        producerConfig: ProducerConfig,
        context: CoroutineScope = GlobalScope,
        private val channel: Channel<Pair<K, V>> = Channel(),
        private val producer: Producer<K, V> = KafkaProducer(producerConfig.originals())
): SendChannel<Pair<K, V>> by channel{

    constructor(
            topic: String,
            clientId: String,
            bootstraps: List<String>,
            defaultKeySerializer: Serializer<K>,
            defaultValueSerializer: Serializer<V>,
            context: CoroutineScope = GlobalScope
    ): this (topic, ProducerConfig(mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstraps,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to defaultValueSerializer::class.java,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to defaultKeySerializer::class.java
    )), context)

    private val job = channel.consumeAsFlow()
            .onEach { producer.send(ProducerRecord(topic, it.first, it.second)) }
            .onCompletion { producer.close() }
            .launchIn(context)

    override fun close(cause: Throwable?): Boolean {
        job.cancel(CancellationException("channel closed", cause))
        return channel.close(cause)
    }

    suspend fun closeJoin(cause: Throwable? = null): Boolean{
        job.cancelAndJoin()
        return channel.close()
    }
}