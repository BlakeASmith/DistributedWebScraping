package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.Loaders
import csc.distributed.webscraper.plugins.*
import csc.distributed.webscraper.types.Job
import csc.distributed.webscraper.types.JobResult
import csc.distributed.webscraper.types.ScrapingResult
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.*
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.net.ConnectException

suspend fun <R> HttpClient.tryRepeating(_delay: Long, operation: suspend HttpClient.() -> R): R = kotlin.runCatching {
    operation()
}.getOrElse { println(it); if(it !is ConnectException) throw(it) ; delay(_delay); tryRepeating(_delay, operation) }

val SERVER_URL = System.getenv("PROXY_SERVER") ?: "http://0.0.0.0:8080"


suspend fun jobsAsFlow(client: HttpClient) = flow {
    while (true) {
        emit(
            client.tryRepeating(1000) {
                println("getting a job")
                get<Job>("$SERVER_URL/jobs").also {
                    println("job assigned ${it}")
                }
            }
        )
    }
}

val plugins = mutableMapOf<String, Plugin>()

suspend fun HttpClient.sendCompleteJob(result: JobResult) = tryRepeating(100){
    println("sending completed job")
    post<Boolean>("$SERVER_URL/complete") {
        contentType(ContentType.Application.Json)
        body = result
    }
}

suspend fun HttpClient.getPlugin(name: String) = tryRepeating(100){
    plugins.getOrPut(name) {  loadPluginFromByteArray(get<ByteArray>("$SERVER_URL/plugin/$name")) }
}

suspend fun process(job: Job, client: HttpClient): List<ScrapingResult> =
    job.Urls.map { Loaders.Jsoup.runCatching { load(it) }.getOrNull() }.filterNotNull()
        .flatMap { doc -> job.Plugins.map { ScrapingResult(doc.location(), it, client.getPlugin(it).scrape(doc)) } }


suspend fun main(): Unit  {
    val client = HttpClient(Apache) {
        install(JsonFeature){
            serializer = GsonSerializer()
        }
        engine {
            socketTimeout = Int.MAX_VALUE
        }
    }

    jobsAsFlow(client)
        .map { it to  process(it, client) }
        .onEach { (job, results) -> client.sendCompleteJob(JobResult(job, results)) }
        .onEach { println("processed ${it.first.Id}") }
        .collect()
}




