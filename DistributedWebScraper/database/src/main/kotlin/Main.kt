
import com.google.gson.Gson
import db.Address
import db.Cassandra
import db.TableDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.URL

val gson = Gson()

fun main(args: Array<String>) {
    val ip = args[0]
    println(ip)

    URL("http://blakesmith.pythonanywhere.com/update/db/$ip/9696").openStream()

    fun connectToCass() :Cassandra = runCatching {
        Cassandra(Address("127.0.0.1", 9042)).also {
            println("connected to cassandra db")
        }
    }.getOrElse { sleep(100); connectToCass() }



    val cass = connectToCass()
            .makeKeyspace("webscraper")
            .also { println("created webscraper keyspace") }
            .makeTable(
                    TableDefinition("wordcounts",
                            mapOf(
                                    "url" to "text PRIMARY KEY",
                                    "counts" to "map<text, int>"
                            ),
                            "webscraper"
                    )
            )
            .also { println("made wordcount table") }
            .usingKeyspace("webscraper")

    val wcService = object : DatabaseGrpc.DatabaseImplBase(){
        override fun store(request: Db.JsonObjects?, responseObserver: StreamObserver<Db.StorageConfirmation>?) {
            request!!.textList
                    .onEach { println("storing $it") }
                .forEach { cass.saveJson("wordcounts", it) }
            responseObserver!!.onNext(Db.StorageConfirmation.newBuilder().setSuccess(true).build())
            responseObserver.onCompleted()
        }
    }


    val server = NettyServerBuilder.forAddress(InetSocketAddress(ip, 9696))
        .addService(wcService)
        .build()


    println("running database service on port 9696")
    server.start()
}