package csc.distributed.webscraper

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.plugins.loadPluginFromByteArray
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.jsoup.nodes.Document
import java.util.*

interface Loader<T>{
    fun load(key: String): T
}

fun <T> loader(load: (String) -> T) = object : Loader<T> {
    override fun load(key: String): T = load(key)
}

object Loaders{
    object Jsoup: PageLoader {
        override fun load(url: String): Document = org.jsoup.Jsoup.connect(url).get()
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    class Plugin(kafkaConfig: KafkaConfig): ResolverLoader<csc.distributed.webscraper.plugins.Plugin>({
        consumer(UUID.randomUUID().toString(), kafkaConfig){
            autocommit(false)
            set(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, Int.MAX_VALUE)
        }
                .read(Scraper.Plugins(kafkaConfig))
                .map { it.first to loadPluginFromByteArray(it.second) }
                .resolver()
    })
}

@FlowPreview
@ExperimentalCoroutinesApi
open class ResolverLoader<V>(flowResolver: () -> FlowResolver<String, V, Pair<String, V>>): Loader<V> {
    private val _resolver by lazy(flowResolver)
    override fun load(key: String): V = _resolver.get(key)
}
