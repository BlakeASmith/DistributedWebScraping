package csc.distributed.webscraper.plugins

import ca.blakeasmith.kkafka.jvm.*
import ca.blakeasmith.kkafka.jvm.serialization.JsonSerde
import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerde
import csc.distributed.webscraper.services.Service
import csc.distributed.webscraper.utils.withDeferred
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Serializable
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String)

data class JobResult(val job: Job, val results: Map<UrlWithPlugin, String>)

@Serializable
data class UrlWithPlugin(val url: String, val plugin: String)

const val CONFIG_BOOTSTRAPS = "WEBSCRAPER_BOOTSTRAPS"

data class Config(
        val bootstraps: List<String>
) {
    companion object {
        fun get() = Config(System.getenv(CONFIG_BOOTSTRAPS).split(','))
    }
}

interface PageLoader{
    fun load(url: String): Document
}

object JsoupPageLoader: PageLoader{
    override fun load(url: String): Document = Jsoup.connect(url).get()
}

class ServiceSerde: JsonSerde<Service>(Service.serializer(), JsonConfiguration.Stable)
class JobSerde: JsonSerde<Job>(Job.serializer(), JsonConfiguration.Stable)
class JsonIntSerde: JsonSerde<Int>(Int.serializer(), JsonConfiguration.Stable)
class UrlWithPluginSerde: JsonSerde<UrlWithPlugin>(UrlWithPlugin.serializer(), JsonConfiguration.Stable)

class ServiceSerialization: KeyValueSerde<String, Service>(Serdes.String(), ServiceSerde())
class JobSerialization: KeyValueSerde<Int, Job>(JsonIntSerde(), JobSerde())
class PluginSerialation: KeyValueSerde<String, ByteArray>(Serdes.String(), Serdes.ByteArray())
class OutputSerialization: KeyValueSerde<UrlWithPlugin, String>(UrlWithPluginSerde(), Serdes.String())

@ExperimentalCoroutinesApi
class ScrapingApplication(
        val groupId: String,
        val kafkaConfig: KafkaConfig,
        scope: CoroutineScope = GlobalScope,
        private val pageLoader: PageLoader = JsoupPageLoader
){
    val servicesTopic = Topic("services", ServiceSerialization())
    val pluginsTopic = Topic("plugins", PluginSerialation())
    val jobsTopic = Topic("jobs", JobSerialization())
    val completedJobsTopic = Topic("completed", JobSerialization())

    val services by lazy {
        scope.async {
            Consumer.nonCommitting("$groupId-services", kafkaConfig)
                    .readAll(servicesTopic)
        }
    }

    @FlowPreview
    val plugins by lazy {
            Consumer.UUID(kafkaConfig, false)
                    .open(pluginsTopic)
                    .map { it.key() to loadPluginFromByteArray(it.value()) }
                    .resolver()
    }

    val jobs by lazy {
        consumer("$groupId-jobs", kafkaConfig){
            autocommit(true)
            offsetResetStrategy(OffsetResetStrategy.EARLIEST)
            maxRecordsPerPoll(1)
        }.open(jobsTopic)
    }


    @FlowPreview
    suspend fun process(job: Job, onLoadError: (Throwable) -> Document? = {null}) = plugins.run {
        job.Urls.map {
            pageLoader.runCatching { load(it) }
                    .getOrElse { err -> onLoadError(err) }
        }.filterNotNull()
                .flatMap { doc ->
                    job.Plugins.map {
                        (doc.location() to it) to withAsync(it) { scrape(doc) }
                    }
                }.toMap()
                .mapValues { it.value.await() } }
                .mapKeys { UrlWithPlugin(it.key.first, it.key.second) }
                .let { JobResult(job, it) }
}

fun outputTopicFor(service: Service) = Topic(service.name, OutputSerialization())
fun <V> outputTopicFor(service: Service, serializer: KSerializer<V>) =
        Topic(service.name, OutputSerialization())

fun outputTopicFor(service: String) = Topic(service, OutputSerialization())
fun <V> outputTopicFor(service: String, serializer: KSerializer<V>) =
        Topic(service, OutputSerialization())




