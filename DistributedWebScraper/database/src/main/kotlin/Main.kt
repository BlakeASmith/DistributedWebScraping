
import com.google.gson.Gson
import db.Address
import db.Cassandra
import db.WordCount
import db.readingFrom
import io.grpc.ServerBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URL

const val createWordCountTable = """CREATE TABLE IF NOT EXISTS webscraper.wordcounts ( url text PRIMARY KEY, counts map<text, int> );"""
val createWebscpraperKeyspace = """CREATE KEYSPACE IF NOT EXISTS webscraper WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
};""".trimIndent()

val gson = Gson()


fun main(args: Array<String>) {
    val ip = args[0]
    println(ip)

    URL("http://blakesmith.pythonanywhere.com/update/db/$ip/9696").openStream()

    val cass = Cassandra(Address("127.0.0.1", 9042))
        .exec(createWebscpraperKeyspace)
        .exec(createWordCountTable)
        .usingKeyspace("webscraper")

    val wcMapper = WordCount::class.readingFrom(cass)

    val wcService = object : DatabaseGrpc.DatabaseImplBase(){
        override fun store(request: Db.JsonObjects?, responseObserver: StreamObserver<Db.StorageConfirmation>?) {
            request!!.textList
                .forEach { wcMapper.save(gson.fromJson(it, WordCount::class.java)) }
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