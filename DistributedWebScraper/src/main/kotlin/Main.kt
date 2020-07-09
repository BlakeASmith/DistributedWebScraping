import com.google.gson.Gson
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import shared.Configuration
import java.net.URL


val master = MasterServiceConnection{
    URL("${Configuration.routingServiceAddress}/masterAddress")
            .openConnection()
            .getInputStream()
            .bufferedReader()
            .let { Gson().fromJson(it, Address::class.java) }
}

fun main() = runBlocking {

        val job = master.requestWork(jobRequest(1))
        val confirmation = job.urlsList
                .map { URL(it) }.asFlow()
                .scrape { wc(it) }
                .toList()
                .let { jobResult(job, it) }
                .let { master.completeWork(it) }
        println(confirmation)

}

