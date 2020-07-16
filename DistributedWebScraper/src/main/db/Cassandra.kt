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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.reflect.KClass

val createWordCountTable = """CREATE TABLE webscraper.wordcounts ( url text PRIMARY KEY, counts map<text, int> );"""
val createWebscpraperKeyspace = """CREATE KEYSPACE webscraper WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
};""".trimIndent()

interface CassandraTableObject

inline fun <reified T: CassandraTableObject> KClass<T>.readingFrom(cassandra: Cassandra) =
        cassandra.mapperFor(this.java)

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
    suspend fun <T: CassandraTableObject> select(readAs: KClass<T>, queryFunc: Select.Selection.() -> Select) =
            select(queryFunc).let { mapperFor(readAs.java).map(it) }.all()

    fun usingKeyspace(name: String) = this.also { execute("USE $name") }

    fun <T> mapperFor(clazz: Class<T>): Mapper<T> = MappingManager(this).mapper(clazz)
}

fun main() = runBlocking {
    Cassandra("192.168.1.147", 9042).usingKeyspace("webscraper")
            .select(WordCount::class) { all().from("wordcounts") }
            .map { it.counts!!.toMutableMap() }
            .reduce { acc, map ->
                map.forEach {
                    if (it.key in acc)
                        acc[it.key] = acc[it.key]!! + it.value
                    else
                        acc[it.key] = it.value
                }
                acc
            }.onEach(::println)
            .also { print(it.size) }
            .also { println("MAX" + it.maxBy { it.value }) }
            .json()
            .also { File("wc.json").apply { createNewFile() }.writeText(it) }
}


inline fun <reified T: CassandraTableObject> Flow<T>.store(
        cassandra: Cassandra,
        mapper: Mapper<T> = cassandra.mapperFor(T::class.java)
) = onEach { mapper.save(it) }



