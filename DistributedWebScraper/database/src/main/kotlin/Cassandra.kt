package db
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.Select
import com.datastax.driver.mapping.Mapper
import com.datastax.driver.mapping.MappingManager
import kotlin.reflect.KClass

interface CassandraTableObject
fun <T: CassandraTableObject> KClass<T>.readingFrom(cassandra: Cassandra) = cassandra.mapperFor(this.java)

data class Address(val ip: String, val port: Int)
data class TableDefinition(val name: String, val attrs: Map<String, String>, val keyspace: String)

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

    fun makeTable(def: TableDefinition) =
            exec("CREATE TABLE IF NOT EXISTS ${def.keyspace}.${def.name} (${
                def.attrs.map { (name, type) -> "$name $type" }
                        .reduce { acc, entry -> "$acc, $entry" }
            });")

    fun makeKeyspace(name: String) = exec("""CREATE KEYSPACE IF NOT EXISTS $name WITH REPLICATION = {
        'class' : 'SimpleStrategy',
        'replication_factor' : 1
    };""".trimIndent())

    fun saveJson(table: String, json: String) = exec(
            "INSERT INTO $table JSON '$json';"
    )

    fun <T> mapperFor(clazz: Class<T>): Mapper<T> = MappingManager(this).mapper(clazz)
}

