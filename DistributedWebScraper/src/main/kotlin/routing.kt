import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import shared.Configuration

data class Address(val ip: String, val port: Int){
    override fun toString(): String = "http://$ip:$port"
}

suspend fun <T> withConnectionToMaster (http: HttpRequestHandler<Pair<String, Int>>, action: MasterGrpc.MasterBlockingStub.() -> T) =
    http.get("http://${Configuration.routingServiceAddress}/masterAddress"){ (ip, port) ->
        ManagedChannelBuilder.forAddress(ip, port)
            .usePlaintext().build()
            .let { MasterGrpc.newBlockingStub(it) }
            .let(action)
    }

/**
 * A connection to the M
 */
open class MasterServiceConnection(
    private val http: HttpRequestHandler<Address>) : GrpcClient{

    constructor(getMasterAddr: () -> Address): this(object : HttpRequestHandler<Address> {
        override suspend fun <R> get(url: String, error: (Throwable) -> R, action: suspend (Address) -> R) = action(getMasterAddr())

    })

    var stub: MasterGrpc.MasterBlockingStub = runBlocking { obtainBlockingStub() }

    private suspend fun obtainBlockingStub() = http
        .get("${Configuration.routingServiceAddress}/masterAddress"){ (ip, port) ->
            ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build()
                .let { MasterGrpc.newBlockingStub(it) }
        }

    private suspend  fun <R> withStub(
        error: (Throwable) -> R = { throw it },
        action: MasterGrpc.MasterBlockingStub.() -> R
    ): R = kotlin.runCatching { stub.action() }
        .recoverCatching {
            stub = obtainBlockingStub()
            stub.action()
        }
        .recover(error)
        .getOrThrow()

    override suspend fun requestWork(jobRequest: App.JobRequest): App.Job =
        withStub { requestJob(jobRequest) }

    override suspend fun completeWork(job: App.JobResult): App.JobCompletion =
        withStub { completeJob(job) }
}


