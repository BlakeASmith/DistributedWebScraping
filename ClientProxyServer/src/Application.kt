package csc.distributed

import csc.distributed.webscraper.clients.*
import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.kafka.produceTo
import csc.distributed.webscraper.plugins.PluginConsumer
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.request.receive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect

val kafka = Kafka(listOf("127.0.0.1:9092"))

@FlowPreview
@ExperimentalCoroutinesApi
val plugins = PluginConsumer(kafka).asResolver()
@FlowPreview
@ExperimentalCoroutinesApi
val jobs = jobConsumer(kafka)
    .asChannel()

val completeJobsChannel = completeJobProduction(kafka)

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun sendResult(result: JobResult){
    result.results.flatMap { it.value.asIterable() }.map { it.key to it.value }
        .asFlow()
        .produceTo(outputProducer.produceTo(result.job.Service))
        .collect()
    completeJobsChannel.sendConfirm(result.job.Id.toString() to result.job)
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@FlowPreview
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {}
    }

    val client = HttpClient(Apache) {}

    routing {
        get("/job") {
            call.respond(jobs.receive())
        }

        post("/complete") {
            val result = call.receive<JobResult>()
            sendResult(result)
        }

        get("/plugin/{name}"){
            val name = call.parameters["name"]
            call.respond(plugins.get(name!!))
        }
    }
}

