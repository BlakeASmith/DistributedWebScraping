import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.*
import java.net.URL


class WordCount {
    var url: String? = null
    var counts: Map<String, Int>? = null

    override fun toString(): String = "$url\n$counts"
}

val routing = RoutingService(Configuration.routingServiceAddress)

val channel = routing.connectMaster()
val dbChannel = routing.connectDb()

suspend fun main() {
    val master = MasterServiceConnection(channel)
    val database = DatabaseServiceConnection(dbChannel)

    while(true) {
        val job = master.requestWork(jobRequest(1))
        job.urlsList
                .map { kotlin.runCatching {  URL(it) }.getOrNull()  }.asFlow()
                .filterNotNull()
                .scrape {  wc(it) }
                .map { (url, map) ->  WordCount().apply { this.url = url.toString(); this.counts = map  } }
                .map { it.json() }
                .onEach { println(it) }
                .toList()
                .let { Db.JsonObjects.newBuilder().addAllText(it).setType("WORDCOUNT").build() }
                .also { database.store(it) }
        master.completeWork(jobResult(job, true))
    }
}

