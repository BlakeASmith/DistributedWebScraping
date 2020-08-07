package csc.distributed

import csc.distributed.webscraper.clients.*
import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.webscraper.kafka.produceTo
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.PluginConsumer
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.request.receive
import kotlinx.coroutines.*

val kafka = Kafka(Config.get().bootstraps, CoroutineScope(newFixedThreadPoolContext(5, "jobs"))).also {
    println("initialized Kafka Config $it")
}


@FlowPreview
@ExperimentalCoroutinesApi
val plugins by lazy {
    PluginConsumer(kafka).asResolver().also {
        println("initialized plugin resolver")
    }
}

@FlowPreview
@ExperimentalCoroutinesApi
val jobs by lazy {
    jobConsumer(kafka)
            .asChannel().also {
                println("initialized jobs channel")
            }

}
val completeJobsChannel by lazy {
    completeJobProduction(kafka).also {
        println("initialized complete jobs production")
    }
}

/*
@FlowPreview
@ExperimentalCoroutinesApi
suspend fun sendResult(result: JobResult){
    println("sent completed result for ${result.job.Id}")
    result.results.flatMap { it.value.asIterable() }.map { it.key to it.value }
        .asFlow()
        .produceTo(outputProducer.produceTo(result.job.Service))
        .collect()
    completeJobsChannel.sendConfirm(result.job.Id.toString() to result.job)
}

 */


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@FlowPreview
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {}
    }

    routing {
        get("/jobs") {
            println("received job request")
            val job = jobs.receive()
            println("serving job ${job.key}")
            call.respond(job.value)

        }

        post("/complete") {
            val result = call.receive<JobResult>()
            //sendResult(result)
            call.respond(true)
        }

        get("/plugin/{name}"){
            val name = call.parameters["name"]
            println("fetching plugin $name")
            call.respond(plugins.get(name!!))
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        println("Proxy server up")
    }

    environment.monitor.subscribe(ApplicationStopped) {
        println("Proxy server down")
    }
}

