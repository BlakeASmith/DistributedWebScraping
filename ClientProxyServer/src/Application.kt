package csc.distributed

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.Loaders
import csc.distributed.webscraper.ResolverLoader
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.types.JobResult
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

val kafka = KafkaConfig(Config.get().bootstraps.map { BootstrapServer.fromString(it) }).also {
    println("initialized Kafka Config $it")
}

@FlowPreview
@ExperimentalCoroutinesApi
val scrapingClient = Scraper.Client(kafka, Loaders.Plugin(kafka), Loaders.Jsoup)

@ObsoleteCoroutinesApi
val consumerContext = CoroutineScope(newFixedThreadPoolContext(2, "client"))

@ObsoleteCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
val jobs = scrapingClient.jobs
    .read()
    .map { (_, job) -> job }
    .onEach { println("collected Job ${it.Id}") }
    .produceIn(consumerContext)

@FlowPreview
@ExperimentalCoroutinesApi
val plugins = ResolverLoader {
    Scraper.Plugins(kafka).readableBy { Consumer.UUID(kafka, false) }
        .read().resolver()
}


@FlowPreview
@ExperimentalCoroutinesApi
suspend fun sendResult(result: JobResult) {
    scrapingClient.completed.write(null, result.job)
}


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@FlowPreview
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation){
        gson {  }
    }

    routing {
        get("/jobs") {
            call.application.log.debug("received job request")
            call.respond(jobs.receive())
        }

        post("/complete") {
            val result = call.receive<JobResult>()
            sendResult(result)
            call.respond(true)
        }

        get("/plugin/{name}"){
            val name = call.parameters["name"]
            println("fetching plugin $name")
            call.respondBytes(plugins.load(name!!))
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        println("Proxy server up")
    }

    environment.monitor.subscribe(ApplicationStopped) {
        println("Proxy server down")
    }
}

