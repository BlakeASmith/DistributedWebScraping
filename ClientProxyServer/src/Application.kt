package csc.distributed

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.PluginLoader
import csc.distributed.webscraper.ResolverLoader
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import csc.distributed.webscraper.definitions.types.JobMetadata
import csc.distributed.webscraper.definitions.types.JobResult
import csc.distributed.webscraper.types.JsoupLoader
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





fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalCoroutinesApi
@FlowPreview
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation){
        gson {  }
    }

    val scrapingClient = Scraper.Client(kafka, PluginLoader(kafka), JsoupLoader)

    val consumerContext = CoroutineScope(newFixedThreadPoolContext(2, "client"))

    val jobs = scrapingClient.jobs
            .map { (_, job) -> job }
            .onEach { println("collected Job ${it.Id}") }
            .produceIn(consumerContext)

    @FlowPreview
    @ExperimentalCoroutinesApi
    suspend fun sendResult(result: JobResult) {
        Scraper.Client.Output(result.jobMetadata.job.Service)
                .writeableBy(kafka)
                .writeFrom(result.results.map { it.url to it })
                .join()
        scrapingClient.completeJob(result.jobMetadata)
    }

    @ExperimentalCoroutinesApi
    val plugins = ResolverLoader {
        Scraper.Plugins(kafka).readableBy { Consumer.UUID(kafka, false) }
                .read().resolver()
    }

    routing {
        get("/") {
            call.respondText("Hello World")
        }

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

