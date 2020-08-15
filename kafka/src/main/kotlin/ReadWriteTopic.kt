package ca.blakeasmith.kkafka.jvm

import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerialization
import ca.blakeasmith.kkafka.jvm.serialization.StringSerialization
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer

interface WriteableKafkaTopic<K, V>: KafkaTopic<K, V>{
    fun write(k: K, v: V): WriteableKafkaTopic<K, V>
    fun writeFrom(flow: Flow<Pair<K, V>>): WriteableKafkaTopic<K, V>
    fun writeFrom(coll: Collection<Pair<K, V>>): WriteableKafkaTopic<K, V>
    suspend fun join(): Unit
    suspend fun close(): Unit
}

interface ReadableKafakTopic<K ,V>: KafkaTopic<K, V>{
    fun read(): Flow<Pair<K, V>>
    fun open(): Flow<ConsumerRecord<K, V>>
    fun <R> readWithConsumer(op: (KafkaConsumer<K, V>, Flow<ConsumerRecord<K, V>>) -> R): R
}

interface ReadWriteKafkaTopic<K, V>: ReadableKafakTopic<K, V>, WriteableKafkaTopic<K, V>

@ExperimentalCoroutinesApi
class ReadableTopic<K, V> (
        override val name: String,
        override val consumer: KeyValueSerialization<K, V>,
        consumerInit: () -> Consumer
) : ReadableKafakTopic<K, V>{

    private val _consumer by lazy { consumerInit() }
    override fun read(): Flow<Pair<K, V>> = _consumer.read(this)
    override fun open(): Flow<ConsumerRecord<K, V>> = _consumer.open(this)
    override fun <R> readWithConsumer(op: (KafkaConsumer<K, V>, Flow<ConsumerRecord<K, V>>) -> R): R= _consumer.readWithConsumer(this, op)
}

@ExperimentalCoroutinesApi
class WriteableTopic<K, V> (
        override val name: String,
        override val consumer: KeyValueSerialization<K, V>,
        producerInit: () -> Producer<K, V>
): KafkaTopic<K, V>, WriteableKafkaTopic<K, V>{
    private val producer by lazy { producerInit() }

    override fun write(k: K, v: V) = this.apply {
        producer.send(k to v)
    }

    override fun writeFrom(flow: Flow<Pair<K, V>>) = this.apply {
        producer.sendFrom(flow)
    }

    override fun writeFrom(coll: Collection<Pair<K, V>>) = this.apply {
        producer.sendFrom(coll)
    }

    override suspend fun close() = producer.close()
    override suspend fun join() = producer.join()
}

@ExperimentalCoroutinesApi
class ReadWriteTopic<K, V>(
        override val name: String,
        override val consumer: KeyValueSerialization<K, V>,
        producerInit: () -> Producer<K, V>,
        consumerInit: () -> Consumer
): ReadableKafakTopic<K, V> by ReadableTopic(name, consumer, consumerInit),
        WriteableKafkaTopic<K, V> by WriteableTopic(name, consumer, producerInit), ReadWriteKafkaTopic<K, V>

@ExperimentalCoroutinesApi
fun <K, V> KafkaTopic<K, V>.writeableBy(kafkaConfig: KafkaConfig, producerInit: Producer.Builder<K, V>.() -> Unit = {}) = WriteableTopic(name, consumer) { producer(kafkaConfig, this, init = producerInit) }

@ExperimentalCoroutinesApi
fun <K, V> KafkaTopic<K, V>.readableBy(consumerInit: () -> Consumer) = ReadableTopic(name, consumer, consumerInit)


@ExperimentalCoroutinesApi
fun <K, V> WriteableKafkaTopic<K, V>.readableBy(consumerInit: () -> Consumer): ReadWriteKafkaTopic<K, V> =
        object: ReadWriteKafkaTopic<K, V>,
                WriteableKafkaTopic<K, V> by this,
                ReadableKafakTopic<K, V> by ReadableTopic(name, consumer, consumerInit){
            override val consumer: KeyValueSerialization<K, V> = this@readableBy.consumer
            override val name: String = this@readableBy.name
        }



