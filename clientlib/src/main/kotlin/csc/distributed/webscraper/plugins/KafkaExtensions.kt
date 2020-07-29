import csc.distributed.webscraper.kafka.*
import csc.distributed.webscraper.plugins.loadPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.apache.kafka.common.serialization.*
import services.resolver
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile


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
