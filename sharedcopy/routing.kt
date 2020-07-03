import io.grpc.ManagedChannelBuilder
import shared.Configuration

suspend fun <T> withConnectionToMaster (http: HttpRequestHandler<Pair<String, Int>>, action: MasterGrpc.MasterBlockingStub.() -> T): T =
    http.get("http://${Configuration.routingServiceIP}:${Configuration.routingServicePort}"){ (ip, port) ->
        ManagedChannelBuilder.forAddress(ip, port) }.usePlaintext().build()
        .let { MasterGrpc.newBlockingStub(it) }
        .let(action)
