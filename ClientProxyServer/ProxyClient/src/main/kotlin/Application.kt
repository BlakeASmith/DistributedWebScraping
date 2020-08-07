package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.clients.*
import csc.distributed.webscraper.clients.Job
import csc.distributed.webscraper.plugins.jarToPlugin
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jsoup.Jsoup
import java.net.ConnectException

val SERVER_URL = "http://proxy:8080"

suspend fun jobsAsFlow(client: HttpClient) = flow {
    while (true) {
        emit(
            client.tryRepeating(500) {
                println("getting a job")
                get<Job>("$SERVER_URL/jobs").also {
                    println("job assigned ${it.Id}")
                }
            }
        )
    }
}

suspend fun <R> HttpClient.tryRepeating(delay: Long, operation: suspend HttpClient.() -> R): R = kotlin.runCatching {
    operation()
}.getOrElse { if(it !is ConnectException) throw(it) ; delay(delay); tryRepeating(delay, operation) }

suspend fun HttpClient.sendCompleteJob(result: JobResult) = tryRepeating(100){
    println("sending completed job ${result.job.Id}")
    post<JobResult>("$SERVER_URL/complete") { body = result }
}

suspend fun HttpClient.getPlugin(name: String) = tryRepeating(100){
    println("trying to get plugin $name")
    jarToPlugin(name, get<ByteArray>("$SERVER_URL/plugin/$name")).second
}

suspend fun main(): Unit  {
    val client = HttpClient() {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    jobsAsFlow(client)
        .onEach { println("received $it") }
        .map { job ->
        // process each plugin separately
        job to job.Plugins.map { plugin -> plugin to GlobalScope.async { client.getPlugin(plugin) } }
            .map { (name, loaded) -> name to loaded.await() }
            .map { (name, plug) ->
                name to withContext(Dispatchers.IO){
                    // process each url in parallel
                    job.Urls.map { async { kotlin.runCatching { plug.scrape(Jsoup.connect(it).get()) }.getOrNull() } }
                        .mapNotNull { it.await() }
                        .associateWith { name }
                }
            }.toMap()
    }.map { (job, results) -> JobResult(job, results) }
        .onEach { client.sendCompleteJob(it) }
        .onEach { println(it.job) }
        .collect()
}




