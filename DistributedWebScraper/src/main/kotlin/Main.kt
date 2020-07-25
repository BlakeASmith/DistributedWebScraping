import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.util.*
import java.util.jar.JarFile

const val BOOTSTRAP_CONFIG_ENV = "BOOTSTRAP_CONFIG"
const val DEFAULT_BOOTSTRAP_CONFIG =  "127.0.0.1:9092"
const val APPLICATION_ID_CONFIG_ENV = "APPLICATION_ID_CONFIG"
const val DEFAULT_APPLICATION_ID = "rover-client"

const val PLUGIN_DIR = "./plugins"
val routing = RoutingService(Configuration.routingServiceAddress)
val plugins : MutableMap<String, RoverPlugin> = File(PLUGIN_DIR).apply {
    if (!exists()) {
        createNewFile()
        mkdir()
    }
}.walk().drop(1)
        .filter { it.extension == "jar" }
        .map { JarFile(it) }
        .associateBy { it.name }
        .mapValues { (_, jar) -> loadPlugin(jar) }
        .toMutableMap()


val props = Properties().also {
    it[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = System.getenv(BOOTSTRAP_CONFIG_ENV) ?: DEFAULT_BOOTSTRAP_CONFIG.also {
        println("running with bootstrap server $DEFAULT_BOOTSTRAP_CONFIG")
        println("to change bootstrap server set the $BOOTSTRAP_CONFIG_ENV environment variable")
    }
    it[StreamsConfig.APPLICATION_ID_CONFIG] = System.getenv(APPLICATION_ID_CONFIG_ENV) ?: DEFAULT_APPLICATION_ID.also {
        println("using application id $DEFAULT_APPLICATION_ID, to change this set the $APPLICATION_ID_CONFIG_ENV environment variable")
    }
    // use strings by default for keys and values
    it[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
    it[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
}

fun <R> withPlugin(plugin: String, action: RoverPlugin.() -> R): R =
    plugins.getOrElse(plugin) { routing.getPlugin(plugin, PLUGIN_DIR).also { plugins[plugin] = it } }.action()

suspend fun main() {
    val sb = StreamsBuilder()
    sb.stream<String, String>("url")
            .map { plugin, url -> println("running $plugin on $url")
                KeyValue(plugin, runCatching { Jsoup.connect(url).get() }.getOrNull()) }
            .filter { _, doc -> (doc != null).also { if (!it) println("filtered out a url") } }
            .map { plugin, doc -> withPlugin(plugin) { KeyValue(doc!!.location(), scrape(doc)) }  }
            .to("json")


    sb.stream<String, String>("json")
            .foreach { key, value -> println("$key, ${value.take(100)}") }

    KafkaStreams(sb.build(), props)
            .apply { cleanUp() }
            .start()

}

suspend fun grpc_master_loop() {
    val channel = routing.connectMaster()
    val dbChannel = routing.connectDb()
    val master = MasterServiceConnection(channel)
    val database = DatabaseServiceConnection(dbChannel)

    while(true) {
        println("requesting a job")
        val job = master.requestWork(jobRequest(1))
        val plugs = job.pluginsList
                .map { plugins.getOrElse(it) {
                    routing.getPlugin(it, PLUGIN_DIR)
                } }
        job.urlsList
                .map { kotlin.runCatching {  URL(it) }.getOrNull()  }.asFlow()
                .filterNotNull()
                .scrape { doc ->  plugs.map { it.scrape(doc) } }
                .flatMapConcat { (_, results) -> results.asFlow() }
                .let { Db.JsonObjects.newBuilder().addAllText(it.toList()).setType("WORDCOUNT").build() }
                .also { database.store(it) }
        master.completeWork(jobResult(job, true))
    }
}

