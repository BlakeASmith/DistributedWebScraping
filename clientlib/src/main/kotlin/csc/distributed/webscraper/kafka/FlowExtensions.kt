package csc.distributed.webscraper.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import org.apache.kafka.common.serialization.Serde
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import services.resolver
import java.time.Duration

data class Topic<K, V>(val name: String, val keySerde: Serde<K>, val valueSerde: Serde<V>){
    @Transient var defaultConfig: Kafka? = null
}

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K, V>.resolver() = this.asFlow().resolver()

@ExperimentalCoroutinesApi
fun <K,V> Topic<K,V>.asFlow(
        recordsPerPoll: Int = this.defaultConfig!!.defaultRecordsPerPoll,
        config: ConsumerConfig = this.defaultConfig!!.consumerConfig(recordsPerPoll = recordsPerPoll),
        pollingTimeout: Duration = this.defaultConfig!!.pollingRate
) = recordsAsFlow().map { it.key() to it.value() }


@ExperimentalCoroutinesApi
fun <K,V> Topic<K,V>.recordsAsFlow(
        recordsPerPoll: Int = this.defaultConfig!!.defaultRecordsPerPoll,
        config: ConsumerConfig = this.defaultConfig!!.consumerConfig(recordsPerPoll = recordsPerPoll),
        pollingTimeout: Duration = this.defaultConfig!!.pollingRate
) = KafkaConsumer<K,V>(config.originals().apply {
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keySerde.deserializer()::class.java)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueSerde.deserializer()::class.java)
})
        .apply { subscribe(listOf(name)) }
        .run {
            generateSequence { poll(pollingTimeout) }.flatMap { it.asSequence() }
                    .asFlow()
                    .onCompletion { close() }
        }

private fun <K,V> Topic<K,V>.pollSequence(
        recordsPerPoll: Int = this.defaultConfig!!.defaultRecordsPerPoll,
        config: ConsumerConfig = this.defaultConfig!!.consumerConfig(recordsPerPoll = recordsPerPoll),
        pollingTimeout: Duration = this.defaultConfig!!.pollingRate
) = KafkaConsumer<K,V>(config.originals().apply {
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keySerde.deserializer()::class.java)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueSerde.deserializer()::class.java)
})
        .apply { subscribe(listOf(name)) }
        .run {
            generateSequence { poll(pollingTimeout) }
        }

@ExperimentalCoroutinesApi
suspend fun <K,V> Topic<K, V>.read(entries: Int) = asFlow(1)
        .take(entries).toList()

@ExperimentalCoroutinesApi
suspend fun <K,V> Topic<K, V>.readAvailable() = pollSequence()
        .dropWhile { it.isEmpty }
        .takeWhile { !it.isEmpty }
        .flatMap { it.asSequence() }
        .map { it.key() to it.value() }


@ExperimentalCoroutinesApi
@FlowPreview
fun <K, V> Topic<K, V>.broadcast(clientId: String, kafka: Kafka, recordsPerPoll: Int? = null) =
        TopicBroadcast(this,  clientId to kafka.defaultGroupId, kafka.bootstraps, kafka.autocommit, kafka.pollingRate, kafka.context, recordsPerPoll ?: kafka.defaultRecordsPerPoll)

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K, V>.broadcast(clientId: String, groupId: String) =
        this.broadcast(clientId, this.defaultConfig!!)


@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K, V>.producer(config: ProducerConfig) = TopicProduction(this, config)

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K, V>.producer(clientId: String? = null) = TopicProduction(this, this.defaultConfig!!.producerConfig())

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K,V>.produceAs(config: ProducerConfig, context: CoroutineScope = GlobalScope,  producer: suspend () -> Pair<K, V>) =
        context.produce<Pair<K, V>>{ producer() }
                .let { TopicProduction(this, config, context, it.broadcast(Channel.UNLIMITED)) }

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Topic<K,V>.produceAs(context: CoroutineScope = GlobalScope, producer: suspend () -> Pair<K, V>): TopicProduction<K, V> =
        this.produceAs(this.defaultConfig!!.producerConfig(), context ,  producer)


@ExperimentalCoroutinesApi
@FlowPreview
fun <K, V> Flow<Pair<K, V>>.produceTo(producer: TopicProduction<K, V>, closeProducer: Boolean = true) =
        this.onEach { producer.sendBlocking(it) }.let {
            if (closeProducer) it.onCompletion { producer.close() }
            else it
        }

@ExperimentalCoroutinesApi
@FlowPreview
fun <K, V> Flow<Pair<K, V>>.produceWithConfirmation(producer: TopicProduction<K, V>, closeProducer: Boolean = true) =
        this.map { it to producer.sendConfirm(it) }.let {
            if (closeProducer) it.onCompletion { producer.close() }
            else it
        }

