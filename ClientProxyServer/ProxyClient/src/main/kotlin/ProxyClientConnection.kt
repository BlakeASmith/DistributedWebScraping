package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.definitions.plugins.Plugin
import csc.distributed.webscraper.definitions.plugins.loadPluginFromByteArray
import csc.distributed.webscraper.definitions.types.Job
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
                    httpClient.tryRepeating(1000) {
                        println("getting a job")
                        get<Job>("$SERVER_URL/jobs").also {
                            println("job assigned ${it}")
                        }
                    }
            )
        }
    }

    private suspend fun sendCompleteJob(result: JobResult) = httpClient.tryRepeating(100){
        println("sending completed job")
        post<Boolean>("$SERVER_URL/complete") {
            contentType(ContentType.Application.Json)
            body = result
        }
    }

    private suspend fun getPlugin(name: String) = httpClient.tryRepeating(100){
        plugins.getOrPut(name) {  loadPluginFromByteArray(get<ByteArray>("$SERVER_URL/plugin/$name")) }
    }

    suspend fun process(job: Job): List<ScrapingResult> =
            job.Urls.map { jsoup.runCatching { load(it) }.getOrNull() }.filterNotNull()
                    .flatMap { doc -> job.Plugins.map { ScrapingResult(doc.location(), it, getPlugin(it).scrape(doc)) } }


    fun start(scope: CoroutineScope = GlobalScope, transform: Flow<JobResult>.() -> Flow<JobResult> = {this}) = jobs
            .map { it to  process(it) }
            .map { JobResult(it.first, it.second) }
            .transform()
            .onEach { sendCompleteJob(it) }
            .apply { if(verbose) onEach { println("sent results for ${it.job}") } }
            .launchIn(scope)
}


suspend fun <R> HttpClient.tryRepeating(_delay: Long, operation: suspend HttpClient.() -> R): R = kotlin.runCatching {
    operation()
}.getOrElse { println(it); if(it !is ConnectException) throw(it) ; delay(_delay); tryRepeating(_delay, operation) }




