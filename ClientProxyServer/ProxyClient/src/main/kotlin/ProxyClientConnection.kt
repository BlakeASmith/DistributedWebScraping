package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.definitions.plugins.Plugin
import csc.distributed.webscraper.definitions.plugins.loadPluginFromByteArray
import csc.distributed.webscraper.definitions.types.Job
import csc.distributed.webscraper.definitions.types.JobMetadata
import csc.distributed.webscraper.definitions.types.JobResult
import csc.distributed.webscraper.definitions.types.ScrapingResult
import csc.distributed.webscraper.types.Loader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.jsoup.nodes.Document
import java.net.ConnectException

@ExperimentalCoroutinesApi
class ProxyClient(val httpClient: HttpClient,
                  val serverAddr: String,
                  private val jsoup: Loader<Document>,
                  private val verbose: Boolean
) {
    private val plugins = mutableMapOf<String, Plugin>()
    private val jobs: Flow<Job> = flow {
        while (true) {
            emit(
                    httpClient.tryRepeating(1000, errors) {
                        println("getting a job")
                        get<Job>("$serverAddr/jobs").also {
                            println("job assigned ${it}")
                        }
                    }
            )
        }
    }

    val errors = Channel<Throwable>(Channel.UNLIMITED)

    private suspend fun sendCompleteJob(result: JobResult) = httpClient.tryRepeating(100, errors){
        println("sending completed job")
        post<Boolean>("$serverAddr/complete") {
            contentType(ContentType.Application.Json)
            body = result
        }
    }

    private suspend fun getPlugin(name: String) = httpClient.tryRepeating(100, errors){
        plugins.getOrPut(name) {  loadPluginFromByteArray(get<ByteArray>("$serverAddr/plugin/$name")) }
    }

    suspend fun process(job: Job): List<ScrapingResult> =
            job.Urls.map { url -> jsoup.runCatching { load(url) }.getOrElse {
                jsoup
                        .runCatching {
                            load(url.replace("http", "https"))
                        }.getOrNull()
            } }.filterNotNull()
                    .flatMap { doc -> job.Plugins.map { ScrapingResult(doc.location(), it, getPlugin(it).scrape(doc)) } }


    fun start(
            scope: CoroutineScope = GlobalScope,
            transform: Flow<JobResult>.() -> Flow<JobResult> = {this},
            before: Flow<Job>.() -> Flow<Job> = {this}
    ) = jobs
            .before()
            .map { JobMetadata.Tracker(it).begin() }
            .map { it to  process(it.job) }
            .map { (tracker, results) ->
                repeat(tracker.job.Urls.size * tracker.job.Plugins.size){
                    tracker.produceRecord()
                }
                repeat((tracker.job.Urls.size * tracker.job.Plugins.size) - results.size){
                    tracker.couldNotReachSite()
                }
                JobResult(tracker.done(), results)
            }
            .transform()
            .onEach { sendCompleteJob(it) }
            .apply { if(verbose) onEach { println("sent results  ${it.jobMetadata}") } }
            .launchIn(scope)
}


suspend fun <R> HttpClient.tryRepeating(
        _delay: Long,
        errorCh: SendChannel<Throwable>,
        operation: suspend HttpClient.() -> R): R = kotlin.runCatching {
    operation()
}.getOrElse { errorCh.send(it); if(it !is ConnectException) throw(it) ; delay(_delay); tryRepeating(_delay, errorCh, operation) }




