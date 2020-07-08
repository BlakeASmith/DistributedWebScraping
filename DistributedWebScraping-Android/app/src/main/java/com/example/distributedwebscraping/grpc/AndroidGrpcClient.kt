package com.example.distributedwebscraping.grpc
import Address
import GrpcClient
import HttpRequestHandler
import MasterServiceConnection
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import withConnectionToMaster

val routingServerRequestHandler = object : HttpRequestHandler<Address> {
    override fun <R> get(url: String, error: (Throwable) -> R,  action: (Address) -> R) = GlobalScope.async {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
            .body().string().also(::println)
            .let { JSONObject(it) }
            .let { Address(it.getString("ip"), it.getInt("port")) }
            .let(action)
    }

}

class AndroidMasterServiceConnection: MasterServiceConnection(routingServerRequestHandler)
