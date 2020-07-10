import com.google.gson.Gson
import db.Cassandra
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import shared.Configuration
import java.net.URL

fun Any.json(gson: Gson = Gson()): String = gson.toJson(this)
inline fun <reified T> String.fromJson(gson: Gson = Gson()) = gson.fromJson(this, T::class.java)

val master = MasterServiceConnection{
    URL("${Configuration.routingServiceAddress}/masterAddress")
            .openConnection()
            .getInputStream()
            .bufferedReader()
            .let { Gson().fromJson(it, Address::class.java) }
}

fun main() = runBlocking {
    val cassandra = Cassandra(Configuration.cassandraAddress)

    val job = master.requestWork(jobRequest(1))
    val confirmation = job.urlsList
            .map { URL(it) }.asFlow()
            .scrape { wc(it) }
            .store(cassandra)
            .onEach { println("Storing $it") }
            .toList()
            .map { it.json() }
            .let { jobResult(job, it) }
            .let { master.completeWork(it) }
}

