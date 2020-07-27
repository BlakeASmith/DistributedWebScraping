package main

import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.plugins.loadPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.Consumed
import java.io.ByteArrayInputStream
import java.lang.reflect.TypeVariable
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

@ExperimentalCoroutinesApi
fun pluginFlow(kafka: Kafka): Flow<Pair<String, Plugin>> =
        kafka.topic("plugins", Consumed.with(Serdes.String(), Serdes.ByteArray()))
                .map { (name, bytes) ->
                    val tmp = createTempFile(name, ".jar").apply {
                        deleteOnExit()
                    }
                    withContext(Dispatchers.IO){
                        Files.copy(ByteArrayInputStream(bytes), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        name to loadPlugin(JarFile(tmp), "Plugin")
                    }
                }

@ExperimentalCoroutinesApi
class PluginLoader private constructor(
        kafka: Kafka,
        scope: CoroutineScope,
        private val plugins: MutableMap<String, Plugin>
) {

    // pubic access to primary constructor
    constructor(kafka: Kafka, scope: CoroutineScope = GlobalScope): this(kafka, scope, mutableMapOf())

    private val plugChannel = Channel<Pair<String, Plugin>>()

    val loadingRoutine = pluginFlow(kafka)
            .onEach { (name, plug) ->
                plugChannel.send(name to plug)
            }.launchIn(scope)

    private suspend fun receivePlugin() {
            val (plugname, plug) = plugChannel.receive()
            plugins[plugname] = plug
    }

    suspend fun getAsync(name: String): Plugin = plugins[name] ?: suspend {
        receivePlugin()
        getAsync(name)
    }()

    operator fun get(name: String) = runBlocking { getAsync(name) }
}
