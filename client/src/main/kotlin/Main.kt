package csc.distributed.webscraper.clients

import ca.blakeasmith.kkafka.jvm.BootstrapServer
import ca.blakeasmith.kkafka.jvm.KafkaConfig
import ca.blakeasmith.kkafka.jvm.producer
import ca.blakeasmith.kkafka.jvm.sendTo
import csc.distributed.webscraper.plugins.Config
import csc.distributed.webscraper.plugins.ScrapingApplication
import csc.distributed.webscraper.plugins.outputTopicFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
suspend fun main(){
    val app = ScrapingApplication(
            groupId = "clients",
            kafkaConfig = KafkaConfig(
                    Config.get().bootstraps.map {
                        BootstrapServer.fromString(it)
                    }
            )
    )
    val complete = producer(app.kafkaConfig, app.completedJobsTopic)

    app.jobs
            .map { it.value() }
            .onEach { println(it) }
            .map { app.process(it)  }
            .onEach { it.results.sendTo(app.kafkaConfig, outputTopicFor(it.job.Service)) }
            .onEach { complete.send(it.job.Id to it.job) }
            .onCompletion { complete.close() }
            .collect()
}