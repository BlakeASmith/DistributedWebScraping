package com.example.distributedwebscraping.grpc
import GrpcClient
import HttpRequestHandler
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import withConnectionToMaster

val routingServerRequestHandler = object : HttpRequestHandler<Pair<String, Int>> {
    override fun <R> get(url: String, action: (Pair<String, Int>) -> R) = GlobalScope.async {
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
            .body().string().also(::println)
            .let { JSONObject(it) }
            .let { it.getString("ip") to it.getInt("port") }
            .let(action)
    }

}


class AndroidGrpcClient: GrpcClient {

    // TODO: get a new stub if there is a failure to connect
    val stub by lazy { runBlocking {
        withConnectionToMaster(routingServerRequestHandler){ this }.await()
    } }

    override fun requestWork(jobRequest: App.JobRequest): App.Job = runBlocking {
        //withConnectionToMaster(routingServerRequestHandler){ requestJob(jobRequest) }.await()
        stub.requestJob(jobRequest)
    }

    override fun completeWork(job: App.JobResult): App.JobCompletion = runBlocking {
        //withConnectionToMaster(routingServerRequestHandler) { completeJob(job) }.await()
        stub.completeJob(job)
    }
}