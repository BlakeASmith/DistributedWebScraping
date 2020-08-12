package csc.distributed.webscraper

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.definitions.plugins.Plugin
import csc.distributed.webscraper.definitions.plugins.loadPluginFromByteArray
import csc.distributed.webscraper.types.Loader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*



@ExperimentalCoroutinesApi
@FlowPreview
class PluginLoader(kafkaConfig: KafkaConfig): ResolverLoader<Plugin>({
    consumer(UUID.randomUUID().toString(), kafkaConfig){
        autocommit(false)
        set(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, Int.MAX_VALUE)
    }
            .read(Scraper.Plugins(kafkaConfig))
            .map { it.first to loadPluginFromByteArray(it.second) }
            .resolver()
})

@FlowPreview
@ExperimentalCoroutinesApi
open class ResolverLoader<V>(flowResolver: () -> FlowResolver<String, V, Pair<String, V>>): Loader<V> {
    private val _resolver by lazy(flowResolver)
    override fun load(key: String): V = _resolver.get(key)
}
