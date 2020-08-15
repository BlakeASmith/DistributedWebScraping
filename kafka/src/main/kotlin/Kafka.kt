package ca.blakeasmith.kkafka.jvm

import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerialization
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class BootstrapServer(val host: String, val port: Int = 9042){
    companion object{
        fun fromString(string: String) = BootstrapServer(
                host = string.takeWhile { it != ':' },
                port = string.takeLastWhile { it != ':' }.toInt()
        )
    }
    override fun toString() = "$host:$port"
}

data class KafkaConfig(val bootstraps: List<BootstrapServer>){

    constructor(vararg  bootstraps: BootstrapServer): this(bootstraps.toList())
    constructor(vararg bootstraps: String): this(bootstraps.map { BootstrapServer.fromString(it) })
    constructor(vararg bootstraps: Pair<String, Int>): this(bootstraps.map { BootstrapServer(it.first, it.second) })

    companion object {
        fun localhost() = KafkaConfig(BootstrapServer("127.0.0.1"))
    }

    class Builder constructor() {
        private var bootstrapServers = mutableListOf<BootstrapServer>()

        constructor(bootstraps: List<BootstrapServer>): this() {
            bootstrapServers.addAll(bootstraps)
        }

        constructor(vararg  bootstraps: BootstrapServer): this(bootstraps.toList())
        constructor(vararg bootstraps: String): this(bootstraps.map { BootstrapServer.fromString(it) })
        constructor(vararg bootstraps: Pair<String, Int>): this(bootstraps.map { BootstrapServer(it.first, it.second) })

        fun servers(value: List<BootstrapServer>){
            bootstrapServers.addAll(value)
        }

        fun server(value: BootstrapServer){
            bootstrapServers.add(value)
        }

        fun server(value: Pair<String, Int>){
            bootstrapServers.add(BootstrapServer(value.first, value.second))
        }

        fun server(value: String){
            bootstrapServers.add(BootstrapServer.fromString(value))
        }

        fun localhost(){
            bootstrapServers.add(BootstrapServer("127.0.0.1"))
        }

        fun hosts(vararg hosts: String) {
            bootstrapServers.addAll(hosts.map { BootstrapServer(it) })
        }

        fun build(): KafkaConfig {
            if (bootstrapServers.isEmpty())
                throw IllegalStateException("No bootstrap servers specified")
            return KafkaConfig(bootstrapServers)
        }
    }
}



