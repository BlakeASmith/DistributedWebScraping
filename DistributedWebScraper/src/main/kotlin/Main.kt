import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

fun main() {
    runBlocking {
        val job = master.requestWork(jobRequest(1))
        println(job)
    }
}

