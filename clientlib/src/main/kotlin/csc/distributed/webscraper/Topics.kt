package csc.distributed.webscraper

import ca.blakeasmith.kkafka.jvm.*
import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerde
import csc.distributed.webscraper.definitions.plugins.Plugin
import csc.distributed.webscraper.definitions.types.Job
import csc.distributed.webscraper.definitions.types.JobMetadata
import csc.distributed.webscraper.definitions.types.ScrapingResult
import csc.distributed.webscraper.definitions.types.Service
import csc.distributed.webscraper.types.Loader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.Serdes
import org.jsoup.nodes.Document
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
sealed class Scraper {
    class Services(kafkaConfig: KafkaConfig): WriteableKafkaTopic<String, Service> by Topic("services", Serialization()).writeableBy(kafkaConfig) {
        class Serialization: KeyValueSerde<String, Service>(Serdes.String(), ScraperSerdes.Service())
    }

    class Plugins(kafkaConfig: KafkaConfig): WriteableKafkaTopic<String, ByteArray> by Topic("plugins", Serialization()).writeableBy(kafkaConfig)  {
        class Serialization: KeyValueSerde<String, ByteArray>(Serdes.String(), Serdes.ByteArray())
    }

    @FlowPreview
    class Client(
            val kafkaConfig: KafkaConfig,
            private val plugins: Loader<Plugin>,
            private val pageLoader: Loader<Document>,
            val scope: CoroutineScope = GlobalScope
    ){
        class Output(name: String): KafkaTopic<String, ScrapingResult> by Topic(name, Serialization.Output())
        private lateinit var jobConsumer: KafkaConsumer<String?, Job>
        private lateinit var runtime: Duration
        private lateinit var timeStarted: LocalDateTime
        private lateinit var timeEnded: LocalDateTime

        private val completed = Topic("completed", Serialization.Job()).writeableBy(kafkaConfig)
        private val metadata = Topic("jobs-metadata", Serialization.JobMetadata()).writeableBy(kafkaConfig)

        val jobs by lazy {
            Topic("jobs", Serialization.Job()).readableBy {
                consumer("clients-jobs", kafkaConfig) {
                    autocommit(false)
                    offsetResetStrategy(OffsetResetStrategy.EARLIEST)
                    maxRecordsPerPoll(1)
                }
            }.readWithConsumer { consumer, records ->
                jobConsumer = consumer
                records.map { it.key() to it.value() }
            }
        }


        suspend fun completeJob(job: JobMetadata){
            completed.write(null, job.job)
            jobConsumer.commitAsync()
            println("sending metadata $job")
            metadata.write(null, job)
        }

        @FlowPreview
        suspend fun process(job: Job, onLoadError: (Throwable) -> Document? = {null}) = plugins.run {
            job.Urls.map { pageLoader.runCatching { load(it) }.getOrElse { err -> onLoadError(err) } }.filterNotNull()
                    .flatMap { doc -> job.Plugins.map { ScrapingResult(doc.location(), it, load(it).scrape(doc)) } }
        }

        private lateinit var job: kotlinx.coroutines.Job
        fun start()  = this.apply{
            timeStarted = LocalDateTime.now()
            job = jobs.onEach { println(it) }
                    .map { JobMetadata.Tracker(it.second).begin() }
                    .map { it to process(it.job) }
                    .onEach { (tracker, results) ->
                        repeat(results.size){
                            tracker.produceRecord()
                        }
                        repeat((tracker.job.Urls.size * tracker.job.Plugins.size) - results.size){
                            tracker.couldNotReachSite()
                        }
                    }
                    .onEach { (tracker, results) ->
                        val job = tracker.job
                        results.map { it.url to it }.asFlow()
                                .sendAndReceiveRecords(kafkaConfig, Output(job.Service))
                                .onEach { println("Sent $it to ${job.Service}") }
                                .collect()
                        completeJob(tracker.done())
                    }
                    .onCompletion { completed.close() }
                    .launchIn(scope)
        }

        suspend fun closeJoin() {
            timeEnded = LocalDateTime.now()
            runtime = Duration.ofMillis(ChronoUnit.MILLIS.between(timeStarted, timeEnded))
            println("client ran for $runtime")
            job.cancelAndJoin()
        }

        suspend fun join() {
            job.join()
        }
    }

    class Output<T>(name: String, private val serializer: KSerializer<T>, private val kafkaConfig: KafkaConfig):
            KafkaTopic<String, ScrapingResult> by Topic(name, Serialization.Output())
    {
        private val json by lazy { Json(JsonConfiguration.Stable) }

        fun read(group: String) = this.readableBy { Consumer.committing(group, kafkaConfig) }
                .read()
                .map { it.second }
                .map { (url, plugin, result) -> Triple(url, plugin, json.parse(serializer, result)) }
    }

    class Metadata(kafkaConfig: KafkaConfig): KafkaTopic<String?, JobMetadata> by Topic("jobs-metadata", Serialization.JobMetadata())
}

