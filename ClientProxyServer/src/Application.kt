package csc.distributed

import ca.blakeasmith.kkafka.jvm.BootstrapServer
import ca.blakeasmith.kkafka.jvm.KafkaConfig
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.Job
import csc.distributed.webscraper.plugins.JobResult
import csc.distributed.webscraper.plugins.ScrapingApplication
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.request.receive
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@FlowPreview
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {}
    }

    val consumerContext = CoroutineScope(newFixedThreadPoolContext(1, "jobs"))

    val kafka = KafkaConfig(Config.get().bootstraps.map { BootstrapServer.fromString(it) }).also {
        println("initialized Kafka Config $it")
    }

    @ExperimentalCoroutinesApi
    val webscraper = ScrapingApplication("clients", kafka, consumerContext)

    val jobs = webscraper.jobs
            .map { it.value() }
            .onEach { println("collected Job ${it.Id}") }
            .produceIn(consumerContext)


    routing {
        get("/jobs") {
            call.application.log.debug("received job request")
            call.respond(jobs.receive())
        }

        post("/complete") {
            val result = call.receive<JobResult>()
            //sendResult(result)
            call.respond(true)
        }

        get("/plugin/{name}"){
            val name = call.parameters["name"]
            println("fetching plugin $name")
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        println("Proxy server up")
    }

    environment.monitor.subscribe(ApplicationStopped) {
        println("Proxy server down")
    }
}

