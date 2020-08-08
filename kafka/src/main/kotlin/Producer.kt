package ca.blakeasmith.kkafka.jvm

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

class Producer<K, V>(
    val kafka: KafkaConfig,
    val topics: Collection<KafkaTopic<K, V>>,
    private val props: MutableMap<String, Any?>
){
    private val config: ProducerConfig = props.apply {
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topics.first().consumer.keySerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topics.first().consumer.valueSerializer::class.java)
    }.let { ProducerConfig(it) }

    private val producer: KafkaProducer<K, V> by lazy {
        KafkaProducer<K, V>(config.originals())
    }

    class Builder<K, V>(private val kafka: KafkaConfig, private val topics: Collection<KafkaTopic<K, V>>){
        private val props = mutableMapOf<String, Any?>().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstraps.map { it.toString() })
        }

        fun set(prop: String, value: Any) = this.apply{
            props[prop] = value
        }

        fun build() = Producer(kafka, topics , props)
    }

    fun sendFrom(collection: Collection<Pair<K, V>>) = this.apply {
        topics.forEach { topic ->
            collection
                    .map { ProducerRecord(topic.name, it.first, it.second) }
                    .forEach { producer.send(it) }
        }
    }

    private val jobs by lazy { mutableListOf<Job>() }

    fun sendFrom(flow: Flow<Pair<K, V>>, scope: CoroutineScope = GlobalScope) = this.apply{
        jobs.add(
                scope.launch {
                    flow.onEach {
                        topics.forEach {topic ->
                            producer.send(ProducerRecord(topic.name, it.first, it.second))
                        }
                    }.collect()
                }
        )
    }


    @FlowPreview
    fun sendAndReceiveRecordsFrom(flow: Flow<Pair<K, V>>, outChannel: Channel<RecordMetadata>, scope: CoroutineScope = GlobalScope) = this.apply{
        jobs.add(
                scope.launch {
                    flow.flatMapConcat {
                        topics.map {topic ->
                            producer.send(ProducerRecord(topic.name, it.first, it.second)).get()
                        }.asFlow()
                    }.onEach { outChannel.send(it) }
                            .collect()
                }
        )
    }

    fun send(vararg values: Pair<K, V>) = this.apply {
        topics.forEach { topic ->
            values.forEach {
                producer.send(ProducerRecord(topic.name, it.first, it.second))
            }
        }
    }

    suspend fun join() {
        jobs.forEach { it.join() }
        producer.close()
    }

    suspend fun close(){
        for (job in jobs) {
            job.cancelAndJoin()
        }
        producer.close()
    }
}

fun <K, V> producer(kafka: KafkaConfig, vararg topics: KafkaTopic<K, V>, init: Producer.Builder<K, V>.() -> Unit = {})
        = Producer.Builder(kafka, topics.toList())
    .apply(init).build()

@ExperimentalCoroutinesApi
fun <K, V> Flow<Pair<K, V>>.sendTo(
        kafka: KafkaConfig,
        vararg topics: KafkaTopic<K, V>,
        init: Producer.Builder<K, V>.() -> Unit = {}
) = this.apply {
    val producer = producer(kafka, *topics) { init() }
            .sendFrom(this)
    this.onCompletion { producer.close() }
}

@FlowPreview
@ExperimentalCoroutinesApi
fun <K, V> Flow<Pair<K, V>>.sendAndReceiveRecords(
        kafka: KafkaConfig,
        vararg topics: KafkaTopic<K, V>,
        init: Producer.Builder<K, V>.() -> Unit = {}
) = this.run {
    val recordChannel = Channel<RecordMetadata>(Channel.UNLIMITED)
    val producer = producer(kafka, *topics) { init() }
            .sendAndReceiveRecordsFrom(this, recordChannel)
    this.onCompletion { producer.close() }
            .map { recordChannel.receive() }
            .onCompletion { recordChannel.close() }
}

fun <K, V> Collection<Pair<K, V>>.sendTo(
        kafka: KafkaConfig,
        vararg topics: KafkaTopic<K, V>,
        init: Producer.Builder<K, V>.() -> Unit = {}
) = this.apply {
    runBlocking {
        producer(kafka, *topics) { init() }
                .sendFrom(this@apply)
                .close()
    }
}

fun <K, V> Map<K, V>.sendTo(
    kafka: KafkaConfig,
    vararg topics: KafkaTopic<K, V>,
    init: Producer.Builder<K, V>.() -> Unit = {}
) = this.apply {
    runBlocking {
        producer(kafka, *topics) { init() }
                .sendFrom(this@apply.map { it.key to it.value })
                .close()
    }
}



