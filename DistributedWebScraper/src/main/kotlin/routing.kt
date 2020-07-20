import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import plugin.loadRoverPlugin
import java.io.File
import java.io.Reader
import java.net.URL
import java.util.jar.JarFile
import kotlin.reflect.KClass


fun Any.json(gson: Gson = GsonBuilder().setPrettyPrinting().create()): String = gson.toJson(this)
fun <T : Any> KClass<T>.fromJson(json: String, gson: Gson = Gson()) =gson.fromJson(json, java)
fun <T : Any> KClass<T>.fromJson(json: Reader, gson: Gson = Gson()) =gson.fromJson(json, java)

data class Address(val ip: String, val port: Int){
    override fun toString(): String = "http://$ip:$port"
}

class RoutingService(val url: String){
    fun connectMaster() = connectByRoutingService("/masterAddress")
    fun connectDb() = connectByRoutingService("/dbAddress")
    fun getPlugin(name: String, storagePath: String) = requestToRoutingService("/plugins/$name")
            .let { File("$storagePath/$name").apply {
                if (!exists()) createNewFile()
                writeBytes(it.readBytes())
            } }
            .let { JarFile(it) }
            .let { loadRoverPlugin(it) }

    fun getAllPlugin(storagePath: String) = requestToRoutingService("/plugins")
            .bufferedReader()
            .readText()
            .let { Gson().fromJson(it, mutableListOf<String>()::class.java) }
            .map { getPlugin(it, storagePath) }


    private fun requestToRoutingService(path: String) =
        URL("${Configuration.routingServiceAddress}$path")
                .openConnection()
                .getInputStream()

    private fun connectByRoutingService(path: String): () -> ManagedChannel = {
        requestToRoutingService(path)
                .bufferedReader()
                .let { Address::class.fromJson(it) }
                .also { println(it) }
                .let { ManagedChannelBuilder.forAddress(it.ip, it.port).usePlaintext().build() }
    }
}

/**
 * Open a connection to a gRPC service via a ManagedChannel,
 * Automatically reconnect to the service when required
 * */
abstract class ServiceConnection<STUB>(val channel: () -> ManagedChannel) {

    var stub: STUB = runBlocking { obtainStub() }

    abstract suspend fun obtainStub(): STUB

    protected suspend  fun <R> withStub(
            error: (Throwable) -> R = { throw it },
            action: STUB.() -> R
    ): R = kotlin.runCatching { stub.action() }
            .recoverCatching {
                stub = obtainStub()
                stub.action()
            }
            .recover(error)
            .getOrThrow()
}

class DatabaseServiceConnection(channel: () -> ManagedChannel)
    : ServiceConnection<DatabaseGrpc.DatabaseBlockingStub>(channel) {
    override suspend fun obtainStub(): DatabaseGrpc.DatabaseBlockingStub = DatabaseGrpc.newBlockingStub(channel())
    suspend fun store(objects: Db.JsonObjects) = withStub { store(objects) }
}

/**
 * A connection to the MasterService
 */
class MasterServiceConnection(connect: () -> ManagedChannel) : ServiceConnection<MasterGrpc.MasterBlockingStub>(connect), MasterGrpcClient{
    override suspend fun obtainStub():MasterGrpc.MasterBlockingStub = MasterGrpc.newBlockingStub(channel())
    override suspend fun requestWork(jobRequest: App.JobRequest): App.Job =
        withStub { requestJob(jobRequest) }
    override suspend fun completeWork(job: App.JobResult): App.JobCompletion =
        withStub { completeJob(job) }
}


