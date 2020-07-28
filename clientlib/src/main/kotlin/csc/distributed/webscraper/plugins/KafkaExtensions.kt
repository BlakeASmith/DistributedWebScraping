package csc.distributed.webscraper.plugins
import csc.distributed.webscraper.kafka.ConsumerChannel
import csc.distributed.webscraper.kafka.ProducerChannel
import csc.distributed.webscraper.kafka.TopicChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.*
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.jar.JarFile

data class Topic<K, V>(val name: String, val keySerde: Serde<K>, val valueSerde: Serde<V>)

val PLUGIN_TOPIC = Topic("plugins", Serdes.String(), Serdes.ByteArray())

@FlowPreview
@ExperimentalCoroutinesApi
data class KafkaConfig(
        val bootstraps: List<String>,
        val autocommit: Boolean = true,
        val context: CoroutineScope = GlobalScope,
        val pollingRate: Duration = Duration.ofMillis(100)
){
    fun <K, V> openTopic(
            topic: Topic<K, V>,
            clientIds: Pair<String,String> = "${topic.name}-consumer" to "${topic.name}-producer"
    ) = TopicChannel(
            topic.name,
            "plugin-consumer" to "plugin-producer",
            bootstraps,
            autocommit,
            topic.keySerde,
            topic.valueSerde,
            context,
            pollingRate
    )

    fun <K, V> consumeFrom(topic: Topic<K, V>, clientId: String = "${topic.name}-consumer", groupId: String? = null) = ConsumerChannel<K, V>(
            topic.name,
            clientId to groupId,
            bootstraps,
            topic.keySerde.deserializer()::class.java,
            topic.valueSerde.deserializer()::class.java,
            autocommit,
            pollingRate,
            context
    )

    fun <K, V> produceTo(topic: Topic<K,V>, clientId: String = "${topic.name}-producer") = ProducerChannel<K, V>(
            topic.name,
            clientId,
            bootstraps,
            topic.keySerde.serializer(),
            topic.valueSerde.serializer(),
            context

    )

}

@FlowPreview
@ExperimentalCoroutinesApi
class WebScraper(kafkaConfig: KafkaConfig){

    private val plugMap: MutableMap<String, Plugin> = mutableMapOf()

    private val pluginProduceChannel by lazy {
        kafkaConfig.produceTo(PLUGIN_TOPIC)
    }

    private val pluginReceiveChannel by lazy {
        kafkaConfig.consumeFrom(PLUGIN_TOPIC)
    }

    private val notificationChannel = Channel<Pair<String, Plugin>>()

    private val loadingThread by lazy {
        pluginReceiveChannel.consumeAsFlow()
                .map { (name, bytes) ->
                    val tmp = createTempFile(name, ".jar").apply {
                        deleteOnExit()
                    }
                    withContext(Dispatchers.IO){
                        Files.copy(ByteArrayInputStream(bytes), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        name to loadPlugin(JarFile(tmp), "Plugin")
                    }
                }.onEach { notificationChannel.send(it) }.launchIn(kafkaConfig.context)
    }

    suspend fun registerPlugin(jar: File){
        pluginProduceChannel.send(jar.name to jar.readBytes())
    }

    fun startLoading() = loadingThread
    fun quitLoading() = loadingThread.cancel()
    suspend fun quitLoadingAndJoin() = loadingThread.cancelAndJoin()

    operator fun get(key: String) = runBlocking { getAsync(key) }

    suspend fun getAsync(key: String): Plugin = plugMap[key] ?: suspend {
        val (plugkey, plugin) = notificationChannel.receive()
        plugMap[plugkey] = plugin
        getAsync(key)
    }()

}


