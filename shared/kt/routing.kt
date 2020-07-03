import android.util.Log
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Deferred
import shared.Configuration


fun <T> withConnectionToMaster (http: HttpRequestHandler<Pair<String, Int>>, action: MasterGrpc.MasterBlockingStub.() -> T): Deferred<T> =
    http.get("http://${Configuration.routingServiceIP}:${Configuration.routingServicePort}/masterAddress"){ (ip, port) ->
        Log.d("RESP", "$ip:$port")
        ManagedChannelBuilder.forAddress(ip, port)
            .usePlaintext().build()
            .let { MasterGrpc.newBlockingStub(it) }
            .let(action)
    }
