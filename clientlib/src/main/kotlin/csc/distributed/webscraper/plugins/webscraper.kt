package csc.distributed.webscraper.plugins

import com.google.gson.Gson
import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.services.ServiceDeserializer
import csc.distributed.webscraper.services.ServiceSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.apache.kafka.common.serialization.*
import services.resolver
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile


const val CONFIG_ENV_VARIABLE = "WEBSCRAPER_CONFIG"
const val CONFIG_DEFAULT_LOCATION = "config.json"

val PLUGIN_TOPIC = Topic("plugins", Serdes.String(), Serdes.ByteArray())

fun jarToPlugin(name: String, bytes: ByteArray) = run {
    val tmp = createTempFile(name, ".jar").apply {
        deleteOnExit()
    }
    Files.copy(ByteArrayInputStream(bytes), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
    name to loadPlugin(JarFile(tmp), "Plugin")
}

@ExperimentalCoroutinesApi
val pluginFlow by lazy {
    PLUGIN_TOPIC.asFlow()
            .map { (name, bytes) -> jarToPlugin(name, bytes) }
}

@FlowPreview
@ExperimentalCoroutinesApi
val pluginResolver by lazy {
    pluginFlow.resolver()
}

val services = Topic("services", Serdes.String(), Serdes.serdeFrom(ServiceSerializer(), ServiceDeserializer()))

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

