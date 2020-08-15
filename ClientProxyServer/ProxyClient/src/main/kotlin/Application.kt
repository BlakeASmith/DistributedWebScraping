package csc.distributed.webscraper.proxy

import csc.distributed.webscraper.types.JsoupLoader
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

val SERVER_URL = System.getenv("PROXY_SERVER") ?: "http://0.0.0.0:8080"

@ExperimentalCoroutinesApi
suspend fun main(): Unit  {
    val client = HttpClient(Apache) {
        install(JsonFeature){
            serializer = GsonSerializer()
        }
        engine {
            socketTimeout = Int.MAX_VALUE
        }
    }

    ProxyClient(client, SERVER_URL, JsoupLoader, true)
            .start()
            .join()
}




