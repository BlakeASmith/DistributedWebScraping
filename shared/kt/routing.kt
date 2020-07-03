import io.grpc.ManagedChannelBuilder
import shared.Configuration

fun <T> withConnectionToMaster (http: HttpRequestHandler<Pair<String, Int>>, action: MasterGrpc.MasterBlockingStub.() -> T): T =
    http.get("http://${Configuration.routingServiceIP}:${Configuration.routingServicePort}/masterAddress"){ (ip, port) ->
        print(ip to port)
        ManagedChannelBuilder.forAddress(ip, port) }.usePlaintext().build()
        .let { MasterGrpc.newBlockingStub(it) }
        .let(action)
