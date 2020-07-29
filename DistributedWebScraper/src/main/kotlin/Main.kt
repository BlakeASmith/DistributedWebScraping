import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.plugins.KafkaConfig
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.plugins.WebScraper
import kotlinx.coroutines.*
import main.PluginLoader
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import java.util.*

const val BOOTSTRAP_CONFIG_ENV = "BOOTSTRAP_CONFIG"
const val DEFAULT_BOOTSTRAP_CONFIG =  "127.0.0.1:9092"
const val APPLICATION_ID_CONFIG_ENV = "APPLICATION_ID_CONFIG"
const val DEFAULT_APPLICATION_ID = "rover-client"

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
    it["group.id"] = 1
    it["client.id"] = "client"
    it["enable.auto.commit"] = true
}

@ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    val webScraper = WebScraper(KafkaConfig(listOf("127.0.0.1"), false))
    webScraper.startLoading()
    print(webScraper["test"])
}

