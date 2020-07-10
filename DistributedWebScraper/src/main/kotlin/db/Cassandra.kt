package db

import Address
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.Select
import com.datastax.driver.mapping.Mapper
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping.annotations.PartitionKey
import com.datastax.driver.mapping.annotations.Table
import kotlinx.coroutines.runBlocking

val createWordCountTable = """CREATE TABLE webscraper.wordcounts ( url text PRIMARY KEY, counts map<text, int> );"""
val createWebscpraperKeyspace = """CREATE KEYSPACE webscraper WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
};""".trimIndent()

interface CassandraTableObject

@Table(keyspace = "webscraper", name = "wordcounts")
class WordCount : CassandraTableObject{
    @PartitionKey var url: String? = null
    var counts: Map<String, Int>? = null

    override fun toString(): String = "$url\n$counts"
}

class Cassandra (val node: String, val port: Int): Session by Cluster
    .builder()
    .addContactPoint(node)
    .withPort(port)
    .build()
    .connect(){

    constructor(address: Address): this(address.ip, address.port)

    suspend fun select(queryFunc: Select.Selection.() -> Select): ResultSet = execute(QueryBuilder.select().queryFunc().queryString)

    fun usingKeyspace(name: String) = this.also { execute("USE $name") }

    fun <T> mapperFor(clazz: Class<T>): Mapper<T> = MappingManager(this).mapper(clazz)
}

fun main() { runBlocking {
    val cassandra = Cassandra("127.0.0.1", 9042)

    val table = cassandra
        .mapperFor(WordCount::class.java)

    table.save(WordCount().apply { this.counts = mapOf("f00" to 7); this.url = "fooo" })

    cassandra.usingKeyspace("webscraper")
        .select { all().from("wordcounts") }
        .let { table.map(it) }
        .forEach(::println)
} }





