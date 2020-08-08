package csc.distributed.webscraper.types

import ca.blakeasmith.kkafka.jvm.serialization.JsonSerde
import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerde
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.common.serialization.Serdes

@Serializable
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String){
        class Serde: JsonSerde<Job>(serializer(), JsonConfiguration.Stable)
}

@Serializable
data class ScrapingResult(
        val url: String,
        val plugin: String,
        val data: String){
        class Serde: JsonSerde<ScrapingResult>(serializer(), JsonConfiguration.Stable)
}

@Serializable
data class JobResult(val job: Job, val results: List<ScrapingResult>)

@Serializable
data class Service(
        val name: String,
        val rootDomains: List<String>,
        val filters: List<String>,
        val plugins: List<String>
){
        class Serde: JsonSerde<Service>(serializer(), JsonConfiguration.Stable)
}

class PluginSerialization: KeyValueSerde<String, ByteArray>(Serdes.String(), Serdes.ByteArray())
class OutputSerialization: KeyValueSerde<String, ScrapingResult>(Serdes.String(), ScrapingResult.Serde())




