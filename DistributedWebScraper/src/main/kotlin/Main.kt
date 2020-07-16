import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.*
import java.net.URL

val channel = {
    URL("${Configuration.routingServiceAddress}/masterAddress")
            .openConnection()
            .getInputStream()
            .bufferedReader()
            .let { Address::class.fromJson(it) }
            .also(::println)
            .let { ManagedChannelBuilder.forAddress(it.ip, it.port).usePlaintext().build() }
}

suspend fun main() {
    val master = MasterServiceConnection(channel)
    val database = DatabaseServiceConnection(channel)

    while(true) {
        val job = master.requestWork(jobRequest(1))
        job.urlsList
                .map { kotlin.runCatching {  URL(it) }.getOrNull()  }.asFlow()
                .filterNotNull()
                .scrape {  wc(it) }
                .map { it.json() }
                .onEach { println(it) }
                .toList()
                .let { Db.JsonObjects.newBuilder().addAllText(it).setType("WORDCOUNT").build() }
                .also { database.store(it) }
        master.completeWork(jobResult(job, true))
    }
}

