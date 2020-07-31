package csc.distributed.webscraper.plugins

import com.google.gson.Gson
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.services.Service
import csc.distributed.webscraper.services.ServiceDeserializer
import csc.distributed.webscraper.services.ServiceSerializer
import kotlinx.coroutines.*
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.*
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
const val CONFIG_ENV_VARIABLE = "WEBSCRAPER_CONFIG"
const val CONFIG_DEFAULT_LOCATION = "config.json"

@ExperimentalCoroutinesApi
fun pluginProduction(kafka: Kafka) = PluginProducer(kafka).produceTo("plugins")

class PluginProducer(kafka: Kafka): Producer<String, ByteArray>(
        kafka,
        StringSerializer::class.java,
        ByteArraySerializer::class.java
)

fun jarToPlugin(name: String, bytes: ByteArray) = run {
    val tmp = createTempFile(name, ".jar").apply {
        deleteOnExit()
    }
    Files.copy(ByteArrayInputStream(bytes), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
    name to loadPlugin(JarFile(tmp), "Plugin")
}

class PluginConsumer(kafka: Kafka): Consumer<String, ByteArray>(
        listOf("plugins"),
        StringDeserializer::class.java,
        ByteArrayDeserializer::class.java,
        kafka,
        groupId = "***" // group id will never be seen by kafka as we are not committing any offsets and assigning the partition manually
                        // but it is required by the API
)

@ExperimentalCoroutinesApi
fun plugins(kafka: Kafka) = PluginConsumer(kafka)
        .partitionFlow(listOf(TopicPartition("plugins", 0)))

class ServiceConsumer(kafka: Kafka): Consumer<String, Service>(
        listOf("services"),
        StringDeserializer::class.java,
        ServiceDeserializer::class.java,
        kafka,
        groupId = "logger"
)


@FlowPreview
@ExperimentalCoroutinesApi
fun serviceProduction(kafka: Kafka) = Producer(kafka, StringSerializer::class.java, ServiceSerializer::class.java)
        .produceTo("service")


data class Config(
        val bootstraps: List<String>
){
    companion object{
        fun get(): Config = (System.getenv(CONFIG_ENV_VARIABLE) ?: CONFIG_DEFAULT_LOCATION)
                .let { File(it) }
                .let { Gson().fromJson(it.readText(), Config::class.java) }
    }

    fun writeTo(file: File) = Gson().toJson(this)
            .let { file.writeText(it) }
}

fun writeDefaultConfig(path: String = System.getenv(CONFIG_ENV_VARIABLE) ?: CONFIG_DEFAULT_LOCATION) = File(path).apply {
    if(!exists()) createNewFile()
    Config(listOf("127.0.0.1:9092")).writeTo(this)
}

