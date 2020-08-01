import com.google.gson.Gson
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.jsoup.Jsoup

/* Json that this class is serialized from is produced from Golang, thus the capitals */
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String)
data class UrlResult(val url: String, val json: String)
val gson = Gson()

val kafka = Kafka(Config.get().bootstraps)

@FlowPreview
@ExperimentalCoroutinesApi
val jobs = Consumer(listOf("jobs"), StringDeserializer::class.java, StringDeserializer::class.java, autocommit = true, groupId = "clients", kafka = kafka)
        .asFlow()
        .map { (_, job, _) ->  gson.fromJson(job, Job::class.java) }

@FlowPreview
@ExperimentalCoroutinesApi
val pluginResolver =
        plugins(kafka).map { (name, bytes) -> jarToPlugin(name, bytes) }.resolver()

val outputProducer = Producer(
        kafka,
        StringSerializer::class.java,
        StringSerializer::class.java
)

// send a job into a special topic to mark it complete TODO
suspend fun sendComplete(results: Job) {
}

@FlowPreview
@ExperimentalCoroutinesApi
val jobProcessorFlow = jobs.map { job ->
        // process each plugin separately
        job to job.Plugins.map { plugin ->
            plugin to pluginResolver.withAsync(plugin) {
                // use Dispatchers.IO since Jsoup.connect is blocking (IO can create extra threads)
                withContext(Dispatchers.IO){
                    // process each url in parallel
                    job.Urls.map { async { kotlin.runCatching { scrape(Jsoup.connect(it).get()) }.getOrNull() } }
                            .mapNotNull { it.await() }
                            .associateWith { plugin }
                }
            }.await()
        }
    }.onEach { println(it.first) }.onEach { sendComplete(it.first) }
    // flatten the results, using the plugin name as the key and a UrlResult as the value
    .map { (job, resultsByPlugin) ->
        job to resultsByPlugin.flatMap { (plugin, results) ->
            results.map { (url, result) ->  plugin to UrlResult(url, result) }
        }
    }
    // write each job's result to it's associated service
    .map { (job, entries) -> job.Service to entries.asFlow() }
    .onEach { (topic, entries) ->
        entries.map { it.first to gson.toJson(it.second) }
                .produceTo(outputProducer.produceTo(topic))
                .launchIn(GlobalScope)
    }.onCompletion{ outputProducer.close() }

@ExperimentalCoroutinesApi
@FlowPreview
suspend fun main(args: Array<String>) = jobProcessorFlow.collect()
