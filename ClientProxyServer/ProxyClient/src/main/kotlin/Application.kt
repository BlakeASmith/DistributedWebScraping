package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.plugins.Job
import csc.distributed.webscraper.plugins.JobResult
import csc.distributed.webscraper.plugins.Plugin
import csc.distributed.webscraper.plugins.loadPluginFromByteArray
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.*
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jsoup.Jsoup
import java.net.ConnectException

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

suspend fun <R> HttpClient.tryRepeating(_delay: Long, operation: suspend HttpClient.() -> R): R = kotlin.runCatching {
    operation()
}.getOrElse { println(it); if(it !is ConnectException) throw(it) ; delay(_delay); tryRepeating(_delay, operation) }

suspend fun HttpClient.sendCompleteJob(result: JobResult) = tryRepeating(100){
    println("sending completed job ${result.job.Id}")
    post<JobResult>("$SERVER_URL/complete") { body = result }
}

suspend fun HttpClient.getPlugin(name: String) = tryRepeating(100){
    plugins[name] = loadPluginFromByteArray(get<ByteArray>("$SERVER_URL/plugin/$name"))
}

suspend fun main(): Unit  {
    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    jobsAsFlow(client)
            .onEach { println(it) }
            .collect()
}




