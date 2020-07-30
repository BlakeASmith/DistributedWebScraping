import com.google.gson.Gson
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.PLUGIN_TOPIC
import csc.distributed.webscraper.plugins.pluginResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.common.serialization.Serdes
import org.jsoup.Jsoup

/* Json that this class is serialized from is produced from Golang, thus the capitals */
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String)

data class UrlResult(val url: String, val json: String)

val gson = Gson()

val jobsTopic = Topic("jobs", Serdes.String(), Serdes.String()).apply {
    defaultConfig = Kafka("client", Config.get().bootstraps, autocommit = true, defaultRecordsPerPoll = 1)
}
@ExperimentalCoroutinesApi
val jobs = jobsTopic.asFlow()
        .map { gson.fromJson(it.second, Job::class.java) }

// send a job into a special topic to mark it complete TODO
suspend fun sendComplete(results: Job) {
}

val producerDefault = Kafka("plugin-producer", Config.get().bootstraps).producerConfig()

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
    .map { (job, entries) -> Topic(job.Service, Serdes.String(), Serdes.String()) to entries.asFlow() }
    .onEach { (topic, entries) ->
        entries.map { it.first to gson.toJson(it.second) }
                .produceTo(topic.producer(config = producerDefault))
                .launchIn(GlobalScope)
    }

@ExperimentalCoroutinesApi
@FlowPreview
suspend fun main(args: Array<String>){
    PLUGIN_TOPIC.defaultConfig = Kafka(
            "plugin-consumer",
            Config.get().bootstraps,
            autocommit = false
    )

    jobProcessorFlow.launchIn(GlobalScope)

    listOf("test1").map {
        withContext(Dispatchers.IO){
            launch {
                Topic("test", Serdes.String(), Serdes.String())
                        .apply {
                            defaultConfig = PLUGIN_TOPIC.defaultConfig
                        }
                        .asFlow(config = PLUGIN_TOPIC.defaultConfig!!.consumerConfig(groupId = "test"))
                        .collectIndexed { index, value -> if(index % 50 == 0) println("$index $value") }
            }
        }
    }.forEach { it.join() }

}