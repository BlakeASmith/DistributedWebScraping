package com.example.distributedwebscraping.grpc
import com.example.distributedwebscraping.*
import com.example.proto.MasterGrpc
import com.example.proto.Proto
import io.grpc.ManagedChannelBuilder

/**
 *  Get a Grpc Stub to access RPC's for the current Master
 *
 *  @author Blake Smith
 */
fun test(n:Int) = AndroidGrpcClient().requestWork(App.JobRequest.newBuilder().setId(10).build())
        .also { print(it.id) }

fun main() { test(7) }
