package db

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.Select
import com.datastax.driver.mapping.Mapper
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping.annotations.PartitionKey
import com.datastax.driver.mapping.annotations.Table
import kotlin.reflect.KClass

data class Address(val ip: String, val port: Int)


interface CassandraTableObject
fun <T: CassandraTableObject> KClass<T>.readingFrom(cassandra: Cassandra) =
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

    fun exec(statement: String) = this.apply { execute(statement) }

    fun usingKeyspace(name: String) = this.also { execute("USE $name") }

    fun <T> mapperFor(clazz: Class<T>): Mapper<T> = MappingManager(this).mapper(clazz)
}

