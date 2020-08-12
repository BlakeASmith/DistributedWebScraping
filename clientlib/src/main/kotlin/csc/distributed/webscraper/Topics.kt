package csc.distributed.webscraper

import ca.blakeasmith.kkafka.jvm.*
import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerde
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.types.Job
import csc.distributed.webscraper.types.ScrapingResult
import csc.distributed.webscraper.types.Service
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Serdes
import org.jsoup.nodes.Document

@ExperimentalCoroutinesApi
sealed class Scraper {
    class Services(kafkaConfig: KafkaConfig): WriteableKafkaTopic<String, Service> by Topic("services", Serialization()).writeableBy(kafkaConfig) {
        class Serialization: KeyValueSerde<String, Service>(Serdes.String(), Service.Serde())
    }

    class Plugins(kafkaConfig: KafkaConfig): WriteableKafkaTopic<String, ByteArray> by Topic("plugins", Serialization()).writeableBy(kafkaConfig)  {
        class Serialization: KeyValueSerde<String, ByteArray>(Serdes.String(), Serdes.ByteArray())
    }

    class Client(val kafkaConfig: KafkaConfig, private val plugins: Loader<Plugin>, private val pageLoader: Loader<Document>){
        class Serialization: KeyValueSerde<String?, Job>(Serdes.String(), Job.Serde())

        val jobs = Topic("jobs", Serialization()).readableBy {
            consumer("clients-jobs", kafkaConfig) {
                autocommit(true)
                offsetResetStrategy(OffsetResetStrategy.EARLIEST)
                maxRecordsPerPoll(1)
            }
        }

        val completed = Topic("completed", Serialization()).writeableBy(kafkaConfig)

        @FlowPreview
        suspend fun process(job: Job, onLoadError: (Throwable) -> Document? = {null}) = plugins.run {
            job.Urls.map { pageLoader.runCatching { load(it) }.getOrElse { err -> onLoadError(err) } }.filterNotNull()
                    .flatMap { doc -> job.Plugins.map { ScrapingResult(doc.location(), it, load(it).scrape(doc)) } }
        }

        class Output(name: String):
                KafkaTopic<String, ScrapingResult> by Topic(name, Serialization())
        {
            class Serialization: KeyValueSerde<String, ScrapingResult>(Serdes.String(), ScrapingResult.Serde())
        }
    }

    class Output<T>(name: String, private val serializer: KSerializer<T>, private val kafkaConfig: KafkaConfig):
            KafkaTopic<String, ScrapingResult> by Topic(name, Serialization())
    {
        class Serialization: KeyValueSerde<String, ScrapingResult>(Serdes.String(), ScrapingResult.Serde())

        private val json by lazy { Json(JsonConfiguration.Stable) }

        fun read(group: String) = this.readableBy { Consumer.committing(group, kafkaConfig) }
                .read()
                .map { it.second }
                .map { (url, plugin, result) -> Triple(url, plugin, json.parse(serializer, result)) }
    }
}

