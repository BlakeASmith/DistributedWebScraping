package csc.distributed.webscraper.kafka

import csc.distributed.BuildConfig
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.plugins.loadPlugin
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import csc.distributed.webscraper.services.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarFile

fun <K, V> Pair<K, V>.keyvalue() = KeyValue(first, second)


class Kafka(private val id: String, val props: Properties = Properties().also {
    it[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = BuildConfig.bootstraps
    it[StreamsConfig.APPLICATION_ID_CONFIG] = id
    // use strings by default for keys and values
    it[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
    it[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
}): StreamsBuilder() {
    private lateinit var streams: KafkaStreams

    constructor(id: String, before: Kafka.() -> Unit): this(id){
        apply(before)
    }

    operator fun invoke() =
        KafkaStreams(build(), props).apply {
            streams = this
            println("set streams")
            cleanUp()
            start()
            localThreadsMetadata().forEach(::println)
            Runtime.getRuntime().addShutdownHook(Thread(::close))
        }


    fun close() = streams.close()

    fun serviceProducer(name: String = id) = KafkaProducer<String, Service>(Properties().apply {
        putAll(props)
        put("client.id", name)
        put("key.serializer", StringSerializer::class.java.name)
        put("value.serializer", ServiceSerializer::class.java)
    })

    @ExperimentalCoroutinesApi
    fun <K, V>topic(topic: String, serdes: Consumed<K, V>): Flow<Pair<K,V>> = channelFlow {
        this@Kafka {
           stream(topic, serdes).foreach{ key, value ->
               println("received $key")
                runBlocking { send(key to value) }
           }
        }
        awaitClose()
    }

    fun <K, V> topicChannel(topic: String, serdes: Consumed<K,V>, scope: CoroutineScope = GlobalScope) =
            Channel<Pair<K,V>>()
            .apply {
                scope.launch {
                    this@Kafka {
                        stream(topic, serdes).foreach { k, v ->
                            sendBlocking(k to v)
                        }
                    }
                }
            }

    fun <K, V> producer(keySerializer: Class<out Serializer<K>>, valueSerializer: Class<out Serializer<V>>) = KafkaProducer<K, V>(Properties().apply {
        putAll(props)
        put("client.id", id)
        put("key.serializer", keySerializer)
        put("value.serializer", valueSerializer)
    })

    fun pushToTopic(topic: String, key: String, value: ByteArray) =
        pushToTopic(topic, key, value, ByteArraySerializer::class.java)

    fun <V> pushToTopic(topic: String, key: String, value: V, valueSerializer: Class<out Serializer<V>>) = this.apply {
        val producer = producer(StringSerializer::class.java, valueSerializer)
        producer.send(ProducerRecord(topic, key, value))
                .get().also(::println)
        producer.close()
    }

    suspend operator fun invoke (before: suspend Kafka.() -> Unit) = this.apply {
        before()
        invoke()
    }
    fun start() = invoke()
}