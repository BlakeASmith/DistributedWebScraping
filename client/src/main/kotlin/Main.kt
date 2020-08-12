package csc.distributed.webscraper.clients

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.PluginLoader
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.types.JsoupLoader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun main(){
    val config = KafkaConfig(
        Config.get().bootstraps.map {
            BootstrapServer.fromString(it)
        }
    )

    val client = Scraper.Client(config, PluginLoader(config), JsoupLoader)

    client.start().join()
}