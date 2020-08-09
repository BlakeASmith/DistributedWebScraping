package csc.distributed.webscraper.clients

import ca.blakeasmith.kkafka.jvm.*
import csc.distributed.webscraper.Loaders
import csc.distributed.webscraper.Scraper
import csc.distributed.webscraper.config.Config
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun main(){
    val config = KafkaConfig(
        Config.get().bootstraps.map {
            BootstrapServer.fromString(it)
        }
    )

    val client = Scraper.Client(config, Loaders.Plugin(config), Loaders.Jsoup)

    client.jobs
            .read()
            .map { it.second }
            .onEach { println(it) }
            .map { it to client.process(it)  }
            .onEach { (job, results) ->
                results.map { it.url to it }.asFlow()
                        .sendAndReceiveRecords(config, Scraper.Client.Output(job.Service))
                        .onEach { println("Sent $it to ${job.Service}") }
                        .collect()
                client.completed.write(null, job)
            }
            .onCompletion { client.completed.close() }
            .collect()
}