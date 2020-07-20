import kotlinx.coroutines.flow.*
import java.io.File
import java.net.URL
import java.util.jar.JarFile

const val PLUGIN_DIR = "./plugins"

val routing = RoutingService(Configuration.routingServiceAddress)
val channel = routing.connectMaster()
val dbChannel = routing.connectDb()

val plugins : Map<String, RoverPlugin> = File(PLUGIN_DIR).apply {
    if (!exists()) {
        createNewFile()
        mkdir()
    }
}.walk().drop(1)
        .filter { it.extension == "jar" }
        .map { JarFile(it) }
        .associateBy { it.name }
        .mapValues { (_, jar) -> loadPlugin(jar) }

suspend fun main() {
    val master = MasterServiceConnection(channel)
    val database = DatabaseServiceConnection(dbChannel)

    while(true) {
        val job = master.requestWork(jobRequest(1))
        val plugs = job.pluginsList
                .map { plugins.getOrElse(it) {
                    routing.getPlugin(it, PLUGIN_DIR)
                } }
        job.urlsList
                .map { kotlin.runCatching {  URL(it) }.getOrNull()  }.asFlow()
                .filterNotNull()
                .scrape { doc ->  plugs.map { it.scrape(doc) } }
                .flatMapConcat { (_, results) -> results.asFlow() }
                .let { Db.JsonObjects.newBuilder().addAllText(it.toList()).setType("WORDCOUNT").build() }
                .also { database.store(it) }
        master.completeWork(jobResult(job, true))
    }
}

