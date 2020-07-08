package com.example.distributedwebscraping
import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.distributedwebscraping.grpc.AndroidMasterServiceConnection
import jobRequest
import jobResult

val INTERNET = Permission(Manifest.permission.INTERNET)

val masterConnection = AndroidMasterServiceConnection()

class MainActivity : PermissionHandler() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun setViewText(text: String){
        val txtview: TextView = findViewById(R.id.textview1)
        txtview.text = text
    }

    suspend fun startWorking(startWorkingButton: View) = requiring(INTERNET){
            startWorkingButton as Button
            val job = masterConnection.requestWork(jobRequest(7))
            masterConnection.completeWork(jobResult(job, listOf("FOOOO")))
    }
}