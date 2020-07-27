package csc.distributed.webscraper.services

import csc.distributed.webscraper.kafka.Kafka
import csc.distributed.BuildConfig
import org.apache.kafka.streams.kstream.KStream
import java.io.Serializable

data class Service(
    val name: String,
    val rootDomains: List<String>,
    val filters: List<String>,
    val destination: Pair<String, Int> = "127.0.0.1" to 9696,
    val plugins: List<String>
) : Serializable {
    @Transient
    private var kafka: Kafka? = null
    private var registered = false

    private fun getKafka() = kafka ?: Kafka(name).also { kafka = it }

    fun register(){
        registered = true
        getKafka().pushToTopic(BuildConfig.SERVICES_TOPIC, name, this@Service, ServiceSerializer::class.java)
    }

    suspend fun listen(apply: KStream<String, String>.() -> Unit) = getKafka().invoke{
        if (!registered) register()
        stream<String, String>(name).let(apply)
    }
}


