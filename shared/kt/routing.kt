import io.grpc.ManagedChannel
import kotlinx.coroutines.*

data class Address(val ip: String, val port: Int){
    override fun toString(): String = "http://$ip:$port"
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


